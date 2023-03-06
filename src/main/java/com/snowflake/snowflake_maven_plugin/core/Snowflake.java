package com.snowflake.snowflake_maven_plugin.core;

import com.snowflake.snowflake_maven_plugin.UserDefined;
import java.sql.SQLException;
import java.util.*;
import net.snowflake.client.jdbc.SnowflakeConnectionV1;
import org.apache.maven.plugin.logging.Log;

/**
 * Class to execute create stage, upload file, create function/procedure commands on Snowflake
 * through a JDBC connection
 */
public class Snowflake {

  private Log log;
  private String artifactDirOnStage = "libs";
  private String dependencyDirOnStage = "dependency";
  private SnowflakeConnectionV1 conn;

  /**
   * Create a snowflake object representing a session with a logger
   *
   * @param l
   * @param c
   */
  Snowflake(Log l, SnowflakeConnectionV1 c) throws SQLException {
    log = l;
    conn = c;
  }

  public void createStage(String stageName) throws SQLException {
    log.info(String.format("Creating stage @%s if not exists", stageName));
    conn.createStatement().execute(String.format("create stage if not exists %s", stageName));
    log.info("Stage located or created!");
  }

  public void uploadArtifact(String localFileName, String stageName) throws SQLException {
    log.info("Uploading artifact JAR: " + localFileName);
    uploadFiles(localFileName, stageName, artifactDirOnStage, true);
    log.info("Artifact JAR uploaded!");
  }

  public void uploadDependencies(
      String localFilePath, String stageName, Map<String, String> depsToStagePath)
      throws SQLException {
    log.info("Uploading dependency JARs from: " + localFilePath);
    for (Map.Entry<String, String> entry : depsToStagePath.entrySet()) {
      String dependencyFile = entry.getKey();
      String stagePath = entry.getValue();
      log.info("Uploading " + dependencyFile);
      uploadFiles(
          String.format("%s/%s", localFilePath, dependencyFile), stageName, stagePath, false);
    }
    log.info("Dependency JARs uploaded!");
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
      imports.add(stagePath + "/" + dependencyFile);
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
        String.format("CREATE OR REPLACE %s %s (%s)\n", udx.getType(), udx.name, udx.getInputs())
            + String.format("RETURNS %s\n", udx.returns)
            + "LANGUAGE java\n"
            + getPackageString(udx.getType())
            + String.format("HANDLER = '%s'\n", udx.handler)
            + String.format(
                "IMPORTS = (%s);",
                getImportString(
                    fileName,
                    stageName,
                    depsToStagePath)); // TODO: Store these variables as properties of the Snowflake
                                       // class so that we avoid passing variables through several
                                       // functions
    log.info("Running create function statement: ");
    log.info(s);
    conn.createStatement().execute(s);
  }
}
