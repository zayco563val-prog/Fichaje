#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  Darwin* )
    darwin=true
    ;;
  Linux* )
    linux=true
    ;;
esac

# Attempt to locate java
if [ -z "$JAVA_HOME" ] ; then
  JAVA_CMD=`which java`
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

if [ ! -x "$JAVA_CMD" ] ; then
  die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

# Locate the Gradle Wrapper JAR
WRAPPER_JAR="`dirname "$0"`/gradle/wrapper/gradle-wrapper.jar"
if [ ! -r "$WRAPPER_JAR" ]; then
  die "ERROR: Gradle wrapper jar not found at $WRAPPER_JAR"
fi

# Run Gradle
exec "$JAVA_CMD" -jar "$WRAPPER_JAR" "$@"
