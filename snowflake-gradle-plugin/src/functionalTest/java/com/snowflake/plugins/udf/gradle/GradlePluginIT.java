package com.snowflake.plugins.udf.gradle;

import net.snowflake.client.jdbc.SnowflakeConnectionV1;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class GradlePluginIT {
  private String itProjectsDir = "./src/it/";
  private String propertiesFilePath = itProjectsDir + "profile.properties";
  private SnowflakeConnectionV1 conn;
  private UdxChecker checker;

  private String stageName =
      "SNOWFLAKE_GRADLE_PLUGIN_INTEGRATION_TEST_"
          + Math.abs(new Random(System.nanoTime()).nextInt());

  @Before
  public void setup() throws SQLException, IOException {
    Properties prop = new Properties();
    prop.load(new FileInputStream(propertiesFilePath));
    conn = new SnowflakeConnectionV1("jdbc:snowflake://" + prop.get("URL").toString(), prop);
    checker = new UdxChecker(conn);
  }

  @After
  public void cleanupEach() throws SQLException {
    conn.createStatement().execute("DROP STAGE IF EXISTS " + stageName);
  }

  @Test
  public void testSimpleUdfIt() throws Exception {
    String functionName = "simpleUdfItMyStringConcat";
    String testName = "simple-udf-it";

    try {
      runTest(testName);
      checker.checkStringFunction(functionName, "'hi', ' there'", "hi there");
    } finally {
      Statement statement = conn.createStatement();
      statement.execute(String.format("DROP FUNCTION IF EXISTS %s (string, string)", functionName));
    }
  }

  @Test
  public void testMultiplUdfProc() throws Exception {
    String procedureName = "multipleUdfProcItMYSTRINGCONCAT";
    String functionName = "multipleUdfProcItMyNewJsonSki";
    String functionName2 = "multipleUdfProcItMyStringConcatWithLog";

    String testName = "multiple-udf-proc-it";
    try {
      runTest(testName);
      checker.checkStringFunction(functionName, "35", "{\"brand\":\"snowflake\",\"length\":35}");
      checker.checkStringProcedure(procedureName, "'hi', ' there'", "hi there");
      checker.checkStringFunction(functionName2, "'hi', ' there'", "hi there");
    } finally {
      Statement statement = conn.createStatement();
      statement.execute(
          String.format("DROP PROCEDURE IF EXISTS %s (string, string)", procedureName));
      statement.execute(String.format("DROP FUNCTION IF EXISTS %s (int)", functionName));
      statement.execute(
          String.format("DROP FUNCTION IF EXISTS %s (string, string)", functionName2));
    }
  }

  @Test
  public void testCliUdf() throws Exception {
    String functionName = "simpleUdfItMyStringConcat";
    String testName = "cli-udf-it";
    try {
      runTest(
          testName,
          List.of(
              "--deploy-type=function",
              String.format("--deploy-name=%s", functionName),
              "--deploy-handler=SimpleUdf.myStringConcat",
              "--deploy-args=a string, b string",
              "--deploy-returns=string"));
      checker.checkStringFunction(functionName, "'hi', ' there'", "hi there");
    } finally {
      Statement statement = conn.createStatement();
      statement.execute(String.format("DROP FUNCTION IF EXISTS %s (string, string)", functionName));
    }
  }

  public void runTest(String testName) {
    runTest(testName, List.of());
  }

  public void runTest(String testName, List<String> testArgs) {
    Map<String, String> envVars = new HashMap<>();
    envVars.put("SNOWFLAKEPLUGINSTAGE", stageName);
    List<String> args = new ArrayList<>(List.of("clean", "build", "snowflakeDeploy", "--info"));
    args.addAll(testArgs);
    GradleRunner.create()
        .withProjectDir(new File(itProjectsDir + testName))
        .withArguments(args)
        .withPluginClasspath()
        .withEnvironment(envVars)
        .forwardOutput()
        .build();
  }
}
