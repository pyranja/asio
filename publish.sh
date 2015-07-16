#!/usr/bin/env bash
# =============================================================================
# publish rpm artifact on bintray
# =============================================================================
set +x  # disable xtrace to hide api key
set -ev

VERSION=${1:?'missing artifact version'}
BINTRAY_KEY=${2:?'missing bintray api key'}
BINTRAY_USER=${3:?'missing bintray user'}

API='https://api.bintray.com/'

RPM_PATH="./distribution/target/publish"
RPM_FILE="${RPM_PATH}/asio-distribution-${VERSION}.rpm"

# download the rpm artifact from local or remote repo
rm -f "${RPM_PATH}/*.rpm"
mvn dependency:copy -Dartifact=at.ac.univie.isc:asio-distribution:${VERSION}:rpm -DoutputDirectory=${RPM_PATH}/ -Dmdep.overwriteReleases=true

# upload to bintray
CURL="curl -u ${BINTRAY_USER}:${BINTRAY_KEY} -H Content-Type:application/json -H Accept:application/json --write-out %{http_code} --output /dev/stderr --silent --show-error"

[[ $(${CURL} -T "${RPM_FILE}" "${API}/content/${BINTRAY_USER}/rpm/asio/${VERSION}/asio-distribution-${VERSION}.rpm?publish=1&override=0") -eq 201 ]] \
&& echo "at.ac.univie.isc:asio:${VERSION} rpm deployed successfully"
