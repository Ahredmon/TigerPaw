#!/bin/sh
# Gradle wrapper startup script for POSIX systems.

# Attempt to set APP_HOME
APP_HOME="${0%/*}"
APP_NAME="Gradle"
APP_BASE_NAME="${0##*/}"

# Resolve links
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    case "$link" in
        /*) PRG="$link" ;;
        *)  PRG="$(dirname "$PRG")/$link" ;;
    esac
done
APP_HOME="$(cd "$(dirname "$PRG")" && pwd -P)"

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# OS specific support
cygwin=false
darwin=false
nonstop=false
case "$(uname)" in
    CYGWIN*)  cygwin=true ;;
    Darwin*)  darwin=true ;;
    NONSTOP*) nonstop=true ;;
esac

# Determine JVM to use
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi
if ! command -v "$JAVACMD" > /dev/null 2>&1; then
    echo "ERROR: JAVA_HOME is not set and java could not be found in PATH." >&2
    exit 1
fi

exec "$JAVACMD" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
