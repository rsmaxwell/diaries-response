#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# zip-src.sh
#
# Create a source ZIP while excluding generated/build/IDE directories.
#
# Usage:
#   zip-src.sh [project-directory] [zip-name]
# ==============================================================================

PROJECT_DIR="${1:-.}"
ZIP_NAME="${2:-java-src.zip}"

if [[ ! -d "${PROJECT_DIR}" ]]; then
    echo "ERROR: Project directory not found: ${PROJECT_DIR}" >&2
    exit 1
fi

if ! command -v zip >/dev/null 2>&1; then
    echo "ERROR: Required command not found: zip" >&2
    exit 1
fi

cd "${PROJECT_DIR}"

find . \
    -path './build'   -prune -o \
    -path './runtime' -prune -o \
    -path './.gradle' -prune -o \
    -path './out'     -prune -o \
    -path './target'  -prune -o \
    -path './bin'     -prune -o \
    -path './.idea'   -prune -o \
    -path './.vscode' -prune -o \
    -path './.git'    -prune -o \
    -type f -print \
    | zip -q "${ZIP_NAME}" -@

echo "Created: ${ZIP_NAME}"
