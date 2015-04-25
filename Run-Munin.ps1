<#
.SYNOPSIS
  invoke the munin cli tool with dev settings
#>

java -client -jar cli/target/asio-cli-exec.jar --username=root --password=change --server-address=https://localhost:8443/ --insecure-connection @args
