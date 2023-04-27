package com.snowflake.plugins.udf.gradle;

import com.snowflake.plugins.udf.core.Snowflake;
import com.snowflake.plugins.udf.core.SnowflakeBuilder;
import com.snowflake.plugins.udf.core.UserDefined;
import com.snowflake.plugins.udf.gradle.extensions.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.slf4j.Logger;

/** Plugin publish task entry point. Executes all actions associated with snowflakePublish */
public class SnowflakeDeployTask extends DefaultTask {
  @Inject
  public SnowflakeDeployTask(File dependencyLogFile) {
    this.dependencyLogFile = dependencyLogFile;
  }

  private final String PLUGIN = "snowflake";
  private SnowflakeExtension extension =
      (SnowflakeExtension) getProject().getExtensions().getByName(PLUGIN);
  private Logger logger = Logging.getLogger(SnowflakeDeployTask.class);
  // Snowflake connection authentication information for the task
  private AuthConfig auth;
  // Name of Snowflake stage for artifact uploads
  private String stage;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(option = "auth-url", description = "Override the Snowflake auth URL")
  private String authUrl;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(option = "auth-password", description = "Override the Snowflake auth password")
  private String authPassword;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(option = "auth-user", description = "Override the Snowflake auth user")
  private String authUser;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(option = "auth-role", description = "Override the Snowflake auth role")
  private String authRole;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(option = "auth-db", description = "Override the Snowflake auth db")
  private String authDb;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(option = "auth-schema", description = "Override the Snowflake auth schema")
  private String authSchema;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(
      option = "deploy-name",
      description = "Specify a function or procedure name for a new deploy")
  private String deployName;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(
      option = "deploy-type",
      description = "Supply either 'function' or 'procedure' for a new deploy")
  private String deployType;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(option = "deploy-args", description = "Specify arguments for a new deploy")
  private String deployArgs;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(option = "deploy-handler", description = "Specify the handler method for a new deploy")
  private String deployHandler;

  @Optional
  @Input
  @Getter
  @Setter
  @Option(option = "deploy-returns", description = "Specify the return type for a new deploy")
  private String deployReturns;

  // Temporary file which lists each dependency in the user project and their organization or
  // artifact ID. Populated by ListDependenciesTask
  private File dependencyLogFile;
  // Snowflake core instance configured for the user
  private Snowflake snowflake;
  // Set of Function/Procedure containers inputted by the user from their build file
  private Set<UserDefinedContainer> udxContainers = new HashSet<>();

  @TaskAction
  public void publish() throws IOException, SQLException {
    auth = extension.getAuth();
    stage = extension.getStage();
    // TODO: Follow the user's configuration for project.libsDir
    String buildDirectory = getProject().getBuildDir() + SnowflakePlugin.libsString;
    String artifactFileName =
        String.format("%s-%s.jar", getProject().getName(), getProject().getVersion());
    udxContainers.addAll(
        (Set<FunctionContainer>) getProject().getExtensions().getByName("functions"));
    udxContainers.addAll(
        (Set<ProcedureContainer>) getProject().getExtensions().getByName("procedures"));
    // Validate user configuration of functions and procedures by converting the Gradle API
    // containers into concrete Java objects
    Set<UserDefinedConcrete> concreteUdxs = new HashSet<>();
    for (UserDefinedContainer container : udxContainers) {
      concreteUdxs.add(container.concrete());
    }
    appendCliUdxIfDefined(concreteUdxs);
    validateUserConfig(concreteUdxs);
    SnowflakeBuilder builder = new SnowflakeBuilder(logger::info);
    // Get authentication options from properties file, gradle.build, and CLI to create
    // Snowflake connection
    configureSnowflakeAuth(builder);
    configureSnowflakeDeployParams(builder, stage, artifactFileName);
    // Create snowflake JDBC Connection
    try {
      snowflake = builder.create();
    } catch (SQLException e) {
      throw new RuntimeException(
              "Error creating JDBC connection to snowflake. You likely need to change/add information to your auth config for the plugin: ",
              e);
    }
    snowflake.createStage();
    snowflake.uploadArtifact(String.format("%s/%s", buildDirectory, artifactFileName));
    snowflake.uploadDependencies(
        String.format(buildDirectory + SnowflakePlugin.dependenciesString));
    for (UserDefinedConcrete udx : concreteUdxs) {
      snowflake.createFunctionOrProc(udx);
    }
    logger.info("Functions created!");
  }

  private void validateUserConfig(Set<UserDefinedConcrete> udxs) {
    if (stage == null) {
      throw new IllegalArgumentException("'stage' name for file upload must be provided");
    }
    if (udxs.size() == 0) {
      throw new IllegalArgumentException("At least one function or procedure must be specified");
    }
  }

  /**
   * Reads user auth configurations to configure snowflake connection
   *
   * @throws IOException
   * @throws SQLException
   */
  private void configureSnowflakeAuth(SnowflakeBuilder builder) throws IOException, SQLException {
    if (auth != null) {
      if (auth.getPropertiesFile() != null) {
        // User has supplied an authentication file name
        logger.info("Reading auth config from file: " + auth.getPropertiesFile());
        try {
          builder.configFile(auth.getPropertiesFile());
        } catch (IOException e) {
          throw new RuntimeException(
              "Error reading or accessing the specified properties file: ", e);
        }
      }
      // Read build.gradle auth config
      builder.config(auth.getAuthMap());
    }
    // Read CLI auth config
    builder.config("url", authUrl);
    builder.config("user", authUser);
    builder.config("password", authPassword);
    builder.config("role", authRole);
    builder.config("db", authDb);
    builder.config("schema", authSchema);
  }

  private void configureSnowflakeDeployParams(
          SnowflakeBuilder builder, String stageName, String artifactFileName) {
    builder.stageName(stageName);
    builder.artifactFileName(artifactFileName);
    try {
      builder.depsToStagePaths(mapDependenciesToStagePaths());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a map of dependency file names to their corresponding file path in the stage path Their
   * file paths will imitate the .m2 cache: Ex:
   * groupID[0]/groupID[1]/groupID[2]/.../artifactID/version/filename Takes as input the
   * `dependency.log` file created by ListDependenciesTask
   */
  private Map<String, String> mapDependenciesToStagePaths() throws IOException {
    // Reads the file produced by 'gradle dependencies' task
    LineIterator it = null;
    Map<String, String> result;
    try {
      it = FileUtils.lineIterator(dependencyLogFile, "UTF-8");
      result = mapDependenciesToStagePathsHelper(it);
    } catch (IOException e) {
      throw e;
    } finally {
      if (it != null) {
        it.close();
      }
    }
    logger.info("Mapped dependencies to stage paths: " + result);
    return result;
  }

  public static Map<String, String> mapDependenciesToStagePathsHelper(LineIterator lineIt) {
    Map<String, String> result = new HashMap<>();
    while (lineIt.hasNext()) {
      String line = lineIt.nextLine().trim();
      // The line contains a dependency.
      if (line.startsWith("\\") || line.startsWith("|") || line.startsWith("+")) {
        // (*) indicates the dependency has already been previously listed
        if (line.endsWith("(*)")) {
          continue;
        }
        // Ignore the leading non-alphabetical characters
        int startIndex = -1;
        for (int i = 0, n = line.length(); i < n; i++) {
          char c = line.charAt(i);
          if (Character.isAlphabetic(c)) {
            startIndex = i;
            break;
          }
        }
        String dependencyLine = line.substring(startIndex);
        // Extract dependency information
        String[] info = dependencyLine.split(":");
        String groupId = info[0];
        String artifactId = info[1];
        String versionInfo = info[2];
        // version information can contain the '->' characters to show a dependency version
        // conflict that was automatically resolved by Gradle. The version number on the right of
        // the
        // arrow is used.
        // e.g. com.fasterxml.jackson.core:jackson-annotations:2.13.2 -> 2.13.4
        if (versionInfo.contains("->")) {
          versionInfo = versionInfo.substring(versionInfo.indexOf("->") + 2).trim();
        }
        // version information with "(c)" indicates a dependency restriction. Remove extra symbol to
        // get the version
        if (versionInfo.endsWith("(c)")) {
          versionInfo = versionInfo.substring(0, versionInfo.length() - 3).trim();
        }
        String stagePath =
            String.format("%s/%s/%s", groupId.replace('.', '/'), artifactId, versionInfo);
        // Assume the file name is ArtifactId-VersionNumber.
        // TODO: Handle the case where the file name does not conform to the above format.
        // Can this occur?
        String fileName = String.format("%s-%s.jar", artifactId, versionInfo);
        result.put(fileName, stagePath);
      }
    }
    return result;
  }

  /**
   * Try to create a concrete Function/Procedure object if ANY of the CLI arguments for a
   * function/procedure are provided. Throws an error if the arguments are incomplete or malformed.
   * If new Function/Procedure object is created, append it to the UDFs list
   */
  private void appendCliUdxIfDefined(Set<UserDefinedConcrete> udxs) {
    UserDefinedConcrete udx;
    Set<String> udxCliParams = new HashSet<>();
    udxCliParams.add(deployType);
    udxCliParams.add(deployName);
    udxCliParams.add(deployArgs);
    udxCliParams.add(deployHandler);
    udxCliParams.add(deployReturns);

    for (String udxCliParam : udxCliParams) {
      if (udxCliParam != null) {
        if (deployType == null) {
          throw new IllegalArgumentException(
              "The type of the user defined creation must be specified. Type may be \"procedure\" or \"function\"");
        }
        switch (deployType.toLowerCase()) {
          case UserDefined.procedure:
            udx = new ProcedureConcrete(deployName, deployArgs, deployHandler, deployReturns);
            break;
          case UserDefined.function:
            udx = new FunctionConcrete(deployName, deployArgs, deployHandler, deployReturns);
            break;
          default:
            throw new IllegalArgumentException(
                "The specified type is not recognized. The type of the user defined creation may be \"procedure\" or \"function\"");
        }
        udxs.add(udx);
        break;
      }
    }
  }
}
