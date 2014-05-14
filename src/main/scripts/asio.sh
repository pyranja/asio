#!/bin/bash

# --------------------------------------------------------------------------------------------
# asio management script
#
# asio needs to know its installation location. It defaults to /usr/share/asio, but can be
#   set explicitly through the environment variable $ASIO_BASE.
# --------------------------------------------------------------------------------------------

set -e # fail fast on any uncatched error

: ${ASIO_BASE:="/usr/share/asio"}

# resolve env variables
source "${ASIO_BASE}/bin/setenv.sh"
: ${ASIO_HOME:="/var/lib/asio"}
: ${ASIO_OWNER:="$(id -u -n)"}
: ${CATALINA_HOME:="/usr/share/tomcat"}

# constants
MIGRATED_STORE="${ASIO_HOME}/migrated"
ACTIVE_STORE="${ASIO_HOME}/active"

# ********************************* utility ********************************************************

# print usage message
function usage () {
  echo ""
  echo "Usage : asio.sh command [dataset_name] [path/to/config.ttl]"
  echo "  deploy    ensure a dataset instance with the given name and d2r mapping exists"
  echo "  undeploy  remove the dataset instance with the given name"
  echo "  upgrade   redeploy all existing dataset instances to update their runtime"
  echo "  migrate   attempt to convert all local webapps into asio instances"
}

# print an error message and exit with return code 1
# args : message
function fail () {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')] [ERROR] $@" >&2
  exit 1
}

# print a timestamped message
# args : message
function log () {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')] [INFO] $@"
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

# check whether the given path is an asio instance
# args : path
# return : 0 if path is an asio instance
function is_asio () {
  if [[ ! -d "${1}" ]]; then
    return 1
  fi
  if [[ ! -f "${1}/.asio" ]]; then
    return 1
  fi
}

# compares given versions
# args :
#   first_version
#   second_version
# return : 0 if same, 1 else
function compare_version () {
  local -r this_version="$1"
  local -r other_version="$2"

  fail "not implemented"
}

# prepare execution, e.g. create required directories
function initialize () {
  mkdir -p "${MIGRATED_STORE}"
  mkdir -p "${ACTIVE_STORE}"
  log "ASIO_BASE=${ASIO_BASE}"
  log "ASIO_HOME=${ASIO_HOME}"
  log "CATALINA_HOME=${CATALINA_HOME}"
}

# ********************************* actions ********************************************************
# these functions perform only limited input validation!

# create a new asio instance with given name and mapping
# args :
#   dataset_name
#   mapping_file path
# return : void
function create () {
  local -r dataset_name="$1"
  local -r mapping_file="$2"
  local -r source_webapp="${ASIO_BASE}/webapp"
  local -r target_webapp="${CATALINA_HOME}/webapps/${dataset_name}"

  # verify preconditions
  log "creating new asio instance at ${target_webapp}"
  mkdir "${target_webapp}"
  verify_directory "${target_webapp}"
  verify_directory "${source_webapp}"

  # do the magic
  mkdir "${target_webapp}/WEB-INF"
  ln -s "${source_webapp}/WEB-INF/classes/" "${target_webapp}/WEB-INF/"
  ln -s "${source_webapp}/WEB-INF/lib/" "${target_webapp}/WEB-INF/"
  ln -s "${source_webapp}/explore/" "${target_webapp}/"
  cp -r "${source_webapp}/WEB-INF/etc" "${target_webapp}/WEB-INF/"  # copy as ogsadai writes to it
  cp "${mapping_file}" "${target_webapp}/WEB-INF/config.ttl"
  cp "${source_webapp}/WEB-INF/web.xml" "${target_webapp}/WEB-INF/web.xml"
  # marks this web application as an asio instance
  echo "${ASIO_VERSION}" > "${target_webapp}/.asio"
  # sets (group-)ownership of instance [fix-16]
  chgrp -R "${ASIO_OWNER}" "${target_webapp}"
  chown -R "${ASIO_OWNER}" "${target_webapp}"
  # allow rw for all -> asio executed by root, but tomcat may run as other user
  chmod -R a+rwX "${target_webapp}/WEB-INF/etc"

  # add to active set
  cp "${mapping_file}" "${ACTIVE_STORE}/${dataset_name}.ttl"
}

# replace the mapping if given dataset and trigger a reload
# args :
#   dataset_name
#   mapping_file path
# return : void
# fails if target is not an asio instance
function update () {
  local -r dataset_name="$1"
  local -r mapping_file="$2"
  local -r target_webapp="${CATALINA_HOME}/webapps/${dataset_name}"

  log "updating asio instance at ${target_webapp}"
  cp "${mapping_file}" "${target_webapp}/WEB-INF/config.ttl"
  touch "${target_webapp}/WEB-INF/web.xml"

  # add to active set
  cp "${mapping_file}" "${ACTIVE_STORE}/${dataset_name}.ttl"
}

# ********************************* commands *******************************************************

# ensure no asio instance with given name exists
# args :
#   dataset_name
# return : void
function undeploy () {
  local -r dataset_name="$1"

  # verify input
  if [[ -z "${dataset_name}" ]]; then
    fail "missing dataset name"
  fi

  # sanity checks
  local target_webapp="${CATALINA_HOME}/webapps/${dataset_name}"
  if ! is_asio "${target_webapp}"; then
    fail "${target_webapp} seems to be no asio instance"
  fi

  # danger !
  rm -rf "${target_webapp}"
  rm -f "${ACTIVE_STORE}/${dataset_name}.ttl"
  log "${dataset_name} undeployed"
}

# ensure an asio instance exists with given name and mapping
# args :
#   dataset_name
#   mapping_file path
# return : void
function deploy () {
  local -r dataset_name="$1"
  local -r mapping_file="$2"

  # verify input
  if [[ -z "${dataset_name}" ]]; then
    fail "missing dataset name"
  elif [[ "${dataset_name}" == "ROOT|manager" ]]; then
    fail "illegal dataset name : <${dataset_name}>"
  fi

  # sanity checks
  if [[ ! -r "${mapping_file}" ]]; then
    fail "mapping not accessible : <${mapping_file}>"
  fi

  # determine correct action
  local target_webapp="${CATALINA_HOME}/webapps/${dataset_name}"
  if [[ -d "${target_webapp}" ]]; then
    if is_asio "${target_webapp}"; then
      local -r target_version=$(<"${target_webapp}/.asio")
      if [[ "${target_version}" == "${ASIO_VERSION}" ]]; then
        # asio instance with matching version -> just bump mapping
        update "${dataset_name}" "${mapping_file}"
      else
        # non-matching asio -> recreate with installed runtime
        undeploy "${dataset_name}"
        create "${dataset_name}" "${mapping_file}"
      fi
    else
      # cannot update non-asio apps
      fail "${target_webapp} exists and is not an asio instance"
    fi
  else
    # no webapp with name exists
    create "${dataset_name}" "${mapping_file}"
  fi
  log "${dataset_name} deployed"
}

# ********************************* migration ******************************************************

# migration sanity checks
# args :
#   dataset_name
# return : 0 if valid candidate, 1 else
function verify_migration_candidate () {
  local -r candidate_name="$1"
  local -r target_webapp="${CATALINA_HOME}/webapps/${candidate_name}"

  if [[ ! -d "${target_webapp}" ]]; then
    log "missing d2r webapp for ${candidate_name}"
    return 1
  fi
  if [[ ! -d "${target_webapp}Service" ]]; then
    log "missing vce webapp for ${candidate_name}"
    return 1
  fi
  if [[ ! -r "${target_webapp}/WEB-INF/config.ttl" ]]; then
    log "missing d2r mapping for ${candidate_name}"
    return 1
  fi
  if [[ -f "${target_webapp}/.asio" ]]; then
    log "'.asio' found ${candidate_name} seems to be an asio instance already"
    return 1
  fi
  return 0
}

# convert a dataset instance by saving the former d2r mapping and removing both. Then deploy a new
# asio instance from the saved d2r mapping
# args :
#   dataset_name
# return : void
function convert () {
  local -r dataset_name="$1"
  local -r webapps="${CATALINA_HOME}/webapps"

  log "saving ${dataset_name} mapping to ${MIGRATED_STORE}"
  mv "${webapps}/${dataset_name}/WEB-INF/config.ttl" "${MIGRATED_STORE}/${dataset_name}.ttl" || fail "backing up ${dataset_name} mapping failed"
  # ! danger
  rm -rf "${webapps}/${dataset_name}/"
  rm -rf "${webapps}/${dataset_name}Service/"

  local -r mapping="${MIGRATED_STORE}/${dataset_name}.ttl"
  deploy "${dataset_name}" "${mapping}" || fail "${dataset_name}:deployment failed"
  return 0
}

# replace all D2R/DSE webapp pairs in the local tomcat with a single asio instance
# args :
#   none
# return : void
function migrate () {
  verify_directory "${CATALINA_HOME}/webapps"
  local -r candidates=("${CATALINA_HOME}"/webapps/*Service/)
  for each in "${candidates[@]}"; do
    local dataset_name=$(basename "${each%Service/}")
    log "migrating ${dataset_name}"
    if verify_migration_candidate "${dataset_name}"; then
      # isolate execution in case of errors
      (convert "${dataset_name}") || log "migrating ${dataset_name} failed"
    else
      log "skipping ${dataset_name}"
    fi
  done
}

# ********************************* upgrade ********************************************************

# determine whether given dataset must be upgraded
# args :
#   candidate_name
# return : 0 if upgrade necessary, 1 else
function verify_upgrade_candidate () {
  local -r candidate_name="$1"
  local -r candidate_webapp="${CATALINA_HOME}/webapps/${candidate_name}"

  if is_asio "${candidate_webapp}"; then
    local -r candidate_version=$(<"${candidate_webapp}/.asio")
    if [[ "${candidate_version}" != "${ASIO_VERSION}" ]]; then
      # outdated asio
      return 0
    fi
  fi
  return 1
}

# perform upgrade on a single dataset
# args :
#   dataset_name
# return : void
function perform_upgrade () {
  local -r dataset_name="$1"

  mv "${CATALINA_HOME}/webapps/${dataset_name}/WEB-INF/config.ttl" "${MIGRATED_STORE}/${dataset_name}.ttl"
  rm -rf "${CATALINA_HOME}/webapps/${dataset_name}"
  create "${dataset_name}" "${MIGRATED_STORE}/${dataset_name}.ttl"
}

# redeploy each asio instance with non-matching version
# args :
#   none
# return : void
function upgrade () {
  verify_directory "${CATALINA_HOME}/webapps"
  local -r candidates=("${CATALINA_HOME}"/webapps/*/)
  for each in "${candidates[@]}"; do
    local dataset_name=$(basename "${each}")
    if verify_upgrade_candidate "${dataset_name}"; then
      log "upgrading ${dataset_name}"
      (perform_upgrade "${dataset_name}") || log "upgrading ${dataset_name} failed"
    else
      log "skipping ${dataset_name}"
    fi
  done
}

# ********************************* main ***********************************************************

function main () {
  initialize
  # command interpreter
  case "$1" in
    deploy)   deploy $2 $3 ;;
    undeploy) undeploy $2 ;;
    migrate)  migrate ;;
    upgrade)  upgrade ;;
    help)     usage ;;
    *)        log "unknown command $1" ; usage ;;
  esac
  exit 0
}

main "$@"
