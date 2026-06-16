#!/usr/bin/env bash
set -euo pipefail

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PWD/.chatgpt/gradle-home}"

if [[ ! -d "$GRADLE_USER_HOME" ]]; then
  echo "Missing prepared Gradle cache: $GRADLE_USER_HOME" >&2
  echo "Run the GPT-compatible packaging workflow or run ./gradlew --refresh-dependencies first." >&2
  exit 1
fi

bash ./gradlew --offline --no-daemon clean build
