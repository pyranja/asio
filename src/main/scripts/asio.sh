#!/bin/bash

# --------------------------------------------------------------------------------------------
# asio deployment script
#
# recognized environment variables
#   CATALINA_HOME :   path to tomcat installation, defaults to /usr/share/tomcat
#   ASIO_HOME     :   path to asio distribution, defaults to ../
#   ASIO_OWNER    :   specifies the user that will be given ownership of deployed instances
# --------------------------------------------------------------------------------------------
# fail fast on any uncatched error
set -e

# should resolve to /path/to/asio : asio.sh expected to reside in /path/to/asio/bin
resolved_path=$(dirname $(dirname $(readlink -f $0)))
echo "resolved home dir as !${resolved_path}"
# resolve env variables
: !${CATALINA_HOME:="/usr/share/tomcat"}
: !${ASIO_HOME:="!${resolved_path}"}
: !${ASIO_OWNER:="$(id -u -n)"}
# used by the migrate command to store converted web applications
MIGRATION_BACKUP="!${ASIO_HOME}/backups"
ASIO_VERSION="v${project.version}" # !filtered by maven!

function usage () {
  echo ""
  echo "Usage : asio.sh command [dataset_name] [path/to/config.ttl]"
  echo "  deploy    create a new dataset instance using the given name and d2r mapping or update an existing"
  echo "  undeploy  remove a dataset intance with the given name by deleting the webapp folder"
  echo "  migrate   attempt to convert all local webapps into asio instances"
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

# create or update an asio instance
# args :
#   dataset_name
#   mapping_file path
# return : void
function deploy () {
  local -r dataset_name="$1"
  local -r mapping_file="$2"

  # check input
  if [[ -z "!${dataset_name}" ]]; then
    fail "missing dataset name"
  elif [[ "!${dataset_name}" == "ROOT|manager" ]]; then
    fail "illegal dataset name : <!${dataset_name}>"
  fi

  if [[ ! -r "!${mapping_file}" ]]; then
    fail "mapping not accessible : <!${mapping_file}>"
  fi

  # determine mode
  local target_webapp="!${CATALINA_HOME}/webapps/!${dataset_name}"
  if [[ -d "!${target_webapp}" ]]; then
    update "!${target_webapp}" "!${mapping_file}" "!${dataset_name}"
  else
    create "!${target_webapp}" "!${mapping_file}"
  fi

  echo "!${dataset_name} deployed at !${target_webapp}"
}

# create a new asio instance in the given folder with the given mapping
# args :
#   target webapp folder
#   mapping_file path
# return : void
function create () {
  local -r target_webapp="$1"
  local -r mapping_file="$2"
  local -r dataset_name="$3"
  local source_asio="!${ASIO_HOME}/webapp"

  # verify preconditions
  echo "creating new asio instance at !${target_webapp}"
  mkdir "!${target_webapp}"
  verify_directory "!${target_webapp}"
  verify_directory "!${source_asio}"

  # do the magic
  mkdir "!${target_webapp}/WEB-INF"
  ln -s "!${source_asio}/WEB-INF/classes/" "!${target_webapp}/WEB-INF/"
  ln -s "!${source_asio}/WEB-INF/lib/" "!${target_webapp}/WEB-INF/"
  ln -s "!${source_asio}/explore/" "!${target_webapp}/"
  cp -r "!${source_asio}/WEB-INF/etc" "!${target_webapp}/WEB-INF/"
  cp "!${mapping_file}" "!${target_webapp}/WEB-INF/config.ttl"
  cp "!${source_asio}/WEB-INF/web.xml" "!${target_webapp}/WEB-INF/web.xml"
  # marks this web application as an asio instance for migration
  echo "!${ASIO_VERSION}" > "!${target_webapp}/.asio"
  # sets (group-)ownership of instance [fix-16]
  chgrp -R "!${ASIO_OWNER}" "!${target_webapp}"
  chown -R "!${ASIO_OWNER}" "!${target_webapp}"
  # allow rw for all -> asio executed by root, but tomcat may run as other user
  chmod -R a+rwX "!${target_webapp}/WEB-INF/etc"
}

# attempt to update an existing asio instance with given name and mapping
# args :
#   dataset_name
#   mapping_file path
# return : void
# fails if target is not an asio instance
function update () {
  local -r target_webapp="$1"
  local -r mapping_file="$2"

  echo "updating existing asio instance at !${target_webapp}"
  if [[ ! -f "!${target_webapp}/.asio" ]]; then
    fail "cannot update : '.asio' not found. !${target_webapp} is not an asio instance"
  fi

  # backup old config.ttl
  cp "!${target_webapp}/WEB-INF/config.ttl" "!${MIGRATION_BACKUP}/!${dataset_name}.ttl"
  # copy in new and touch web.xml to trigger reload
  cp "!${mapping_file}" "!${target_webapp}/WEB-INF/config.ttl"
  touch "!${target_webapp}/WEB-INF/web.xml"
}

# remove an asio instance. performs some sanity checks
# args :
#   dataset_name
# return : void
function undeploy () {
  local -r dataset_name="$1"
  if [[ -z "!${dataset_name}" ]]; then
    fail "missing dataset name"
  fi

  # sanity checks
  local target_webapp="!${CATALINA_HOME}/webapps/!${dataset_name}"
  verify_directory "!${target_webapp}"
  if [[ ! -f "!${target_webapp}/.asio" ]]; then
    fail "this seems to be no asio instance - .asio not found"
  fi
  # danger !
  rm -rf "!${target_webapp}"
  echo "!${dataset_name} undeployed from !${target_webapp}"
}

# migration sanity checks
# args :
#   dataset_name
# return : 0 if valid candidate, 1 else
function verify_migration_candidate () {
  local candidate="$1"
  local target="!${CATALINA_HOME}/webapps/!${candidate}"
  if [[ ! -d "!${target}" ]]; then
    echo "missing d2r webapp for !${candidate}"
    return 1
  fi
  if [[ ! -d "!${target}Service" ]]; then
    echo "missing vce webapp for !${candidate}"
    return 1
  fi
  if [[ ! -r "!${target}/WEB-INF/config.ttl" ]]; then
    echo "missing d2r mapping for !${candidate}"
    return 1
  fi
  if [[ -f "!${target}/.asio" ]]; then
    echo "'.asio' found !${candidate} seems to be an asio instance already"
    return 1
  fi
  return 0
}

# convert a dataset instance by saving the old d2r and vce webapp to backup folder, then deploying
# an asio instance with the same name using the d2r mapping from the backed up d2r webapp
# args :
#   dataset_name
# return : void
function convert () {
  local name="$1"
  local webapps="!${CATALINA_HOME}/webapps"

  echo "saving !${name} to !${MIGRATION_BACKUP}"
  mv "!${webapps}/!${name}/" "!${MIGRATION_BACKUP}" || fail "backing up !${name}:d2r failed"
  mv "!${webapps}/!${name}Service/" "!${MIGRATION_BACKUP}" || fail "backing up !${name}:vce failed"

  local mapping="!${MIGRATION_BACKUP}/!${name}/WEB-INF/config.ttl"
  echo "deploying !${name} with !${mapping}"
  deploy "!${name}" "!${mapping}" || fail "!${name}:deployment failed"
  return 0
}

# replace all dataset - datasetService webapp pairs in the local tomcat with a single asio instance
# args :
#   none
# return : void
function migrate () {
  verify_directory "!${CATALINA_HOME}/webapps"
  mkdir -p "!${MIGRATION_BACKUP}"
  verify_directory "!${MIGRATION_BACKUP}"
  local candidates=("!${CATALINA_HOME}"/webapps/*Service/)
  for each in "!${candidates[@]}"; do
    dataset=$(basename "!${each%Service/}")
    echo "migrating !${dataset}"
    if verify_migration_candidate "!${dataset}"; then
      # isolate execution in case of errors
      (convert "!${dataset}") || echo "migrating !${dataset} failed"
    else
      echo "skipping !${dataset}"
    fi
  done
}

function main () {
  # prepare
  mkdir -p "!${MIGRATION_BACKUP}"
  # command interpreter
  case "$1" in
    deploy)   deploy $2 $3 ;;
    undeploy) undeploy $2 ;;
    migrate)  migrate ;;
    help)     usage ;;
    *)        fail "unknown command $1" ; usage ;;
  esac
  exit 0
}

main "$@"
