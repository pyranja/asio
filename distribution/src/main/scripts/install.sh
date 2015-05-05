#!/bin/sh
# -------------------------------------------------------------------
# asio installer
# -------------------------------------------------------------------
set -e

 if [ "$EUID" -ne "0" ]; then
    echo "must be run as root" >&2
    exit 4
  fi

if [[ ! -f ".asio" ]]; then
  echo "'./.asio' file not found - called from wrong directory?" >&2
  exit 5
fi

function create_folder () {
  mkdir -p "$1"
  chgrp -R "${ASIO_OWNER}" "$1"
  chown -R "${ASIO_OWNER}" "$1"
}

# install global config and pick it up immediately
cp etc/asio.default /etc/sysconfig/asio
source /etc/sysconfig/asio

# system user
useradd --system --user-group --shell /bin/false --comment 'asio service' 'asio'

# install binaries
create_folder ${ASIO_BASE}
cp asio-*.jar ${ASIO_BASE}
cp -r bin ${ASIO_BASE}
# link to cli
ln -s ${ASIO_BASE}/bin/asio.sh /usr/bin/asio

# create home
create_folder ${ASIO_HOME}
cp etc/cli.default.properties ${ASIO_HOME}/cli.properties
cp etc/application.default.yml ${ASIO_HOME}/application.yml
cp etc/application-*.yml ${ASIO_HOME}/
cp .asio ${ASIO_HOME}/.asio
# prepare log dir
create_folder /var/log/asio

# install service daemon
create_folder /var/run/asio
cp bin/asiod.sh /etc/init.d/asiod
chkconfig --add asiod
