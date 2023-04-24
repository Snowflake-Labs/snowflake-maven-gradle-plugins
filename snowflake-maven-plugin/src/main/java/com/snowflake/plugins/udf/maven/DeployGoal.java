package com.snowflake.plugins.udf.maven;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import com.snowflake.plugins.udf.core.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/** Plugin deploy goal entry point. Executes all actions associated with snowflake:deploy */
@Mojo(
    name = "deploy",
    defaultPhase = LifecyclePhase.DEPLOY,
    requiresDependencyResolution = ResolutionScope.TEST)
public class DeployGoal extends AbstractMojo {
  /**
   * The project currently being build.
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject mavenProject;

  /**
   * The current Maven session.
   *
   * @parameter expression="${session}"
   * @required
   * @readonly
   */
  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession mavenSession;

  /**
   * The Maven BuildPluginManager component.
   *
   * @component
   * @required
   */
  @Component private BuildPluginManager pluginManager;

  /**
   * The name of the stage to use or create
   *
   * @parameter expression="${session}"
   * @required
   * @readonly
   */
  @Parameter(property = "deploy.stage", required = true)
  private String stage;

  /**
   * The functions to create. Encapsulates function name, handler, arg names, arg types, return
   * types
   *
   * @parameter expression="${session}"
   * @required
   * @readonly
   */
  @Parameter(property = "deploy.functions")
  private Function[] functions;

  /**
   * The procedures to create. Encapsulates function name, handler, arg names, arg types, return
   * types
   *
   * @parameter expression="${session}"
   * @required
   * @readonly
   */
  @Parameter(property = "deploy.procedures")
  private Procedure[] procedures;

  @Parameter(property = "deploy.auth")
  private Map<String, String> auth;

  // Duplicate parameters from the auth configuration object so that users can specify auth
  // arguments through CLI
  // e.g. mvn snowflake-udx:deploy -Ddeploy.auth.url="testtest.snowflakecomputing.com"
  // Using deploy.auth.<param> naming convention for CLI arguments
  @Parameter(property = "deploy.auth.url")
  private String auth_url;

  @Parameter(property = "deploy.auth.user")
  private String auth_user;

  @Parameter(property = "deploy.auth.password")
  private String auth_password;

  @Parameter(property = "deploy.auth.role")
  private String auth_role;

  @Parameter(property = "deploy.auth.db")
  private String auth_db;

  @Parameter(property = "deploy.auth.schema")
  private String auth_schema;

  // Duplicate parameters from the functions/procedures configuration object so that users can
  // specify a single function through CLI
  @Parameter(property = "deploy.type")
  private String udx_type;

  @Parameter(property = "deploy.name")
  private String udx_name;

  @Parameter(property = "deploy.handler")
  private String udx_handler;

  @Parameter(property = "deploy.returns")
  private String udx_returns;

  @Parameter(property = "deploy.args")
  private String udx_args;

  // Path in build directory where dependencies are copied
  private String dependencyDirName = "dependency";
  // The plugin config parameter responsible for choosing the auth file
  private String authFileParamName = "propertiesFile";
  private String buildDirectory;
  private List<UserDefined> udxs;
  private Snowflake snowflake;
  private String dependencyListFile;

  public void execute() throws MojoExecutionException {
    // Collect Project details
    ExecutionEnvironment executionEnv =
        new ExecutionEnvironment(mavenProject, mavenSession, pluginManager);
    String artifactFileName = mavenProject.getBuild().getFinalName() + ".jar";
    buildDirectory = mavenProject.getBuild().getDirectory();
    dependencyListFile = String.format("%s/%s/dependencies.log", buildDirectory, dependencyDirName);
    // Validate user config
    udxs =
        Stream.concat(Arrays.stream(functions), Arrays.stream(procedures))
            .collect(Collectors.toList());
    appendCliUdxIfDefined(udxs);
    validateUserConfig();
    // Get authentication options from properties file, POM, and CLI to create Snowflake connection
    createSnowflakeConnection();
    // Copy dependencies to the "target/dependency" folder
    getLog()
        .info(
            "Execute copy dependencies. Destination: " + buildDirectory + "/" + dependencyDirName);
    executeDependencyCopyDependencies(executionEnv);
    executeDependencyList(executionEnv);
    try {
      snowflake.createStage(stage);
    } catch (SQLException e) {
      throw new MojoExecutionException("Error creating or accessing stage: ", e);
    }
    // Upload artifacts to stage
    Map<String, String> depsToStagePath;

    try {
      depsToStagePath = mapDependenciesToStagePaths();
      snowflake.uploadArtifact(String.format("%s/%s", buildDirectory, artifactFileName), stage);
      snowflake.uploadDependencies(
          String.format("%s/%s", buildDirectory, dependencyDirName), stage, depsToStagePath);
    } catch (SQLException e) {
      throw new MojoExecutionException("Error uploading: ", e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // Register UDFs and Procedures
    try {
      for (UserDefined udf : udxs) {
        snowflake.createFunctionOrProc(udf, stage, artifactFileName, depsToStagePath);
      }
    } catch (SQLException e) {
      throw new MojoExecutionException("Error creating function or procedure.", e);
    }
    getLog().info("Functions created!");
  }

  private void validateUserConfig() {
    if (udxs.size() == 0) {
      throw new IllegalArgumentException("At least one function or procedure must be specified");
    }
    for (UserDefined udx : udxs) {
      udx.throwIfNull();
    }
    String java17Warning =
        "Java 17 is not supported for the Snowflake UDX plugin and will likely cause errors. Your compiler release or target version is set to 17";
    Plugin compiler = mavenProject.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
    if (compiler != null) {
      if (compiler.getConfiguration() != null) {
        String compilerConfig = compiler.getConfiguration().toString();
        if (compilerConfig.contains("<release>17</release>")
            || compilerConfig.contains("<target>17</target>")) {
          getLog().warn(java17Warning);
        }
      }
    }
    Properties properties = mavenProject.getProperties();
    if (properties.getProperty("maven.compiler.release") != null
            && properties.getProperty("maven.compiler.release").contains("17")
        || properties.getProperty("maven.compiler.target") != null
            && properties.getProperty("maven.compiler.target").contains("17")) {
      getLog().warn(java17Warning);
    }
  }

  private void createSnowflakeConnection() throws MojoExecutionException {
    SnowflakeBuilder builder = new SnowflakeBuilder(getLog()::info);
    if (auth != null) {
      if (auth.containsKey(authFileParamName)) {
        // User has supplied an authentication file name
        getLog().info("Reading auth config from file: " + auth.get(authFileParamName));
        try {
          builder.configFile(auth.get(authFileParamName));
        } catch (IOException e) {
          throw new MojoExecutionException(
              "Error reading or accessing the specified properties file: ", e);
        }
      }
      // Read POM file auth config
      builder.config(auth);
    }
    // Read CLI auth config
    Map<String, String> authCliParams = new HashMap<>();
    authCliParams.put("url", auth_url);
    authCliParams.put("user", auth_user);
    authCliParams.put("password", auth_password);
    authCliParams.put("role", auth_role);
    authCliParams.put("db", auth_db);
    authCliParams.put("schema", auth_schema);
    builder.config(authCliParams);
    // Create snowflake JDBC Connection
    try {
      snowflake = builder.create();
    } catch (SQLException e) {
      throw new MojoExecutionException(
          "Error creating JDBC connection to snowflake. You likely need to change/add information to your auth config for the plugin: ",
          e);
    }
  }

  // Create a Function/Procedure object and append it to the UDFs list if ANY of the CLI arguments
  // for a function/procedure are provided
  // May produce a Function/Procedure with null properties so that the user config validation can
  // prompt the user for missing fields
  private void appendCliUdxIfDefined(List<UserDefined> udfs) {
    Set<String> udxCliParams = new HashSet<>();
    udxCliParams.add(udx_type);
    udxCliParams.add(udx_name);
    udxCliParams.add(udx_args);
    udxCliParams.add(udx_handler);
    udxCliParams.add(udx_returns);

    for (String udxCliParam : udxCliParams) {
      if (udxCliParam != null) {
        UserDefined udx =
            UserDefined.create(udx_type, udx_name, udx_args, udx_handler, udx_returns);
        udfs.add(udx);
        break;
      }
    }
  }

  private void executeDependencyCopyDependencies(ExecutionEnvironment executionEnv)
      throws MojoExecutionException {
    executeMojo(
        plugin(
            groupId("org.apache.maven.plugins"),
            artifactId("maven-dependency-plugin"),
            version("3.5.0")),
        goal("copy-dependencies"),
        configuration(
            element(name("includeScope"), "compile"),
            element(name("outputDirectory"), buildDirectory + "/" + dependencyDirName)),
        executionEnv);
  }

  // Gets all dependencies from "compile" scope. i.e. Ignores test dependencies like JUnit
  private void executeDependencyList(ExecutionEnvironment executionEnv)
      throws MojoExecutionException {
    executeMojo(
        plugin(
            groupId("org.apache.maven.plugins"),
            artifactId("maven-dependency-plugin"),
            version("3.5.0")),
        goal("list"),
        configuration(
            element(name("includeScope"), "compile"),
            element(name("outputFile"), dependencyListFile),
            element(name("outputAbsoluteArtifactFilename"), "true")),
        executionEnv);
  }

  // Creates a map of dependency file names to their corresponding file path in the stage path
  // Their file paths will imitate the .m2 cache:
  // Ex: groupID[0]/groupID[1]/groupID[2]/.../artifactID/version/filename
  private Map<String, String> mapDependenciesToStagePaths()
      throws MojoExecutionException, IOException {
    Map<String, String> result = new HashMap<>();
    // Create matcher on file
    String fileNames = "[^/]*\\.jar";
    Pattern pattern = Pattern.compile(fileNames);
    LineIterator it = FileUtils.lineIterator(new File(dependencyListFile), "UTF-8");
    ;
    try {
      it = FileUtils.lineIterator(new File(dependencyListFile), "UTF-8");
      while (it.hasNext()) {
        String line = it.nextLine().trim();
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
          String fileName = matcher.group();
          String[] info = line.split(":");

          String groupId = info[0];
          String artifactId = info[1];
          String version = info[3];
          String stagePath =
              String.format("%s/%s/%s", groupId.replace('.', '/'), artifactId, version);
          result.put(fileName, stagePath);
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException(
          "Unexpected error: snowflake-udx plugin failed to create dependency list file");
    } finally {
      it.close();
    }
    getLog().info("Mapped dependencies to stage paths: " + result.toString());
    return result;
  }
}
