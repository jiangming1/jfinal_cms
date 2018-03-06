#!/bin/sh

if [ -z ${JAVA_HOME} ]; then
JAVA_HOME=/apps/jdk7
fi
APP_HOME=./
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms64m -Xmx1024m -XX:+UseParallelGC -Duser.timezone=GMT+08"
MAIN_CLASS=com.jflyfox.component.config.BaseConfig
MAIN_OPTS="../ 7005"
LIB_OPTS="$APP_HOME/classes:$APP_HOME/lib/*"
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp $LIB_OPTS $MAIN_CLASS $MAIN_OPTS &