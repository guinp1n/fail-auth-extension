#!/usr/bin/env bash

set -x
#set -e

HIVEMQ_HOME='/Users/ds/hivemq/hivemq-4.41.0'
HIVEMQ_PORT=1884
HIVEMQ_JAR_NAME="hivemq.jar"
EXTENSION_NAME="hivemq-fail-auth-extension"
EXTENSION_ZIP="build/hivemq-extension/${EXTENSION_NAME}-4.41.0.zip"
EXTENSION_SRC="/Users/ds/projects/fail-auth-extension"
EXTENSION_TARGET_DIR="$HIVEMQ_HOME/extensions"
LOG_FILE="$HIVEMQ_HOME/log/hivemq.log"
MQTT_VERSION=3


reasonCodes=(
BAD_USER_NAME_OR_PASSWORD
BAD_AUTHENTICATION_METHOD
NOT_AUTHORIZED
UNSPECIFIED_ERROR
MALFORMED_PACKET
PROTOCOL_ERROR
IMPLEMENTATION_SPECIFIC_ERROR
BANNED
TOPIC_NAME_INVALID
PACKET_TOO_LARGE
QUOTA_EXCEEDED
PAYLOAD_FORMAT_INVALID
RETAIN_NOT_SUPPORTED
QOS_NOT_SUPPORTED
CONNECTION_RATE_EXCEEDED
UNSUPPORTED_PROTOCOL_VERSION
CLIENT_IDENTIFIER_NOT_VALID
)

pkill -9 -f "$HIVEMQ_JAR_NAME"
sleep 2
rm -rf "$HIVEMQ_HOME/log/hivemq.log" "$HIVEMQ_HOME/data/"*

"$HIVEMQ_HOME/bin/run.sh" &
HIVEMQ_PID=$!

until grep -q "Started HiveMQ in" "$LOG_FILE"; do
	sleep 10
done

for reasonCode in "${reasonCodes[@]}"; do
	echo "Processing reason code: $reasonCode"

	touch $HIVEMQ_HOME/extensions/hivemq-fail-auth-extension/DISABLED
	sleep 5

	replacement="simpleAuthOutput.failAuthentication(ConnackReasonCode.${reasonCode}, \"Failed Authentication Reason Code: ${reasonCode}\");"

	sed -i.bak -E "s|simpleAuthOutput\.failAuthentication\(.*$|${replacement}|" /Users/ds/projects/fail-auth-extension/src/main/java/com/hivemq/extensions/failauth/HelloWorldMain.java
	./gradlew clean hivemqExtensionZip

	unzip -o build/hivemq-extension/hivemq-fail-auth-extension-4.41.0.zip -d $HIVEMQ_HOME/extensions

	rm $HIVEMQ_HOME/extensions/hivemq-fail-auth-extension/DISABLED
	sleep 5

	mqtt sub -i sub -t '#' -p "$HIVEMQ_PORT" -d -v -V "$MQTT_VERSION"

	sleep 1
done

pkill -9 -f "$HIVEMQ_JAR_NAME"
sleep 2

mv "$HIVEMQ_HOME/log/hivemq.log" "$HIVEMQ_HOME/log/hivemq-${MQTT_VERSION}.log"