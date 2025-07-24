#!/usr/bin/env bash

set -x
#set -e

HIVEMQ_HOME='/Users/ds/hivemq/ce/hivemq-ce-2025.4'
HIVEMQ_PORT=1883
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

for reasonCode in "${reasonCodes[@]}"; do
	echo "Processing reason code: $reasonCode"

	replacement="simpleAuthOutput.failAuthentication(ConnackReasonCode.${reasonCode}, \"Reason Code: ${reasonCode}\");"
	sed -i.bak -E "s|simpleAuthOutput\.failAuthentication\(.*$|${replacement}|" \
	  "$EXTENSION_SRC/src/main/java/com/hivemq/extensions/failauth/HelloWorldMain.java"

	(cd "$EXTENSION_SRC" && ./gradlew clean hivemqExtensionZip)

	unzip -o "$EXTENSION_ZIP" -d "$EXTENSION_TARGET_DIR"

	"$HIVEMQ_HOME/bin/run.sh" &
	HIVEMQ_PID=$!

	until grep -q "Started HiveMQ in" "$LOG_FILE"; do
		sleep 10
	done

	mqtt sub -i sub -t '#' -p "$HIVEMQ_PORT" -d -v -V "$MQTT_VERSION"

	sleep 1

	pkill -9 -f "$HIVEMQ_JAR_NAME"
	sleep 2

	mv "$HIVEMQ_HOME/log/hivemq.log" "$HIVEMQ_HOME/log/hivemq-${reasonCode}-${MQTT_VERSION}.log"


done

pkill -9 -f "$HIVEMQ_JAR_NAME"
sleep 2
