package com.snowflake.snowflake_gradle_plugin;

import com.snowflake.core.Snowflake;
import com.snowflake.core.SnowflakeBuilder;
import com.snowflake.snowflake_gradle_plugin.extensions.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;

/** Plugin publish task entry point. Executes all actions associated with snowflakePublish */
public class SnowflakeDeployTask extends DefaultTask {
  @Inject
  public SnowflakeDeployTask(File dependencyLogFile) {
    this.dependencyLogFile = dependencyLogFile;
  }

  private String PLUGIN = "snowflake";
  private SnowflakeExtension extension =
      (SnowflakeExtension) getProject().getExtensions().getByName(PLUGIN);
  private Logger logger = Logging.getLogger(SnowflakeDeployTask.class);
  // Temporary file which lists each dependency in the user project and their organization or
  // artifact ID. Populated by ListDependenciesTask
  private File dependencyLogFile;
  // Snowflake core instance configured for the user
  private Snowflake snowflake;
  // Name of Snowflake stage for artifact uploads
  private String stage;
  private Set<UserDefinedContainer> udxs = new HashSet<>();

  @TaskAction
  public void publish() throws IOException, SQLException {
    stage = extension.getStage();
    // TODO: Follow the user's configuration for project.libsDir
    String buildDirectory = getProject().getBuildDir() + SnowflakePlugin.libsString;
    String artifactFileName =
        String.format("%s-%s.jar", getProject().getName(), getProject().getVersion());
    // TODO: Validate user configuration
    udxs.addAll((Set<FunctionContainer>) getProject().getExtensions().getByName("functions"));
    udxs.addAll((Set<ProcedureContainer>) getProject().getExtensions().getByName("procedures"));
    validateUserConfig();
    // TODO: Check for functions or procedure in CLI
    // Get authentication options from properties file, gradle.build, and CLI to create
    // Snowflake connection
    createSnowflakeConnection();
    snowflake.createStage(stage);
    Map<String, String> dependenciesToStagePaths = mapDependenciesToStagePaths();
    snowflake.uploadArtifact(String.format("%s/%s", buildDirectory, artifactFileName), stage);
    snowflake.uploadDependencies(
        String.format(buildDirectory + SnowflakePlugin.dependenciesString),
        stage,
        dependenciesToStagePaths);
    for (UserDefinedContainer udx : udxs) {
      snowflake.createFunctionOrProc(udx, stage, artifactFileName, dependenciesToStagePaths);
    }
    logger.info("Functions created!");
  }

  private void validateUserConfig() {
    if (stage == null) {
      throw new IllegalArgumentException("'stage' name for file upload must be provided");
    }
    if (udxs.size() == 0) {
      throw new IllegalArgumentException("At least one function or procedure must be specified");
    }
    for (UserDefinedContainer udx : udxs) {
      udx.throwIfNull();
    }
  }

  /**
   * Reads user configurations to create a connected Snowflake object
   *
   * @throws IOException
   * @throws SQLException
   */
  private void createSnowflakeConnection() throws IOException, SQLException {
    SnowflakeBuilder builder = new SnowflakeBuilder(logger::info);
    AuthConfig auth = extension.getAuth();
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
    // TODO: Read CLI auth config
    // Create snowflake JDBC Connection
    try {
      snowflake = builder.create();
    } catch (SQLException e) {
      throw new RuntimeException(
          "Error creating JDBC connection to snowflake. You likely need to change/add information to your auth config for the plugin: ",
          e);
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
}
