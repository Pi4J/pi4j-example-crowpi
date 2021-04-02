#!/bin/sh

# Configuration variables
BASE_PATH="/opt/fhnw"
INTERFACE_NAME="wlan0"
WP_INPUT_FILE="${BASE_PATH}/wallpaper-static.jpg"
WP_OUTPUT_FILE="${BASE_PATH}/wallpaper-dynamic.jpg"

# Skip if reason is ROUTERADVERT (IPv6 RA happen every couple minutes)
if [ "${reason:-}" = "ROUTERADVERT" ]; then
	exit 0
fi

# Collect IP address
address="$(ip -4 a s "${INTERFACE_NAME}" | grep -Eo 'inet [0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' | awk '{print $2}')"
if [ -z "${address}" ]; then
	address="<not connected>"
fi

# Generate wallpaper with network info
convert "${WP_INPUT_FILE}" \
	-gravity center \
	-pointsize 80 \
	-fill white \
	-draw "text 0,300 'WLAN IPv4: ${address}'" \
	-draw "text 0,400 'Hostname: $(uname -n)'" \
	"${WP_OUTPUT_FILE}"
