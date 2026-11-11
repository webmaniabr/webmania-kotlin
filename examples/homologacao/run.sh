#!/bin/sh
# Optional homologation example (ambiente 2). Builds this SDK with the example into a temporary directory and
# runs it. Nothing is built or sent unless WEBMANIA_LIVE=1. Configuration comes only from the WEBMANIA_*
# environment documented in the README.
set -eu
if [ "${WEBMANIA_LIVE:-}" != "1" ]; then
  printf '%s\n' '{"result":"blocked"}'
  exit 77
fi
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
work=$(mktemp -d "${TMPDIR:-/tmp}/webmania-homologacao.XXXXXX")
trap 'rm -rf "$work"' EXIT INT TERM
# Prepared container locations are used when present: an offline Maven repository and the pinned Java SDK
# archive (JAR and POM). Otherwise the user's local repository must already contain the pinned Java SDK.
repo="${HOME:-/tmp}/.m2/repository"
if [ -d /opt/m2-seed ]; then cp -R /opt/m2-seed "$work/m2"; repo="$work/m2"; fi
if [ -d /deps/java ]; then
  dest="$repo/com/webmania/webmania-sdk/0.1.0-preview.9"; mkdir -p "$dest"
  cp /deps/java/webmania-sdk-0.1.0-preview.9.jar /deps/java/webmania-sdk-0.1.0-preview.9.pom "$dest/"
fi
mkdir -p "$work/sdk"
(cd "$root" && tar -cf - --exclude ./target --exclude ./.maven-local --exclude ./.git .) | (cd "$work/sdk" && tar -xf -)
cp "$here/Homologacao.kt" "$work/sdk/src/test/kotlin/Homologacao.kt"
# Build in a subshell so relative WEBMANIA_LIVE_* paths keep resolving against the caller's directory.
(cd "$work/sdk" && mvn -o -q -B -ntp -Dmaven.repo.local="$repo" -DskipTests test-compile dependency:build-classpath -DincludeScope=test -Dmdep.outputFile="$work/classpath.txt" >&2)
java -cp "$work/sdk/target/classes:$work/sdk/target/test-classes:$(cat "$work/classpath.txt")" HomologacaoKt
