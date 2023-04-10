# Snowflake Maven/Gradle plugin

[//]: # (Maven Central links: )
## Usage:
### [Jump to Maven usage](#usage-maven)
### [Jump to Gradle usage](#usage-gradle)

## Overview
This project is a maven and gradle plugin which will help developers publish [User-Defined Functions](https://docs.snowflake.com/en/sql-reference/udf-overview) (UDF) and [Stored Procedures](https://docs.snowflake.com/en/sql-reference/stored-procedures-overview) for Snowflake.
The plugin can create a stage on Snowflake, copy artifact and dependency `.jar` files to the stage, and run the command to create the User-Defined Function or Stored procedure.

The plugins will:
1. Accept user configuration for a Snowflake connection and one or more UDFs or stored procedures
2. Create a stage if the stage doesn't exist
3. `PUT` build and dependency artifacts onto the stage, based on dependencies declared in the project's `POM.xml` or `build.gradle`. 
4. Run the `CREATE FUNCTION` or `CREATE PROCEDURE`  DDL for each UDF/stored procedure with the necessary imports

# Maven
## Setup Maven
Install the plugins from Maven Central in the future (WIP). For now, follow the[ local build and installation process. ](#contributing-maven)

## Prereqs Maven
| **Tool** | **Required Version** |
|----------|----------------------|
| JDK      | 11                   |
| Maven    | 3                    |

## Usage Maven

Put the following Maven coordinates in the `<plugins>` block of the  POM file.

```xml
<plugin>
    <groupId>com.snowflake</groupId>
    <artifactId>snowflake-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</plugin>
```

After configuration, run:
```mvn
mvn clean package snowflake:deploy
```

`mvn clean package` will build the project and

`mvn snowflake:deploy` executes the plugin goal 

### Configuring for local usage

Create a properties file `profile.properties` in the root of the project 
with information to establish a JDBC connection to your Snowflake account:
```properties
# profile.properties
URL=https://MY_ACCOUNT_NAME.snowflakecomputing.com:443
USER=username
PASSWORD=password
# Optional properties:
ROLE=ACCOUNTADMIN
WAREHOUSE=DEMO_WH
DB=MY_DB
SCHEMA=MY_SCHEMA
```

Provide configuration for auth and stage to the plugin:
```xml
<plugin>
    <groupId>com.snowflake</groupId>
    <artifactId>snowflake-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <configuration>
        <auth>
            <propertiesFile>profile.properties</propertiesFile>
        </auth>
        <stage>STAGE_NAME</stage>
    </configuration>
</plugin>
```

- `<propertiesFile>` should point to the auth properties file created above
- `<stage>` is the name of the internal stage that will be created (if it doesn't exist) and where files will be uploaded. Note: Choose a new stage name or an existing stage where artifact and dependency `.Jar` files can be uploaded.


#### Configuring UDFs and Stored Procedures

Specify UDFs and Stored Procedures that should be published to Snowflake
by creating a new `<function>` tag under `<functions>` or `<procedure>` tag under `<procedures>` for each.

The arguments follow the [`CREATE FUNCTION`](https://docs.snowflake.com/en/sql-reference/sql/create-function#syntax)
 and [`CREATE PROCEDURE`](https://docs.snowflake.com/en/sql-reference/sql/create-procedure) syntax:

- `<name>` is the name to be assigned on Snowflake
- `<handler>` is `className.methodName` for the handler method
- `<args>` is a list of `<arg>` which each require a `<name>` and `<type>`
- `<returns>` is the return type

Example plugin configuration on POM:

```xml
<plugin>
    <groupId>com.snowflake</groupId>
    <artifactId>snowflake-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <configuration>
        <auth>
            <propertiesFile>profile.properties</propertiesFile>
        </auth>
        <stage>STAGE_NAME</stage>
        <functions>
            <function>
                <name>funcNameOnSnowflake</name>
                <handler>PackageName.ClassName.MethodName</handler>
                <args>
                    <arg>
                        <name>firstArg</name>
                        <type>integer</type>
                    </arg>
                    <arg>
                        <name>secondArg</name>
                        <type>string</type>
                    </arg>
                    <!-- More arg go here.. -->
                </args>
                <returns>string</returns>
            </function>
            <!-- More functions go here.. -->
        </functions>
        <procedures>
            <procedure>
                <name>procNameOnSnowflake</name>
                <handler>PackageName.ClassName.SomeMethodName</handler>
                <args>
                    <arg>
                        <name>a</name>
                        <type>string</type>
                    </arg>
                    <!-- More arg go here.. -->
                </args>
                <returns>string</returns>
            </procedure>
            <!-- More procedures go here.. -->
        </procedures>
    </configuration>
</plugin>
```

### Configuring for CI Pipelines

Auth can be read directly from the environment of your CI pipeline, instead of from a properties file.

Simply expose the following environment variables from the secrets provider:

```xml
<plugin>
    <groupId>com.snowflake</groupId>
    <artifactId>snowflake-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <configuration>
        <auth>
            <url>${env.MY_URL}</url>
            <user>${env.MY_USER}</user>
            <password>${env.MY_PASSWORD}</password>
            <!-- optional auth configuration -->  
            <role>${env.MY_ROLE}</role>
            <db>${env.MY_ORG_DB}</db>
            <schema>${env.SCHEMA}</schema>
          </auth>
    </configuration>
</plugin>
```

### Configuring with Command Line:
Auth parameters can optionally be provided as arguments when running the plugin from the CLI.
Values from CLI arguments will override any values set in the properties file or POM:
```
mvn snowflake-udx:deploy -Ddeploy.auth.user=”username” -Ddeploy.auth.password=”password” -Ddeploy.auth.url=”myaccount.snowflakecomputing.com” -Ddeploy.auth.role=”myrole” -Ddeploy.auth.db=”mydb” -Ddeploy.auth.schema=”myschema”
```

A single function or procedure can also be specified through command line arguments. 
The command line function/procedure will be created along with any defined in the POM. 
The arguments have the following syntax:
```
mvn snowflake-udx:deploy -Ddeploy.type=”{procedure | function}” -Ddeploy.name=”<name>” -Ddeploy.args=”[ <arg_name> <arg_data_type> ] [ , ... ]” -Ddeploy.handler=”<class>.<handler>” -Ddeploy.returns=”<data_type>”
```

As an example:
```
mvn clean package snowflake-udx:deploy -Ddeploy.type="procedure" -Ddeploy.name="mvnStringConcat" -Ddeploy.args="a string, b string" -Ddeploy.handler="SimpleUdf.stringConcat" -Ddeploy.returns="string"
```

## Contributing Maven
To build and install the plugin locally, clone and compile with [Apache Maven](https://maven.apache.org) and Java JDK 11+:

```shell
git clone https://github.com/Snowflake-Labs/snowflake-maven-gradle-plugin.git
cd snowflake-maven-gradle-plugin/

# Compile and run unit tests
mvn test

# Install a SNAPSHOT plugin version into the local .m2 repository
mvn install
```

The plugin SNAPSHOT in the local .m2 repository should now be accessible from other Maven projects.
You can install the plugin in other projects POM as described in [Usage](#usage).

See [IntelliJ](#intellij) for IDE usage tips with this project.

### Testing

Unit tests can be run with: 
```shell
mvn test
```

A Snowflake Account is needed for integration tests. Create a `profile.properties` file in `/src/it/`:

> WARNING: Ensure that the credential you provide below are for a safe development database.
  
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

# Gradle
## Setup Gradle
Install the plugin from Maven Central in the future (WIP). For now, follow the[ local build and installation process. ](#contributing-gradle)

## Prereqs
| **Tool** | **Required Version** |
|----------|----------------------|
| JDK      | 11                   |


## Usage Gradle
Add the plugin to your project's `build.gradle`
```groovy
plugins {
    id 'com.snowflake.snowflake-gradle-plugin' version '0.1.0-SNAPSHOT'
}
```

Specify the repository from which you will download the plugin by specify the following at the top of `settings.gradle` 
To use Maven Central:
```groovy
pluginManagement {
    repositories {
        mavenCentral()
    }
}
```

After configuration, run the following to publish your functions and procedures:
```shell
gradle clean assemble snowflakePublish
```

### Configuring for local usage
Create a properties file `profile.properties` in the root of the project
with information to establish a JDBC connection to your Snowflake account:
```properties
# profile.properties
URL=https://MY_ACCOUNT_NAME.snowflakecomputing.com:443
USER=username
PASSWORD=password
# Optional properties:
ROLE=ACCOUNTADMIN
WAREHOUSE=DEMO_WH
DB=MY_DB
SCHEMA=MY_SCHEMA
```

In your `buid.gradle`, provide configuration for auth and stage to the plugin:
```groovy
snowflake {
 auth {
  propertiesFile = "profile.properties"
 }
 stage = "STAGE_NAME"
}
```

- `propertiesFile` should point to the auth properties file created above
- `stage` is the name of the internal stage that will be created (if it doesn't exist) and where files will be uploaded. Note: Choose a new stage name or an existing stage where artifact and dependency `.Jar` files can be uploaded.

#### Configuring UDFs and Stored Procedures

Specify UDFs and Stored Procedures that should be published to Snowflake
by creating a new `function` closure in the `functions` block or `procedure` closure under `procedures` for each.

The arguments follow the [`CREATE FUNCTION`](https://docs.snowflake.com/en/sql-reference/sql/create-function#syntax)
and [`CREATE PROCEDURE`](https://docs.snowflake.com/en/sql-reference/sql/create-procedure) syntax:

- `functionName` or `procedureName` is the name to be used on Snowflake
- `handler` is `packageName.className.methodName` for the handler method
- `args` is a list of argument strings for the function which are formatted as "[ <arg_name> <arg_data_type> ] [ , ... ]"
- `returns` is the return type

Example plugin configuration on POM:

```groovy
plugins {
 id 'com.snowflake.snowflake-gradle-plugin'
}


snowflake {
 auth {
  propertiesFile = './path/to/file'
 }
 stage = 'STAGE_NAME'
 functions {
  functionName {
   args = ["a string", "b int"]
   returns = "string"
   handler = "PackageName.ClassName.methodName"
  }
  // More functions here
 }
 procedures {
  procedureName {
   args = ["a string, b string"]
   returns = "string"
   handler = "PackageName.ClassName.methodName"
  }
  // More procedures here
 }
}

```

### Configuring for CI Pipelines

Auth can be read directly from the environment of your CI pipeline, instead of from a properties file.

Simply expose the following environment variables from the secrets provider:

```groovy
snowflake {
 auth {
  url = System.getenv('SNOWFLAKEURL')
  user = System.getenv('SNOWFLAKEUSER')
  password = System.getenv('SNOWFLAKEPW')
  // Optional:
  role = System.getenv('SNOWFLAKEROLE')
  db = System.getenv('SNOWFLAKEDB')
  schema = System.getenv('SNOWFLAKESCHEMA')
 }
}
```


## Contributing Gradle
```shell
git clone https://github.com/Snowflake-Labs/snowflake-maven-gradle-plugin.git
cd snowflake-maven-gradle-plugin/

# Install a SNAPSHOT plugin version into the local .m2 repository
gradle publishToMavenLocal
```

The plugin SNAPSHOT in the local Maven repository should now be accessible from other local Gradle projects.
You can install the plugin in other projects POM as described in [Usage](#usage-gradle) with one difference:

In local Gradle projects where you want to use the snowflake plugin, specify the following at the top of `settings.gradle` instead of using Maven central:
```groovy
pluginManagement {
    repositories {
        mavenLocal()
    }
}
```

See [IntelliJ](#intellij) for IDE usage tips with this project.


### IntelliJ

To contribute to the gradle plugin:
IntelliJ `File` -> `Open` -> select `build.gradle` file -> Open as project

To contribute to the maven plugin:
IntelliJ `File` -> `Open` -> select `pom.xml` file -> Open as project

To switch between the two plugins, delete the `.idea` IntelliJ cache folder and open the other plugin 

If IntelliJ intellisense is highlighting errors within `snowflake_gradle_plugin` or `snowflake_maven_plugin`, you can fix it with the following:

- Close the IDE
- Open the project with IntelliJ `File` -> `Open` -> select `build.gradle` file -> Open as project
- In the 'Maven' tab of IntelliJ, click the refresh icon to "Reload all maven projects"

This ensures that both Maven and Gradle are active on their respective compile sources, which will help resolve intellisense errors


#### Dependency reuse
When uploading to stage, the plugin will structure dependency artifacts like a local `.m2` cache,
with directories following an artifact's organization name and version.

By default, build artifacts will overwrite upon each publish
but existing dependencies files will not be uploaded again unless the version changes.