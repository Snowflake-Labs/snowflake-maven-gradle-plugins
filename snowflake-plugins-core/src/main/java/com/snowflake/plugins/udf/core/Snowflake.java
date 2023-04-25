package com.snowflake.plugins.udf.core;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import net.snowflake.client.jdbc.SnowflakeConnectionV1;

/**
 * Class to execute create stage, upload file, create function/procedure commands on Snowflake
 * through a JDBC connection
 */
public class Snowflake {

  // Logger object passed from Maven/Gradle plugin
  private SnowflakeLogger sfLogger;
  private String artifactDirOnStage = "libs";
  private String dependencyDirOnStage = "dependency";
  private SnowflakeConnectionV1 conn;
  // A set of dependencies which did not resolve to JAR files and are therefore skipped on upload
  // These are usually Gradle platform dependencies or Maven Bill-Of-Materials (BOM) which do not
  // need to be uploaded
  private Set<String> skippedDependencies = new HashSet<>();

  // The name of the stage used for uploaded files
  private String stageName;
  // The name of the project artifact file
  private String artifactFileName;
  // A map of each dependency to its path on the stage
  private Map<String, String> depsToStagePaths;

  /**
   * Create a snowflake object representing a session with a logger
   *
   * @param logger
   * @param conn
   * @param stageName
   * @param artifactFileName
   * @param depsToStagePaths
   */
  public Snowflake(
      SnowflakeLogger logger,
      SnowflakeConnectionV1 conn,
      String stageName,
      String artifactFileName,
      Map<String, String> depsToStagePaths) {
    this.sfLogger = logger;
    this.conn = conn;
    this.stageName = normalizeStageLocation(stageName);
    this.artifactFileName = artifactFileName;
    this.depsToStagePaths = depsToStagePaths;
  }

  public void createStage() throws SQLException {
    sfLogger.info(String.format("Creating stage %s if not exists", stageName));
    conn.createStatement().execute(String.format("create stage if not exists %s", stageName));
    sfLogger.info("Stage located or created!");
  }

  public void uploadArtifact(String localFileName) throws SQLException {
    sfLogger.info("Uploading artifact JAR: " + localFileName);
    uploadFiles(localFileName, artifactDirOnStage, true);
    sfLogger.info("Artifact JAR uploaded!");
  }

  public void uploadDependencies(String localFilePath) throws SQLException {
    sfLogger.info("Uploading dependency JARs from: " + localFilePath);
    for (Map.Entry<String, String> entry : depsToStagePaths.entrySet()) {
      String dependencyFile = entry.getKey();
      String stagePath = entry.getValue();
      sfLogger.info("Uploading " + dependencyFile);
      String dependencyFilePath = String.format("%s/%s", localFilePath, dependencyFile);
      uploadDependencyIfExists(dependencyFilePath, stagePath, dependencyFile);
    }
    sfLogger.info("Dependency JARs uploaded!");
  }

  // Split up uploadDependencies function to avoid mocking files in tests
  public void uploadDependencyIfExists(
      String dependencyFilePath, String stagePath, String dependencyFile) throws SQLException {
    if (new File(dependencyFilePath).isFile()) {
      uploadFiles(dependencyFilePath, stagePath, false);
    } else {
      // If a jar file for the dependency is not found, skip it
      // These are usually Gradle platform dependencies or Maven Bill-Of-Materials (BOM) which do
      // not need to be uploaded
      skippedDependencies.add(dependencyFile);
      sfLogger.info(
          String.format(
              "Dependency jar not found at %s. This is not a problem if the dependency was a platform/bill-of-materials dependency",
              dependencyFilePath));
    }
  }

  public void uploadFiles(String localFileName, String pathOnStage, boolean overwrite)
      throws SQLException {
    Map<String, String> options = new HashMap<>();
    options.put("AUTO_COMPRESS", "FALSE");
    options.put("PARALLEL", "4");
    options.put("OVERWRITE", String.valueOf(overwrite));
    String sql =
        String.format(
            "PUT %s %s %s",
            normalizeLocalFile(localFileName),
            String.format("@%s/%s", stageName, pathOnStage),
            getOptionsStatement(options));
    conn.createStatement().execute(sql);
  }

  private String normalizeLocalFile(String file) {
    String trimFile = file.trim();
    // For PUT/GET commands, if there are any special characters including spaces in
    // directory and file names, it needs to be quoted with single quote. For example,
    // 'file:///tmp/load data' for a path containing a directory named "load data").
    // So, if `file` is single quoted, it doesn't make sense to add "file://".
    if (trimFile.startsWith("file://") || isSingleQuoted(trimFile)) {
      return trimFile;
    } else {
      return "file://" + trimFile;
    }
  }

  private boolean isSingleQuoted(String name) {
    return name.startsWith("'") && name.endsWith("'");
  }

  private String getOptionsStatement(Map<String, String> options) {
    StringBuilder statement = new StringBuilder();
    for (Map.Entry<String, String> option : options.entrySet()) {
      statement.append(String.format("%s = %s ", option.getKey(), option.getValue()));
    }
    return statement.toString().trim();
  }

  public String getImportString() {
    String artifact = artifactDirOnStage + "/" + artifactFileName;
    List<String> imports = new ArrayList<>();
    imports.add(artifact);
    for (Map.Entry<String, String> entry : depsToStagePaths.entrySet()) {
      String dependencyFile = entry.getKey();
      String stagePath = entry.getValue();
      if (!skippedDependencies.contains(dependencyFile)) {
        imports.add(stagePath + "/" + dependencyFile);
      }
    }
    List<String> importPaths = new ArrayList<>();
    for (String s : imports) {
      importPaths.add(String.format("'@%s/%s'", stageName, s));
    }
    return String.join(", ", importPaths);
  }

  public String getPackageString(String udxType) {
    if (udxType.equals(UserDefined.procedure)) {
      return "PACKAGES = ('com.snowflake:snowpark:latest')\n";
    }
    return "";
  }

  public void createFunctionOrProc(UserDefined udx) throws SQLException {
    String s =
        String.format(
                "CREATE OR REPLACE %s %s (%s)\n", udx.getType(), udx.getName(), udx.getInputs())
            + String.format("RETURNS %s\n", udx.getReturns())
            + "LANGUAGE java\n"
            + getPackageString(udx.getType())
            + String.format("HANDLER = '%s'\n", udx.getHandler())
            + String.format("IMPORTS = (%s);", getImportString());
    sfLogger.info("Running create function statement: ");
    sfLogger.info(s);
    conn.createStatement().execute(s);
  }

  private String normalizeStageLocation(String name) {
    String trimName = name.trim();
    if (trimName.startsWith("@")) {
      return trimName.substring(1);
    } else {
      return trimName;
    }
  }
}
