########################
# Stage 1: Build Android app (APK)
########################
FROM gradle:8.13-jdk21 AS android-build


WORKDIR /src/app

# Copy the entire project  (.dockerignore can edit it)
COPY . .


RUN apt-get update && apt-get install -y wget unzip && rm -rf /var/lib/apt/lists/*

# SDK location inside the container
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk

# Versions – adjust if your project uses different ones
ARG ANDROID_CMD="commandlinetools-linux-11076708_latest.zip"
ARG API_LEVEL=34
ARG BUILD_TOOLS=34.0.0
ARG ANDROID_API_LEVEL="android-${API_LEVEL}"

# Download commandline tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/${ANDROID_CMD} -P /tmp && \
    unzip -q /tmp/${ANDROID_CMD} -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/${ANDROID_CMD}

# Put SDK tools on PATH
ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools"

# Accept licenses
RUN yes | sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --licenses

# Install only what build needs: platform + build-tools + platform-tools
RUN yes | sdkmanager --sdk_root=${ANDROID_SDK_ROOT} \
    "platforms;${ANDROID_API_LEVEL}" \
    "build-tools;${BUILD_TOOLS}" \
    "platform-tools"

# --- local.properties from Windows breaks in Linux, override it ---

# Remove existing local.properties (with Windows path) if present
RUN rm -f local.properties app/local.properties || true \
 && printf "sdk.dir=%s\n" "/opt/android-sdk" > local.properties

# --- Build debug APK ---

RUN gradle :app:assembleDebug --no-daemon

# APK will be here:
# /src/app/app/build/outputs/apk/debug/app-debug.apk
########################

########################
# Stage 2: Build Playwright tests
########################
FROM mcr.microsoft.com/playwright:v1.56.1-jammy AS tests-build

WORKDIR /src/tests

# copy dependencies first for caching
COPY e2e-playwright/package*.json ./
RUN npm install

# now copy the rest of the tests
COPY e2e-playwright/ ./


########################



########################
# Stage 3: Create an emulator run the tests and send the report by HTML
########################
FROM eclipse-temurin:21-jdk-jammy AS runner

WORKDIR /workspace

# 1) Copy APK from android-build stage
COPY --from=android-build \
    /src/app/app/build/outputs/apk/debug/app-debug.apk \
    ./app-debug.apk

# 2) Copy tests from tests-build stage as well all the dependcies
COPY --from=tests-build \
    /src/tests \
    ./tests
# -------- Install base tools + Node + emulator deps + SMPT client  --------
#disable interactive install
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    curl wget unzip bzip2 \
    ca-certificates gnupg \
    libdrm-dev libxkbcommon-dev libgbm-dev libasound-dev libnss3 libxcursor1 \
    libpulse-dev libxshmfence-dev xauth xvfb x11vnc fluxbox wmctrl libdbus-glib-1-2 \
 && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
 && apt-get install -y nodejs msmtp \
 && rm -rf /var/lib/apt/lists/*




# -------- Configure msmtp (SMTP client) --------
# Password will come from SMTP_PASS env at runtime (from .secrets)
RUN printf '%s\n' \
  'defaults' \
  'auth           on' \
  'tls            on' \
  'tls_trust_file /etc/ssl/certs/ca-certificates.crt' \
  'logfile        /tmp/msmtp.log' \
  '' \
  'account gmail' \
  'host           smtp.gmail.com' \
  'port           587' \
  'from           dimaiscool95@gmail.com' \
  'user           dimaiscool95@gmail.com' \
  'passwordeval   "printenv SMTP_PASS"' \
  '' \
  'account default : gmail' \
  > /etc/msmtprc \
  && chmod 600 /etc/msmtprc

ENV FROM_USER="dimaiscool95@gmail.com"
ENV CI_MAIL="dimavoronov95work@gmail.com"

# ---- Build-time args ----
ARG ARCH="x86_64"
ARG TARGET="google_apis"
ARG API_LEVEL="34"
ARG BUILD_TOOLS="34.0.0"
ARG ANDROID_CMD="commandlinetools-linux-11076708_latest.zip"
ARG ANDROID_API_LEVEL="android-${API_LEVEL}"
ARG ANDROID_APIS="${TARGET};${ARCH}"
ARG EMULATOR_PACKAGE="system-images;${ANDROID_API_LEVEL};${ANDROID_APIS}"
ARG PLATFORM_VERSION="platforms;${ANDROID_API_LEVEL}"
ARG BUILD_TOOL="build-tools;${BUILD_TOOLS}"
ARG ANDROID_SDK_PACKAGES="${EMULATOR_PACKAGE} ${PLATFORM_VERSION} ${BUILD_TOOL} platform-tools emulator"


# ---- SDK root + APK PATH ----
ENV ANDROID_SDK_ROOT=/opt/android
ENV APK_PATH=/workspace/app-debug.apk

# ---- Install command-line tools ----
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget https://dl.google.com/android/repository/${ANDROID_CMD} -P /tmp && \
    unzip /tmp/${ANDROID_CMD} -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/${ANDROID_CMD}

# Add tools to PATH
ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/emulator:${ANDROID_SDK_ROOT}/build-tools/${BUILD_TOOLS}"
ENV DOCKER="true"

# ---- Accept licenses ----
RUN yes | sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --licenses

# ---- Install SDK packages (platform, build-tools, emulator, system image, etc.) ----
RUN yes | sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --verbose --no_https ${ANDROID_SDK_PACKAGES}

# ---- Create AVD ----
ENV EMULATOR_NAME="nexus"
ENV  EMULATOR_DEVICE="Nexus 6"

RUN echo "no" | avdmanager --verbose create avd \
    --name "${EMULATOR_NAME}" \
    --device "${EMULATOR_DEVICE}" \
    --package "${EMULATOR_PACKAGE}" \
    --force


# -------- Test reports directory --------
ENV TEST_RESULTS_DIR=/workspace/test-results
ENV TESTS_DIR=/workspace/tests
RUN mkdir -p ${TEST_RESULTS_DIR}

# -------- Copy email script --------
COPY report-email.sh /workspace/report-email.sh
RUN chmod +x /workspace/report-email.sh

# -------- Copy entrypoint script --------
COPY run-tests.sh /workspace/run-tests.sh
RUN chmod +x /workspace/run-tests.sh

ENTRYPOINT ["/workspace/run-tests.sh"]




