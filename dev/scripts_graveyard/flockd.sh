#!/bin/sh
#
# /etc/init.d/flockd
#
# flock is a federated sparql processor node
#
# chkconfig: 345 80 20
# description: asio flock is a federated sparql processor node
# pidfile: /var/run/asio/flockd.pid
# config: /etc/sysconfig/asio.conf

APP="flockd"
PID_DIR="/var/run/asio"
PID_FILE="${PID_DIR}/${APP}.pid"
LOCK_FILE="/var/lock/subsys/${APP}"
LOG_FILE="/var/log/${APP}.out"

[[ -f /etc/asio.conf ]] && source /etc/asio.conf

# overridable by system config *********************************************************************
: ${ASIO_BASE:="/usr/share/asio"}
: ${ASIO_HOME:="${ASIO_BASE}"}
: ${ASIO_OWNER:="$(id -u -n)"}
: ${JAVA:="$(which java)"}
: ${JAVA_OPTS:="-server"}
# **************************************************************************************************

EXECUTABLE="${ASIO_BASE}/lib/asio-flock.war --spring.config.location=file:${ASIO_HOME}"

COMMAND="${JAVA} ${JAVA_OPTS} -Dasio.base=${ASIO_BASE} -Dasio.home=${ASIO_HOME} -Dasio.daemon=true -Dasio.daemon.id=${APP} -jar ${EXECUTABLE}"

source /etc/init.d/functions

function new_line () {
  local -r rc=$?
  echo ""
  return ${rc}
}

function start () {
  echo -n "starting ${APP}: "
  daemon --pidfile=${PID_FILE} --check=${APP} --user=${ASIO_OWNER} "${COMMAND} &> ${LOG_FILE}" && touch ${LOCK_FILE}
  new_line
}

function stop () {
  echo -n "stopping ${APP}: "
  killproc -p ${PID_FILE} ${APP} && rm -f ${LOCK_FILE}
  new_line
}

function initialize () {
  if [ "$EUID" -ne "0" ]; then
    echo "This script must be run as root." >&2
    exit 1
  fi
  # allow daemon process to write the .pid file
  mkdir -p "${PID_DIR}"
  chgrp -R "${ASIO_OWNER}" "${PID_DIR}"
  chown -R "${ASIO_OWNER}" "${PID_DIR}"
}

# ********************************* main ***********************************************************

function main () {
  initialize
  # command interpreter
  case "$1" in
    status)   status -p "${PID_FILE}" -l "${LOCK_FILE}" "${APP}";;
    start)    start;;
    stop)     stop;;
    restart|reload)  stop && start;;
    *)        echo "Usage: ${0} {start|stop|status|restart}";exit 2;;
  esac
}

main "$@"
