#!/usr/bin/env bash
#Run it as bash

# That line is “Bash strict mode”
# e -> exit on error
# u -> error on undefined variable
# o -> the exit code of the pipeline becomes non-zero if any element fails
set -euo pipefail

# Arguments that can be passed or default
REPORT_HTML="${TEST_RESULTS_DIR}/index.html"
BOUNDARY="BOUNDARY_$$"   # simple unique-ish string


if [[ ! -f "${REPORT_HTML}" ]]; then
  echo "ERROR: HTML report not found at ${REPORT_HTML}" >&2
  exit 1
fi

echo "Sending HTML report from ${REPORT_HTML} to ${CI_MAIL} as attachment"
{
  echo "From: ${FROM_USER}"
  echo "To: ${CI_MAIL}"
  echo "Subject: ${SUBJECT}"
  echo "MIME-Version: 1.0"
  echo "Content-Type: multipart/mixed; boundary=\"${BOUNDARY}\""
  echo
  # --- Part 1: plain text body ---
  echo "--${BOUNDARY}"
  echo "Content-Type: text/plain; charset=UTF-8"
  echo
  echo "HTML report attached as ${FILENAME}."
  echo
  # --- Part 2: HTML attachment (base64) ---
  echo "--${BOUNDARY}"
  echo "Content-Type: text/html; name=\"${FILENAME}\""
  echo "Content-Disposition: attachment; filename=\"${FILENAME}\""
  echo "Content-Transfer-Encoding: base64"
  echo
  base64 "${REPORT_HTML}"
  echo
  # --- End of multipart ---
  echo "--${BOUNDARY}--"
} | msmtp "${CI_MAIL}"







