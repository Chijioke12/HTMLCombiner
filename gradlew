#!/bin/sh
# Gradle wrapper script
GRADLE_OPTS="${GRADLE_OPTS:-"-Dfile.encoding=UTF-8"}"

find_java_home() {
    if [ -n "$JAVA_HOME" ]; then
        echo "$JAVA_HOME"
    elif command -v java >/dev/null 2>&1; then
        JAVA_BIN=$(command -v java)
        echo "$(dirname $(dirname $(readlink -f $JAVA_BIN)))"
    fi
}

JAVA_HOME_FOUND=$(find_java_home)
if [ -z "$JAVA_HOME_FOUND" ]; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found." >&2
    exit 1
fi

exec "$JAVA_HOME_FOUND/bin/java" $GRADLE_OPTS -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
