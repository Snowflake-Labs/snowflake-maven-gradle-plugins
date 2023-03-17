# Snowflake UDX maven plugin

A maven plugin used to  will package and deploy a user’s UDFs and stored procedures. The plugin will

1. Build the project
2. Create a stage if the name doesn't already exist
3. Copy the .jar to the stage
4. Run the DDL to create the UDF/sproc

## Usage

1. `<Download the plugin depending on distribution method>`
2. Install to POM with the following:
`<pom config here>`
3. Create a `profile.properties` file with Snowpark Session information as described by on the [Snowpark documentation](https://docs.snowflake.com/en/developer-guide/snowpark/scala/creating-session.html#creating-a-session-for-snowpark). 
TODO: Allow session information to be read from the command line
4. Call `mvn package snowflake:deploy` in the root folder of the project to package artifacts, upload to stage, and register the function/procedure. 
Can supplement missing POM configurations through the command line by specifying arguments with a flag: `-Dcreate.argument=value`. E.g. `-Dcreate.stageName=someName -Dcreate.functionName=helloWorld`

Note: It's important to `mvn clean` if the dependencies of the project have changed

Note: Note that if your Snowflake account is hosted on Google Cloud Platform, PUT statements do not recognize when the OVERWRITE parameter is set to TRUE. A PUT operation always overwrites any existing files in the target stage with the local files you are uploading.

## For Developers

### Building

To build and install the plugin locally, clone and compile with [Apache Maven](https://maven.apache.org) and Java JDK 11+:

```shell
git clone https://github.com/Snowflake-Labs/snowflake-maven-gradle-plugin.git
cd snowflake-maven-gradle-plugin/

# Compile and run unit tests
mvn test

# Install a SNAPSHOT plugin version into the local .m2 repository
mvn install
```

### Testing

To run integration tests, we must be able to execute the shell scripts for running the plugin. In the project root:
```shell
chmod -R +x src/it 
```
