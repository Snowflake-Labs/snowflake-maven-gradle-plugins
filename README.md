# Snowflake plugins for Maven and Gradle

[//]: # (Maven Central links: )

### [Jump to Maven instructions](#maven-configuration)
### [Jump to Gradle instructions](#gradle-configuration)

## Overview

This repo contains the source code for the Snowflake Maven and Gradle plugins, which will help developers publish [User-Defined Functions](https://docs.snowflake.com/en/sql-reference/udf-overview) (UDF) and [Stored Procedures](https://docs.snowflake.com/en/sql-reference/stored-procedures-overview) for Snowflake.
The plugins can create a stage on Snowflake, copy your build artifact and dependency `.jar` files to the stage, and run the `CREATE...` DDL to create your UDF or stored procedure in the account.

> Interested in contributing? See the [Contributing Guide](CONTRIBUTING.md) for guidance.

# Maven

## Maven Prereqs 

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

You can provide your account authentication information using a [properties file](https://docs.snowflake.com/en/developer-guide/snowpark/java/creating-session) or individually specifying your account parameters directly in the plugin config:

#### **Properties File**

Create a file, `profile.properties`, in the root of the project 
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

Then specify this file using the `<propertiesFile>` tag in the auth section:

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

#### **Auth fields**

Alternatively, you can specify your account information directly in the plugin using the `url`, `user`, and `password` fields. The `role`, `db`, and `schema` fields are optional. An example is shown below.

```xml
<plugin>
    <groupId>com.snowflake</groupId>
    <artifactId>snowflake-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <configuration>
        <auth>
            <url>https://MY_ACCOUNT_NAME.snowflakecomputing.com:443</url>
            <user>myUsername</user>
            <password>${env.MY_PASSWORD}</password> <!-- Env var injection for secrets -->
            <!-- optional auth configuration -->  
            <role>accountadmin</role>
            <db>${env.MY_ORG_DB}</db>
            <schema>${env.SCHEMA}</schema>
        </auth>
    </configuration>
</plugin>
```

> If a properties file **and** auth fields are specified in the plugin, then the values provided in the plugin are given priority.

### Object properties

Specify UDFs and Stored Procedures objects that should be created on Snowflake
by adding a new `<function>` tag under `<functions>` or a `<procedure>` tag under `<procedures>` for each object. The arguments follow the [`CREATE FUNCTION`](https://docs.snowflake.com/en/sql-reference/sql/create-function#syntax)
 and [`CREATE PROCEDURE`](https://docs.snowflake.com/en/sql-reference/sql/create-procedure) syntax:

- `<name>` is the name of the UDF/stored proc to be assigned on Snowflake
- `<handler>` is `className.methodName` for the handler method
- `<args>` is a list of `<arg>` which each require a `<name>` and `<type>`
- `<returns>` is the return type
- `<stage>` is the name of the internal stage that will be created (if it doesn't exist) and where files will be uploaded. Note: Choose a new stage name or an existing stage where artifact and dependency `.jar` files can be uploaded.

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

## Maven usage

After configuration, run:

```mvn
mvn clean package snowflake:deploy
```

`mvn clean package` will build the project and `mvn snowflake:deploy` executes the plugin goal, deploying your objects to Snowflake.

### Usage in CI pipelines

As mentioned in [auth fields](#auth-fields), your account properties can be read directly from the environment variables of your CI pipeline. This can be helpful for keeping secrets out of source control, and to deploy to different environments (QA, UAT, production) but just changing the env vars in different pipelines.

### Command Line Usage:

Auth parameters can optionally be provided as arguments when running the plugin from the CLI.
Values from CLI arguments will override any values set in the properties file **or** the POM:

```bash
mvn snowflake:deploy \
  -Ddeploy.auth.user="username" \
  -Ddeploy.auth.password="password" \
  -Ddeploy.auth.url="myaccount.snowflakecomputing.com" \
  -Ddeploy.auth.role="myrole" \
  -Ddeploy.auth.db="mydb" \
  -Ddeploy.auth.schema="myschema"
```

A single function or procedure can also be specified through command line arguments. 
The command line function/procedure will be created along with any objects defined in the POM. 
The arguments have the following syntax:

```bash
mvn snowflake:deploy \
  -Ddeploy.type="{procedure | function}" \
  -Ddeploy.name="<name>" \
  -Ddeploy.args="[ <arg_name> <arg_data_type> ] [ , ... ]" \
  -Ddeploy.handler="<class>.<handler>" \
  -Ddeploy.returns="<data_type>"
```

As an example:

```bash
mvn clean package snowflake:deploy \
  -Ddeploy.type="procedure" \
  -Ddeploy.name="mvnStringConcat" \
  -Ddeploy.args="a string, b string" \
  -Ddeploy.handler="SimpleUdf.stringConcat" \
  -Ddeploy.returns="string"
```

# Gradle

## Gradle Prereqs

| **Tool** | **Required Version** |
|----------|----------------------|
| JDK      | 11                   |

## Gradle Configuration

Add the plugin to your project's `build.gradle`

```groovy
plugins {
    id 'com.snowflake.snowflake-gradle-plugin' version '0.1.0-SNAPSHOT'
}
```

Specify the repository from which you will download the plugin by specify the following at the top of `settings.gradle` to use Maven Central:

```groovy
pluginManagement {
    repositories {
        mavenCentral()
    }
}
```

### Authentication

You can provide your account authentication information using a [properties file](https://docs.snowflake.com/en/developer-guide/snowpark/java/creating-session) or individually specifying your account parameters directly in the plugin config:

#### **Properties File**

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

In your `buid.gradle`, provide configuration for auth to the plugin:

```groovy
snowflake {
 auth {
  propertiesFile = "profile.properties"
 }
}
```

#### **Auth fields**

Alternatively, you can specify your account information directly in the plugin using the `url`, `user`, and `password` fields. The `role`, `db`, and `schema` fields are optional. An example is shown below.

```groovy
snowflake {
 auth {
  url = 'https://MY_ACCOUNT_NAME.snowflakecomputing.com:443'
  user = 'myUsername'
  password = System.getenv('SNOWFLAKEPWD') // Env var injection for secrets
  // Optional:
  role = 'accountadmin'
  db = 'myDB'
  schema = System.getenv('SNOWFLAKESCHEMA')
 }
}
```

> If a properties file **and** auth fields are specified in the plugin, then the values provided in the plugin are given priority.

### Gradle Plugin Configuration

Specify UDFs and Stored Procedures that should be published to Snowflake
by creating a new `function` closure in the `functions` block or `procedure` closure under `procedures` for each.

The arguments follow the [`CREATE FUNCTION`](https://docs.snowflake.com/en/sql-reference/sql/create-function#syntax)
and [`CREATE PROCEDURE`](https://docs.snowflake.com/en/sql-reference/sql/create-procedure) syntax:

- `functionName` or `procedureName` is the name to be used on Snowflake
- `handler` is `packageName.className.methodName` for the handler method
- `args` is a list of argument strings for the function which are formatted as "[ <arg_name> <arg_data_type> ] [ , ... ]"
- `returns` is the return type
- `stage` is the name of the internal stage that will be created (if it doesn't exist) and where files will be uploaded. Note: Choose a new stage name or an existing stage where artifact and dependency `.jar` files can be uploaded.

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

## Gradle Usage

After configuration, run the following to publish your functions and procedures:

```shell
gradle clean build snowflakeDeploy
```

### Usage in CI pipelines

As mentioned in [auth fields](#auth-fields-2), your account properties can be read directly from the environment variables of your CI pipeline. This can be helpful for keeping secrets out of source control, and to deploy to different environments (QA, UAT, production) but just changing the env vars in different pipelines.

### Command Line Usage

Auth parameters can optionally be provided as arguments when running the plugin from the CLI.
Values from CLI arguments will override any values set in the properties file or gradle build file:

```bash
gradle clean build snowflakeDeploy \
  --auth-url="myaccount.snowflakecomputing.com" \
  --auth-user="username" \
  --auth-password="password" \
  --auth-role="myrole" \
  --auth-db="mydb" \
  --auth-schema="myschema"
```

A single function or procedure can also be specified through command line arguments.
The command line function/procedure will be created along with any defined in `build.gradle`.
The arguments have the following syntax:

```bash
gradle clean build snowflakeDeploy \
  --deploy-type="{procedure | function}" \
  --deploy-name="<name>" \
  --deploy-args="[ <arg_name> <arg_data_type> ] [ , ... ]" \
  --deploy-handler="<class>.<handler>" \
  --deploy-returns="<data_type>"
```

As an example:

```bash
gradle clean build snowflakeDeploy \
  --deploy-type="procedure" \
  --deploy-name="mvnStringConcat" \
  --deploy-args="a string, b string" \
  --deploy-handler="SimpleUdf.stringConcat" \
  --deploy-returns="string"
```

## Notes

### Dependency reuse

When uploading to stage, the plugin will structure dependency artifacts like a local `.m2` cache,
with directories following an artifact's organization name and version.

By default, build artifacts will overwrite upon each publish
but existing dependencies files will not be uploaded again unless the version changes.

### Contributors

Special thanks to...

- [Stewart Bryson](https://github.com/stewartbryson) for guidance and providing a reference in his own Gradle plugin for Snowflake
- [Jonathan Cui](https://github.com/Jonathancui123) for bootstrapping the project during his internship at Snowflake
