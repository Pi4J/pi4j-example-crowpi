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

# Skip if this does not affect our monitored interface
if [ "${interface:-}" != "${INTERFACE_NAME}" ]; then
  exit 0
fi

# Determine IP address based on dhcpcd hook data if possible
case "${reason:-}" in
  # If our interface has just been bound, use the passed IP address
  BOUND)
    address="${new_ip_address:-}"
    ;;
  # If our lease expired or interface went down, treat as not connected
  EXPIRE | NOCARRIER)
    address="<not connected>"
    ;;
esac

# If IP address is still empty and therefore unknown, attempt to determine from system
if [ -z "${address:-}" ]; then
  address="$(ip -4 a s "${INTERFACE_NAME}" | grep -Eo 'inet [0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' | awk '{print $2}')"
  if [ -z "${address}" ]; then
	  address="<not connected>"
  fi
fi

# Generate wallpaper with network info
convert "${WP_INPUT_FILE}" \
	-gravity center \
	-pointsize 80 \
	-fill white \
	-draw "text 0,300 'WLAN IPv4: ${address}'" \
	-draw "text 0,400 'Hostname: $(uname -n)'" \
	"${WP_OUTPUT_FILE}.new"

# Atomically replace wallpaper if different from current one
if ! cmp --silent "${WP_OUTPUT_FILE}" "${WP_OUTPUT_FILE}.new"; then
  mv -f "${WP_OUTPUT_FILE}.new" "${WP_OUTPUT_FILE}"
else
  rm -f "${WP_OUTPUT_FILE}.new"
fi
