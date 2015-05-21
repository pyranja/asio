<#
.SYNOPSIS
  invoke the munin cli tool with dev settings
#>

$executable = Get-ChildItem cli/target/asio-cli*.jar -Exclude *-config.jar
java -client -jar "$executable" --username=root --password=change --server-address=https://localhost:8443/ --insecure-connection @args
