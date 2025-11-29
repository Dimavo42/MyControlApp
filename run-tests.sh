#!/usr/bin/env bash
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
  > /tmp/emulator.log 2>&1 &

EMULATOR_PID=$!

echo "Waiting for emulator to be online..."
adb wait-for-device

# Wait for boot_completed property
BOOT_COMPLETED=""
echo "Waiting for sys.boot_completed=1..."
until [[ "${BOOT_COMPLETED}" == "1" ]]; do
  BOOT_COMPLETED=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  echo "boot_completed=${BOOT_COMPLETED}"
  sleep 2
done
echo "Emulator booted."


# Unlock screen just in case
adb shell input keyevent 82 || true


echo "=== Installing APK ==="
adb install -r "${APK_PATH}"

echo "=== Running Playwright tests ==="
cd "${TESTS_DIR}"


# Ensure local node_modules binaries are used
export PATH="./node_modules/.bin:${PATH}"
# Don't try to open HTML report automatically
export PWTEST_HTML_REPORT_OPEN="never"

# Prefer npm script if you defined one, otherwise plain playwright
if npm run | grep -q "playwright"; then
  # Example: "test:ci" script that runs Playwright
  npm run test:ci -- --reporter=line,html --output="${RESULTS_DIR}" || TEST_EXIT=$?
else
  npx playwright test --reporter=line,html --output="${RESULTS_DIR}" || TEST_EXIT=$?
fi

TEST_EXIT=${TEST_EXIT:-0}

echo "=== Stopping emulator ==="
kill "${EMULATOR_PID}" || true

echo "=== Done ==="
echo "Test reports are in: ${RESULTS_DIR}"

exit "${TEST_EXIT}"