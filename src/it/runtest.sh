#!/bin/sh
# This script is used to run the snowflake-maven-plugin on integration test projects

IT=$1

cp src/it/profile.properties src/it/"$IT"/
cd src/it/"$IT"

mvn "clean" "package"
if $2
then
  mvn snowflake:deploy $3 "$4"
else
  mvn snowflake:deploy
fi