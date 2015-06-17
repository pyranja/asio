<#
.SYNOPSIS
  Set the project version
#>
param([Parameter(Mandatory=$true)]$Version)

mvn -pl parent versions:set -DnewVersion="$Version"
mvn versions:commit
git add --all