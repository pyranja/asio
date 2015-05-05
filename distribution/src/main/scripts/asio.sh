#!/bin/sh
# -------------------------------------------------------------------
# asio cli launcher
# -------------------------------------------------------------------
set -e

source /etc/sysconfig/asio

${JAVA} -client -jar -Dconfig_location=${ASIO_HOME} ${ASIO_BASE}/asio-cli.jar "${@}"
