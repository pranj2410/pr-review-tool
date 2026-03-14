#!/usr/bin/env sh

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.5
DIST_NAME="gradle-${GRADLE_VERSION}-bin"
DIST_URL="https://services.gradle.org/distributions/${DIST_NAME}.zip"
DIST_DIR="${APP_HOME}/.gradle-dist"
INSTALL_DIR="${DIST_DIR}/gradle-${GRADLE_VERSION}"
ZIP_PATH="${DIST_DIR}/${DIST_NAME}.zip"

if [ -z "${GRADLE_USER_HOME:-}" ]; then
    GRADLE_USER_HOME="${APP_HOME}/.gradle-home"
    export GRADLE_USER_HOME
fi

if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

mkdir -p "${DIST_DIR}"

if [ ! -x "${INSTALL_DIR}/bin/gradle" ]; then
    echo "Gradle ${GRADLE_VERSION} not found locally. Downloading distribution..."

    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "${DIST_URL}" -o "${ZIP_PATH}"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "${DIST_URL}" -O "${ZIP_PATH}"
    else
        echo "Error: curl or wget is required to download Gradle." >&2
        exit 1
    fi

    if command -v unzip >/dev/null 2>&1; then
        unzip -q -o "${ZIP_PATH}" -d "${DIST_DIR}"
    else
        echo "Error: unzip is required to extract the Gradle distribution." >&2
        exit 1
    fi
fi

exec "${INSTALL_DIR}/bin/gradle" "$@"
