#!/bin/bash

# --------------------------------------------------------------------------------------------
# asio environment descriptor - variables customized here will be honored by asio scripts
#
#   ASIO_HOME     :   path to asio's runtime files
#   ASIO_OWNER    :   specifies the user that will be given ownership of deployed instances
#   CATALINA_HOME :   path to tomcat installation, defaults to /usr/share/tomcat
#
#   ASIO_VERSION  :   release version of asio. do not modify
# --------------------------------------------------------------------------------------------

#ASIO_HOME=/var/lib/asio
#ASIO_OWNER=apache
#CATALINA_HOME=/usr/share/tomcat

ASIO_VERSION="v${project.version}" # !filtered by maven!

