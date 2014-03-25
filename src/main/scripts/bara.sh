#!/bin/sh
#####################################
#dataset name
NAME=$1
#asio release directory
DIST=$2
#####################################

######################################################
# Sollen wir es machen???
cd /home/test/d2rq-0.8.3-dev/
./generate-mapping -u test -p test -o config.ttl --verbose jdbc:mysql://localho$
cd /home/test/serviceTestFramework/DSE-2.0/tomcat/webapps
#####################################################


######################################################
# 
mkdir $NAME
mkdir $NAME/WEB-INF
ln -s $DIST/WEB-INF/classes/ $NAME/WEB-INF/
ln -s $DIST/WEB-INF/lib/ $NAME/WEB-INF/
cp -r $DIST/WEB-INF/etc $NAME/WEB-INF/
cp /home/test/d2rq-0.8.3-dev/config.ttl $NAME/WEB-INF/config.ttl
cp $DIST/WEB-INF/web.xml $NAME/WEB-INF/web.xml
###########################################################
