#!/usr/bin/env bash

#Run it as bash

# That line is “Bash strict mode”. It makes your script much less forgiving and helps catch bugs
# e -> exit on error
# u -> error on undefined variable
# o -> the exit code of the pipeline becomes non-zero if any element fails

set -euo pipefail

APK_PATH="/workspace/app-debug.apk"
TESTS_DIR="/workspace/tests"
RESULTS_DIR="${TEST_RESULTS_DIR:-/workspace/test-results}"
EMULATOR_NAME="${EMULATOR_NAME:-nexus}"



echo "=== Starting Android emulator ==="
emulator -avd "${EMULATOR_NAME}" \
  -no-window \
  -gpu swiftshader_indirect \
  -no-snapshot \
  -no-boot-anim \
  -no-audio \
  -accel off \
  -memory 2048 \
  > /tmp/emulator.log 2>&1 &

EMULATOR_PID=$!

echo "Waiting for emulator to be online..."
# instead of a blind 'adb wait-for-device', poll with a timeout
MAX_WAIT_SEC=600
# Take the Date now and assign it to variable
START_TIME=$(date +%s)

while true; do
  # Don't let 'adb get-state' kill the script on non-zero exit
  STATE=$(adb get-state 2>/dev/null || echo "offline")

  echo "adb state: ${STATE}"

  if [[ "${STATE}" == "device" ]]; then
    echo "Emulator is online."
    break
  fi

  NOW=$(date +%s)
  ELAPSED=$((NOW - START_TIME))
  if (( ELAPSED > MAX_WAIT_SEC )); then
    echo "ERROR: Emulator did not come online within ${MAX_WAIT_SEC}s"
    echo "===== emulator log ====="
    cat /tmp/emulator.log || true
    echo "========================"
    kill "${EMULATOR_PID}" || true
    exit 1
  fi

  sleep 3
done

# Wait for boot_completed property
BOOT_COMPLETED=""
echo "Waiting for sys.boot_completed=1..."
MAX_BOOT_WAIT_SEC=1000
START_TIME=$(date +%s)

until [[ "${BOOT_COMPLETED}" == "1" ]]; do
  #take the status code of the boot and throw errors away strips \r from 1
  BOOT_COMPLETED=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  echo "boot_completed=${BOOT_COMPLETED}"

  NOW=$(date +%s)
  ELAPSED=$((NOW - START_TIME))
  if (( ELAPSED > MAX_BOOT_WAIT_SEC )); then
    echo "ERROR: sys.boot_completed never became 1 within ${MAX_BOOT_WAIT_SEC}s"
    echo "===== emulator log ====="
    cat /tmp/emulator.log || true
    echo "========================"
    kill "${EMULATOR_PID}" || true
    exit 1
  fi

  sleep 3
done
echo "Emulator booted."


echo "=== Installing APK ==="
adb install -r "${APK_PATH}"


echo "=== Starting Appium server ==="
cd /workspace/tests

npx appium --address 0.0.0.0 --port 4723 --log /tmp/appium.log > /tmp/appium-stdout.log 2>&1 &

APPIUM_PID=$!
echo "Appium PID: ${APPIUM_PID}"

echo "=== Running Playwright tests ==="
# Don't try to open HTML report automatically
export PWTEST_HTML_REPORT_OPEN="never"

cd "${TESTS_DIR}"

TEST_EXIT=0

npx playwright test --reporter=line,html --output="${RESULTS_DIR}" || TEST_EXIT=$?


TEST_EXIT=${TEST_EXIT:-0}

echo "=== Stopping emulator & Appium ==="
kill "${EMULATOR_PID}" || true
kill "${APPIUM_PID}" || true



echo "=== Done ==="
echo "Test reports are in: ${RESULTS_DIR}"

exit "${TEST_EXIT}"