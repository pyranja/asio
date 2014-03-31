#!/bin/sh
#####################################
if [ -z $CATALINA_HOME ]
then echo "This Variable 'CATALINA_HOME' shouldn't the left emty!!!"
else
if [ $# != 2 ]
then echo "Two Paramaters must be given!!!\n"
echo "1) Name of Directory z.B: 'test'\n"
# echo "2) Asio Relase Directory z.B: '/usr/share/asio/WEB-INF/classes/'\n"
echo "2) Config file z.B: 'congif.ttl' \n"
else
#dataset name
NAME=$1
#asio release directory
DIST=$ASIO_HOME
#Config file
CONFIGFILE=$3

WebAPP=$CATALINA_HOME/webapps/$NAME
#####################################

######################################################
# Sollen wir es machen???
#cd /home/test/d2rq-0.8.3-dev/
#./generate-mapping -u test -p test -o config.ttl --verbose jdbc:mysql://localho$
#cd /home/test/serviceTestFramework/DSE-2.0/tomcat/webapps
#cd $CATALINA_HOME/webapps
#####################################################

######################################################
#

mkdir $WebAPP
mkdir $WebAPP/WEB-INF
ln -s $DIST/WEB-INF/classes/ $WebAPP/WEB-INF/
ln -s $DIST/WEB-INF/lib/ $WebAPP/WEB-INF/
cp -r $DIST/WEB-INF/etc $WebAPP/WEB-INF/
cp $CONFIGFILE $WebAPP/WEB-INF/config.ttl
cp $DIST/WEB-INF/web.xml $WebAPP/WEB-INF/web.xml
###########################################################
fi
fi