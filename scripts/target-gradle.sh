#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TARGET=${1:-forge-1.20.1}
if [ "$#" -gt 0 ]; then shift; fi
JAVA_HOME=${MC_JAVA_HOME_21:-${JAVA_HOME:-}}
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "Set MC_JAVA_HOME_21 or JAVA_HOME to JDK 21 or newer." >&2
  exit 2
fi
export JAVA_HOME
"$ROOT/gradlew" :tools:target-launcher:installDist --no-daemon
exec "$ROOT/tools/target-launcher/dist/bin/target-launcher" "$TARGET" "$@"
