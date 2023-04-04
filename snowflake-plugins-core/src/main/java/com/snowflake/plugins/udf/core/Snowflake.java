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

  /**
   * Create a snowflake object representing a session with a logger
   *
   * @param l
   * @param c
   */
  public Snowflake(SnowflakeLogger l, SnowflakeConnectionV1 c) throws SQLException {
    sfLogger = l;
    conn = c;
  }

  public void createStage(String stageName) throws SQLException {
    sfLogger.info(String.format("Creating stage @%s if not exists", stageName));
    conn.createStatement().execute(String.format("create stage if not exists %s", stageName));
    sfLogger.info("Stage located or created!");
  }

  public void uploadArtifact(String localFileName, String stageName) throws SQLException {
    sfLogger.info("Uploading artifact JAR: " + localFileName);
    uploadFiles(localFileName, stageName, artifactDirOnStage, true);
    sfLogger.info("Artifact JAR uploaded!");
  }

  public void uploadDependencies(
      String localFilePath, String stageName, Map<String, String> depsToStagePath)
      throws SQLException {
    sfLogger.info("Uploading dependency JARs from: " + localFilePath);
    for (Map.Entry<String, String> entry : depsToStagePath.entrySet()) {
      String dependencyFile = entry.getKey();
      String stagePath = entry.getValue();
      sfLogger.info("Uploading " + dependencyFile);
      String dependencyFilePath = String.format("%s/%s", localFilePath, dependencyFile);
      if (new File(dependencyFilePath).isFile()) {
        uploadFiles(dependencyFilePath, stageName, stagePath, false);
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
    sfLogger.info("Dependency JARs uploaded!");
  }

  public void uploadFiles(
      String localFileName, String stageName, String pathOnStage, boolean overwrite)
      throws SQLException {
    Map<String, String> options = new HashMap<>();
    options.put("AUTO_COMPRESS", "FALSE");
    options.put("PARALLEL", "4");
    options.put("OVERWRITE", String.valueOf(overwrite));
    String sql =
        String.format(
            "PUT %s %s %s",
            normalizeLocalFile(localFileName),
            String.format("%s/%s", normalizeStageLocation(stageName), pathOnStage),
            getOptionsStatement(options));
    conn.createStatement().execute(sql);
  }

  // File upload written to replicate ThunderSnow PUT FileOperation
  private String normalizeStageLocation(String name) {
    String trimName = name.trim();
    if (trimName.startsWith("@")) {
      return trimName;
    } else {
      return "@" + trimName;
    }
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

  public String getImportString(
      String artifactFileName, String stageName, Map<String, String> depsToStagePath) {
    String artifact = artifactDirOnStage + "/" + artifactFileName;
    List<String> imports = new ArrayList<>();
    imports.add(artifact);
    for (Map.Entry<String, String> entry : depsToStagePath.entrySet()) {
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

  public void createFunctionOrProc(
      UserDefined udx, String stageName, String fileName, Map<String, String> depsToStagePath)
      throws SQLException {
    String s =
        String.format(
                "CREATE OR REPLACE %s %s (%s)\n", udx.getType(), udx.getName(), udx.getInputs())
            + String.format("RETURNS %s\n", udx.getReturns())
            + "LANGUAGE java\n"
            + getPackageString(udx.getType())
            + String.format("HANDLER = '%s'\n", udx.getHandler())
            + String.format(
                "IMPORTS = (%s);",
                getImportString(
                    fileName, stageName, depsToStagePath)); // TODO: Store these variables as
    // properties of the Snowflake
    // class so that we avoid passing variables through several
    // functions
    sfLogger.info("Running create function statement: ");
    sfLogger.info(s);
    conn.createStatement().execute(s);
  }
}
