# Snowflake Maven/Gradle plugin

## Overview
This project is a maven plugin which will help developers publish [User-Defined Functions](https://docs.snowflake.com/en/sql-reference/udf-overview) (UDF) and [Stored Procedures](https://docs.snowflake.com/en/sql-reference/stored-procedures-overview) for Snowflake.
The plugin can create a stage on Snowflake, copy artifact and dependency `.jar` files to the stage, and run the command to create the User-Defined Function or Stored procedure.

The corresponding Gradle plugin is under development. 

[//]: # (Maven Central link: )

The plugin will:
1. Accept user configuration for a Snowflake connection and one or more UDFs or stored procedures
2. Create a stage if the stage doesn't exist
3. `PUT` build and dependency artifacts onto the stage, based on dependencies declared in the project's `POM.xml` or `build.gradle`. 
4. Run the `CREATE FUNCTION` or `CREATE PROCEDURE`  DDL for each UDF/stored procedure with the necessary imports

# Maven
## Setup
Install the plugins from Maven Central in the future (WIP). For now, follow the[ local build and installation process. ](#Contributing)

## Prereqs
| **Tool** | **Required Version** |
|----------|----------------------|
| JDK      | 11                   |
| Maven    | 3                    |
TODO: Test other compatible JDK and Maven versions. 

## Usage

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

### Configuring for local development

Create a properties file `profile.properties` in the root of the project 
with information to establish a JDBC connection to your Snowflake account:
```properties
# profile.properties
# Supply either one of the URL or ACCOUNT properties
URL=https://MY_ACCOUNT_NAME.snowflakecomputing.com:443
ACCOUNT=MY_ACCOUNT_NAME
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

#### Dependency reuse
When uploading to stage, the plugin will structure dependency artifacts like a local `.m2` cache,
with directories following an artifact's organization name and version.

By default, build artifacts will overwrite upon each publish
but existing dependencies files will not be uploaded again unless the version changes.

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
```
mvn snowflake-udx:deploy -Ddeploy.auth.user=”username” -Ddeploy.auth.password=”password” -Ddeploy.auth.url=”myaccount.snowflakecomputing.com” -Ddeploy.auth.account=”myaccount” -Ddeploy.auth.role=”myrole” -Ddeploy.auth.db=”mydb” -Ddeploy.auth.schema=”myschema”
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

## Contributing
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