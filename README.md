# Snowflake plugins for Maven and Gradle

## Overview
This repo contains the source code for the Snowflake Maven and Gradle plugins, which will help developers publish [User-Defined Functions](https://docs.snowflake.com/en/sql-reference/udf-overview) (UDF) and [Stored Procedures](https://docs.snowflake.com/en/sql-reference/stored-procedures-overview) for Snowflake.
The plugin can create a stage on Snowflake, copy artifact and dependency `.jar` files to the stage, and run the command to create the User-Defined Function or Stored procedure.

The corresponding Gradle plugin is under development. 

[//]: # (Maven Central link: )

At a high level, these plugins...

1. Accept user configuration for a Snowflake connection and one or more UDFs or stored procedures
2. Create a stage if the stage doesn't exist
3. `PUT` build and dependency artifacts onto the stage, based on dependencies declared in the project's `POM.xml` or `build.gradle`. 
4. Run the `CREATE FUNCTION` or `CREATE PROCEDURE`  DDL for each UDF/stored procedure with the necessary imports

> Interested in contributing? See the [Contributing Guide](CONTRIBUTING.md) for guidance.

# Maven

## Setup Maven
Install the plugins from Maven Central in the future (WIP). For now, follow the[ local build and installation process. ](#Contributing)

## Prereqs
| **Tool** | **Required Version** |
|----------|----------------------|
| JDK      | 11                   |
| Maven    | 3                    |

## Maven Configuration

Put the following Maven coordinates in the `<plugins>` block of the  POM file.

```xml
<plugin>
    <groupId>com.snowflake</groupId>
    <artifactId>snowflake-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</plugin>
```

### Authentication

You can provide your account authentication information using a [properties file](https://docs.snowflake.com/en/developer-guide/snowpark/java/creating-session) or directly in the plugin config. 

#### Properties File

Create a file ,`profile.properties`, in the root of the project 
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
    </configuration>
</plugin>
```

- `<propertiesFile>` should point to the auth properties file created above

#### Plugin Configuration

TODO:

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

### Maven Plugin Configuration

Specify UDFs and Stored Procedures that should be published to Snowflake
by creating a new `<function>` tag under `<functions>` or `<procedure>` tag under `<procedures>` for each.

The arguments follow the [`CREATE FUNCTION`](https://docs.snowflake.com/en/sql-reference/sql/create-function#syntax)
 and [`CREATE PROCEDURE`](https://docs.snowflake.com/en/sql-reference/sql/create-procedure) syntax:

- `<stage>` is the name of the internal stage that will be created (if it doesn't exist) and where files will be uploaded. Note: Choose a new stage name or an existing stage where artifact and dependency `.jar` files can be uploaded.
- `<name>` is the name of the UDF/stored proc to be assigned on Snowflake
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
                <handler>ClassName.MethodName</handler>
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
                <handler>ClassName.SomeMethodName</handler>
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

### Maven usage

After configuration, run:
```mvn
mvn clean package snowflake:deploy
```

`mvn clean package` will build the project and `mvn snowflake:deploy` executes the plugin goal, deploying your objects to Snowflake.

### Configuring for CI Pipelines

Auth can be read directly from the environment of your CI pipeline, instead of from a properties file. This can be helpful for keeping secrets out of source control, and to deplot to different environments (QA, production) but just changing the env vars.

Simply expose the following environment variables from the secrets provider:

```xml
<plugin>
    <groupId>com.snowflake</groupId>
    <artifactId>snowflake-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <configuration>
        <auth>
            <!-- Supply either one of the URL or ACCOUNT properties -->
            <url>${env.MY_URL}</url>
            <account>${env.MY_ACCOUNT}</account>
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

```bash
mvn snowflake-udx:deploy -Ddeploy.auth.user=”username” -Ddeploy.auth.password=”password” -Ddeploy.auth.url=”myaccount.snowflakecomputing.com” -Ddeploy.auth.account=”myaccount” -Ddeploy.auth.role=”myrole” -Ddeploy.auth.db=”mydb” -Ddeploy.auth.schema=”myschema”
```

A single function or procedure can also be specified through command line arguments. 
The command line function/procedure will be created along with any defined in the POM. 
The arguments have the following syntax:

```
mvn snowflake-udx:deploy \
  -Ddeploy.type=”{procedure | function}” \
  -Ddeploy.name=”<name>” \
  -Ddeploy.args=”[ <arg_name> <arg_data_type> ] [ , ... ]” \
  -Ddeploy.handler=”<class>.<handler>” \
  -Ddeploy.returns=”<data_type>”
```

As an example:
```
mvn clean package snowflake-udx:deploy -Ddeploy.type="procedure" -Ddeploy.name="mvnStringConcat" -Ddeploy.args="a string, b string" -Ddeploy.handler="SimpleUdf.stringConcat" -Ddeploy.returns="string"
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

Then:
```shell
gradle clean assemble snowflakePublish
```

## Notes

### Dependency reuse

When uploading to stage, the plugin will structure dependency artifacts like a local `.m2` cache,
with directories following an artifact's organization name and version.

By default, build artifacts will overwrite upon each publish
but existing dependencies files will not be uploaded again unless the version changes.