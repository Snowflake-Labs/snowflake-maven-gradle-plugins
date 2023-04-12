# Contributing Guide

Thank you for your interest in contributing to the Maven and Gradle plugins! 

## Project structure

- [`src/main/java/com/snowflake/core`](src/main/java/com/snowflake/core) contains shared utlities and classes for both the Maven and Gradle implementations
- [`src/main/java/com/snowflake/snowflake_maven_plugin`](src/main/java/com/snowflake/snowflake_maven_plugin) contains the implementation of the **Maven** plugin
- [`src/main/java/com/snowflake/snowflake_gradle_plugin`](src/main/java/com/snowflake/snowflake_gradle_plugin) contains the implementation of the **Gradle** plugin

## Environment setup

We recommend forking this repository into your personal account and submitting pull requests from your fork. Make sure you have Java 11 installed locally (using later versions of Java may cause unintended behavior)

## Maven Setup Instructions

To build and install the Maven plugin locally, fork  this repository and compile with [Apache Maven](https://maven.apache.org) and Java JDK 11+:

```shell
git clone https://github.com/your-username/snowflake-maven-gradle-plugins.git
cd snowflake-maven-gradle-plugins/

# Compile and run unit tests
mvn test

# Install a snapshot of the plugin into your local .m2 repository
mvn install
```

The snapshot of the plugin in the local .m2 repository should now be accessible from other Maven projects.
You can install the plugin in other projects' POM files as described in [Usage](README.md#usage-maven).

### Testing

Unit tests can be run with: 

```shell
mvn test
```

A Snowflake Account is needed for integration tests. Create a `profile.properties` file in `/src/it/`:

> WARNING: Ensure that the credential you provide below are for a development account/database. Don't use production credentials.
  
```properties
# ACCOUNT property will not work for integration tests
URL = ACCOUNT_NAME.snowflakecomputing.com:443
USER = username
PASSWORD = password
# optional properties
DB = database
WAREHOUSE = warehouse
SCHEMA = schema
```

To run integration tests: 
```shell
# Set execution privileges for shell scripts
chmod -R +x src/it 
# Then, integration tests can be run the `run-its` maven profile:
mvn verify -P run-its
```

## Gradle

Clone the repository

```shell
git clone https://github.com/your-username/snowflake-maven-gradle-plugins.git
cd snowflake-maven-gradle-plugins/
```

Install a snapshot of the plugin into your local .m2 repository

```shell
gradle publishToMavenLocal
```

The plugin SNAPSHOT in the local Maven repository should now be accessible from other local Gradle projects.
You can install the plugin in other projects POM as described in [Usage](README.md#usage-gradle) with one difference:

In local Gradle projects where you want to use the snowflake plugin, specify the following at the top of `settings.gradle` instead of using Maven central:

```groovy
pluginManagement {
    repositories {
        mavenLocal() // local Maven instead of Maven Central
    }
}
```

Now you can test your changes in a test project.

### IntelliJ

To contribute to the gradle plugin:
IntelliJ `File` -> `Open` -> select `build.gradle` file -> Open as project

To contribute to the maven plugin:
IntelliJ `File` -> `Open` -> select `pom.xml` file -> Open as project

To switch between the two plugins, delete the `.idea` IntelliJ cache folder and open the other plugin

If IntelliJ intellisense is highlighting errors within `snowflake_gradle_plugin` or `snowflake_maven_plugin`, you can fix it with the following:

- Close the IDE
- Open the project with IntelliJ File -> Open -> select `build.gradle` file -> Open as project
- In the 'Maven' tab of IntelliJ, click the refresh icon to "Reload all maven projects"

This ensures that both Maven and Gradle are active on their respective compile sources, which will help resolve intellisense errors

## Pull Requests

