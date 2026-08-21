#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# deploy.sh
#
# Publish the Diaries responder artifact using build metadata produced by the
# preceding build stage. The script also prints dependency/package diagnostics
# that are useful when verifying the Log4j versions in the published fat JAR.
# ==============================================================================

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
RESPONDER_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
PROJECT_DIR="$(cd -- "${RESPONDER_DIR}/.." && pwd)"
BUILD_INFO="${RESPONDER_DIR}/build/buildinfo"
GRADLE_WRAPPER="${PROJECT_DIR}/gradlew"

if [[ ! -f "${BUILD_INFO}" ]]; then
    echo "ERROR: Build information file not found: ${BUILD_INFO}" >&2
    exit 1
fi

# shellcheck disable=SC1090
. "${BUILD_INFO}"

required_vars=(GRADLE_USER_HOME REPOSITORY VERSION)
for var in "${required_vars[@]}"; do
    if [[ -z "${!var:-}" ]]; then
        echo "ERROR: ${var} is not set or empty." >&2
        exit 2
    fi
done

if [[ ! -x "${GRADLE_WRAPPER}" ]]; then
    echo "ERROR: Gradle wrapper is not executable: ${GRADLE_WRAPPER}" >&2
    exit 1
fi

cd "${PROJECT_DIR}"

echo "=== Gradle user home ==="
echo "${GRADLE_USER_HOME}"

echo "=== Log4j entries in version catalog ==="
if [[ -f gradle/libs.versions.toml ]]; then
    grep -n "log4j" gradle/libs.versions.toml || true
elif [[ -f libs.versions.toml ]]; then
    grep -n "log4j" libs.versions.toml || true
fi

echo "=== runtimeClasspath dependency insight ==="
"${GRADLE_WRAPPER}" :diaries-responder:dependencyInsight \
    --dependency log4j \
    --configuration runtimeClasspath

echo "=== Publishing Diaries responder ==="
"${GRADLE_WRAPPER}" :diaries-responder:publish --info --stacktrace \
    -PrepositoryName="${REPOSITORY}" \
    -PprojectVersion="${VERSION}"

echo "=== Packaged log4j-api version from fat JAR ==="
unzip -p diaries-responder/build/libs/diaries-responder-*-fat.jar \
    META-INF/maven/org.apache.logging.log4j/log4j-api/pom.properties || true

echo "=== Packaged log4j-core version from fat JAR ==="
unzip -p diaries-responder/build/libs/diaries-responder-*-fat.jar \
    META-INF/maven/org.apache.logging.log4j/log4j-core/pom.properties || true
