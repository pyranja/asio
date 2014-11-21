#!/bin/bash

# --------------------------------------------------------------------------------------------
# asio install script
#
# you must provide the absolute path to the asio root directory e.g. if you unpacked to
#   '/usr/src', invoke '/usr/src/asio/bin/install.sh /usr/src/asio'.
# --------------------------------------------------------------------------------------------

set -e # fail fast on any uncatched error

: ${ASIO_BASE:="/usr/share/asio"}
: ${CATALINA_SERVICE:="tomcat"}

SRC="$1"

# ********************************* utility ********************************************************

# print an error message and exit with return code 1
# args : message
function fail () {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')] [ERROR] $@" >&2
  exit 1
}

# exit with error code if given path is not a directory
# args :
#   path
# return : void
function verify_directory () {
  if [[ ! -d "$1" ]]; then
    fail "missing required directory : <$1>"
  fi
}

# ------------------------------------- impl

# check input
verify_directory "${SRC}"

# stop tomcat
service ${CATALINA_SERVICE} stop

# fully replace legacy asio installation
if [[ -d "${ASIO_BASE}" ]]; then
  rm -rf "${ASIO_BASE}"
fi
cp -R "${SRC}" "${ASIO_BASE}"

# upgrade
${ASIO_BASE}/bin/asio.sh upgrade

# restart tomcat
service ${CATALINA_SERVICE} start
