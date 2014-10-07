#!/bin/bash

# --------------------------------------------------------------------------------------------
# asio test setup script
#
# create mock webapps folder for asio management tests
# --------------------------------------------------------------------------------------------

WEBAPPS=/usr/share/tomcat/webapps

mkdir -p "${WEBAPPS}"

# regular asio instance
mkdir -p "${WEBAPPS}/uptodate/WEB-INF"
echo "v0.4.4-SNAPSHOT" > "${WEBAPPS}/uptodate/.asio"
echo "uptodate" > "${WEBAPPS}/uptodate/WEB-INF/config.ttl"

# outdated instance
mkdir -p "${WEBAPPS}/outdated/WEB-INF"
echo "v0.4.0" > "${WEBAPPS}/outdated/.asio"
echo "outdated" > "${WEBAPPS}/outdated/WEB-INF/config.ttl"

# legacy d2r / dse pair
mkdir -p "${WEBAPPS}/migrate/WEB-INF"
mkdir -p "${WEBAPPS}/migrateService"
echo "migrate" > "${WEBAPPS}/migrate/WEB-INF/config.ttl"

# random webapp
mkdir -p "${WEBAPPS}/random/WEB-INF"
echo "do not touch" > "${WEBAPPS}/random/index.html"

# lonely dse app
mkdir -p "${WEBAPPS}/loneService"
