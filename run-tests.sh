#!/usr/bin/env bash

#Run it as bash

# That line is “Bash strict mode”
# e -> exit on error
# u -> error on undefined variable
# o -> the exit code of the pipeline becomes non-zero if any element fails

set -euo pipefail


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


echo "Waiting extra 20s for system to stabilize..."
sleep 20


echo "Waiting for launcher..."
while ! adb shell "pm list packages" >/dev/null 2>&1; do
  echo "Package manager not ready yet..."
  sleep 2
done


echo "=== Installing APK ==="
for i in 1 2 3; do
  echo "=== Installing APK (attempt $i) ==="
  if adb install -r "$APK_PATH"; then
    echo "APK installed successfully."
    break
  fi
  echo "Install failed, sleeping before retry..."
  sleep 10
done


echo "=== Starting Appium server ==="
APPIUM_LOG=/tmp/appium.log

npx appium \
  --address 0.0.0.0 \
  --port 4723 \
  --log "$APPIUM_LOG" \
  --log-level debug \
  --log-timestamp \
  >> "$APPIUM_LOG" 2>&1 &
APPIUM_PID=$!

echo "Appium PID: ${APPIUM_PID}"

echo "=== Running Playwright tests ==="


cd "${TESTS_DIR}"

TEST_EXIT=0

npx playwright test --output="${TEST_RESULTS_DIR}" || TEST_EXIT=$?

exit "$TEST_EXIT"

echo "=== Stopping emulator & Appium ==="
kill "${EMULATOR_PID}" || true
kill "${APPIUM_PID}" || true

echo "=== Sending report by email ==="

if ! /workspace/report-email.sh "${TEST_RESULTS_DIR}"; then
  echo "Failed to send the email"
fi


echo "=== Done ==="
echo "Test reports are in: ${TEST_RESULTS_DIR}"
