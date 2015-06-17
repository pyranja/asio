#!/bin/sh
#
# asio sql/sparql http service
#
# chkconfig:   345 80 20
# description: asio exposes mysql databases via a SQL/SPARQL http endpoint
# pdifile: /var/run/asio/asiod.pid
# config: /etc/sysconfig/asio

source /etc/sysconfig/asio
source /etc/init.d/functions

APP="asio"
PID_FILE="/var/run/asio/${APP}.pid"
LOCK_FILE="/var/lock/subsys/${APP}"
LOG_FILE="/var/log/asio/${APP}.out"

EXECUTABLE="${ASIO_BASE}/asio-server.jar --spring.config.location=file:${ASIO_HOME}/server.yml --logging.file=${LOG_FILE}"
COMMAND="${JAVA} ${JAVA_OPTS} -Dasio.home=${ASIO_HOME} -Ddaemon=true -Dapp-id=${APP} -Dpidfile=${PID_FILE} -jar ${EXECUTABLE}"

function new_line () {
  local -r rc=$?
  echo ""
  return ${rc}
}

function start () {
  echo -n "starting ${APP}: "
  daemon --pidfile=${PID_FILE} --check=${APP} --user=${ASIO_OWNER} "${COMMAND} &> ${LOG_FILE}" && touch ${LOCK_FILE}
}

function stop () {
  echo -n "stopping ${APP}: "
  killproc -p ${PID_FILE} ${APP} && rm -f ${LOCK_FILE}
}

# --------------------------------- main -----------------------------------------------------------

function main () {
  if [ "$EUID" -ne "0" ]; then
    echo "This script must be run as root." >&2
    exit 4
  fi
  # command interpreter
  case "$1" in
    status)   status -p "${PID_FILE}" -l "${LOCK_FILE}" "${APP}";;
    start)    start;;
    stop)     stop;;
    restart|reload)  stop && start;;
    *)        echo "Usage: ${0} {start|stop|status|restart}";exit 2;;
  esac
  new_line
}

main "$@"
