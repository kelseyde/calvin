#!/bin/sh

# Taken from https://web.archive.org/web/20240813210415/https://coderwall.com/p/ssuaxa/how-to-make-a-jar-file-linux-executable

MYSELF=`which "$0" 2>/dev/null`

[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"

java=java

if test -n "$JAVA_HOME"; then
    java="$JAVA_HOME/bin/java"
fi

exec "$java" --add-modules=jdk.incubator.vector -XX:+UseParallelGC $java_args -jar $MYSELF "$@"

exit 1