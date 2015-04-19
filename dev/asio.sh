#!/usr/bin/env bash

# launch asio
/usr/bin/java -server -Dasio.home=/tmp -Ddaemon=true -Dapp-id=brood -Dpidfile=/var/run/asio.pid -jar /vagrant/asio/server/target/asio-server-exec.jar --server.ssl.enabled=false --spring.profiles.active=brood,dev --server.port=8080 --security.user.password=change --logging.level.=INFO --logging.file=/var/log/asio.log
