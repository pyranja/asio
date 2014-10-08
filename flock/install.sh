#!/bin/sh
# install flock on a clean vph vm

su -c "yum -y install java-1.7.0-openjdk"

# assume flock tarbal is in ~
cd /usr/share && tar xzvf ~/flock-0.5.0-RC.tar.gz

# service installation
cp /usr/share/asio/etc/asio.conf /etc/asio.conf
cp /usr/share/asio/bin/flockd.sh /etc/init.d/flockd
chkconfig --add flockd
