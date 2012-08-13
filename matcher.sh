#!/bin/sh
# packet matcher start script
#

java -jar $(dirname "$0")/target/packetmatcher-1.2-jar-with-dependencies.jar $*
