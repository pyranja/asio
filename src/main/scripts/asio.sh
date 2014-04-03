#!/bin/bash

# --------------------------------------------------------------------------------------------
# asio deployment script
#
# recognized environment variables
#   CATALINA_HOME :   path to tomcat installation, defaults to /usr/share/tomcat
#   ASIO_HOME     :   path to asio distribution, defaults to ../
# --------------------------------------------------------------------------------------------
# fail fast on any uncatched error
set -e

# should resolve to /path/to/asio : asio.sh expected to reside in /path/to/asio/bin
resolved_path=$(dirname $(dirname $(readlink -f $0)))
echo "resolved home dir as ${resolved_path}"
# resolve env variables
: ${CATALINA_HOME:="/usr/share/tomcat"}
: ${ASIO_HOME:="${resolved_path}"}

function usage () {
  echo ""
  echo "Usage : asio.sh command dataset_name path/to/config.ttl"
  echo "  deploy    create a new dataset instance using the given name and d2r mapping"
}

# print an error message and exit with return code 1
function fail () {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: $@" >&2
  usage
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

# create an asio instance
# args :
#   dataset_name
#   mapping_file path
# return : void
function deploy () {
  local -r dataset_name="$1"
  local -r mapping_file="$2"

  # check input
  if [[ -z "${dataset_name}" ]]; then
    fail "missing dataset name"
  elif [[ "${dataset_name}" == "ROOT|manager" ]]; then
    fail "illegal dataset name : <${dataset_name}>"
  fi

  if [[ ! -r "${mapping_file}" ]]; then
    fail "mapping not accessible : <${mapping_file}>"
  fi

  # check preconditions
  local target_webapp="${CATALINA_HOME}/webapps/${dataset_name}"
  local source_asio="${ASIO_HOME}/webapp"
  mkdir "${target_webapp}"
  verify_directory "${target_webapp}"
  verify_directory "${source_asio}"

  # do the magic
  mkdir "${target_webapp}/WEB-INF"
  ln -s "${source_asio}/WEB-INF/classes/" "${target_webapp}/WEB-INF/"
  ln -s "${source_asio}/WEB-INF/lib/" "${target_webapp}/WEB-INF/"
  cp -r "${source_asio}/WEB-INF/etc" "${target_webapp}/WEB-INF/"
  cp "${mapping_file}" "${target_webapp}/WEB-INF/config.ttl"
  cp "${source_asio}/WEB-INF/web.xml" "${target_webapp}/WEB-INF/web.xml"
  echo "${dataset_name} deployed at ${target_webapp}"
}

# remove an asio instance. performs some sanity checks
# args :
#   dataset_name
# return : void
function undeploy () {
  local -r dataset_name="$1"
  if [[ -z "${dataset_name}" ]]; then
    fail "missing dataset name"
  fi

  # sanity checks
  local target_webapp="${CATALINA_HOME}/webapps/${dataset_name}"
  verify_directory "${target_webapp}"
  # danger !
  rm -rf "${target_webapp}"
  echo "${dataset_name} undeployed from ${target_webapp}"
}

function main () {
  # command interpreter
  case "$1" in
    deploy)   deploy $2 $3 ;;
    undeploy) undeploy $2 ;;
    help)     usage ;;
    *)        fail "unknown command $1" ; usage ;;
  esac
  exit 0
}

main "$@"
