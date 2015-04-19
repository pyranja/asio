#!/usr/bin/env bash

/usr/bin/java -client -jar /vagrant/asio/cli/target/asio-cli-exec.jar --spring.profiles.active=dev "$@"
