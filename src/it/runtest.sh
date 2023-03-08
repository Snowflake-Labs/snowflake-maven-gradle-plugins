#!/bin/sh
# This script is used to run the snowflake-maven-plugin on integration test projects

IT=$1
snowflakeMavenPluginVersion=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
cd src/it/"$IT"
mvn "clean" "package"
if $2
then
# This line is run to pass CLI arguments to the plugin
# We pass "-Ddeploy.args="a string, b string"" as an unsplit arg with "$4"
# This is so that the spaces in "-Ddeploy.args=a string, b string" doesn't cause this section to be parsed as another lifecycle goal
  mvn snowflake:deploy $3 "$4" -Dcom.snowflake.snowflake-maven-plugin.version="$snowflakeMavenPluginVersion"
else
# There are no arguments here to avoid 'mvn' command thinking that the arguments are other lifecycle goals
  mvn snowflake:deploy -Dcom.snowflake.snowflake-maven-plugin.version="$snowflakeMavenPluginVersion"
fi
