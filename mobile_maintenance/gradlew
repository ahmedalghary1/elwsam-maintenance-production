#!/bin/sh
APP_HOME=$(cd "${0%/*}" && pwd -P)
JAVA_EXE=${JAVA_HOME:+$JAVA_HOME/bin/}java
exec "$JAVA_EXE" ${JAVA_OPTS:-} ${GRADLE_OPTS:-} -Dorg.gradle.appname=gradlew -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
