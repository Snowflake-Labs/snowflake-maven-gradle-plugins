package com.snowflake.snowflake_maven_plugin;

import static org.junit.Assert.assertEquals;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import net.snowflake.client.jdbc.SnowflakeConnectionV1;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MavenPluginIT {

  private String runScriptPath = "./src/it/runtest.sh";
  private String propertiesFilePath = "./src/it/profile.properties";
  private SnowflakeConnectionV1 conn;
  private String stageName =
      "SNOWFLAKE_MAVEN_PLUGIN_INTEGRATION_TEST_"
          + Math.abs(new Random(System.nanoTime()).nextInt());

  @Before
  public void setup() throws SQLException, IOException {
    Properties prop = new Properties();
    prop.load(new FileInputStream(propertiesFilePath));
    conn = new SnowflakeConnectionV1("jdbc:snowflake://" + prop.get("URL").toString(), prop);
  }

  @After
  public void cleanupEach() throws SQLException {
    System.out.println("DROP STAGE IF EXISTS " + stageName);
    conn.createStatement().execute("DROP STAGE IF EXISTS " + stageName);
  }

  @Test
  public void testSimpleUdfIt() throws Exception {
    String functionName = "simpleUdfItMyStringConcat";
    String testName = "simple-udf-it";
    try {
      runTest(testName);
      checkStringFunction(functionName, "'hi', ' there'", "hi there");
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
      checkStringFunction(functionName, "35", "{\"brand\":\"snowflake\",\"length\":35}");
      checkStringProcedure(procedureName, "'hi', ' there'", "hi there");
      checkStringFunction(functionName2, "'hi', ' there'", "hi there");
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
      runTestWithArgs(
          testName,
          true,
          String.format(
              "-Ddeploy.type=function -Ddeploy.name=%s "
                  + "-Ddeploy.handler=SimpleUdf.myStringConcat "
                  + "-Ddeploy.returns=string",
              functionName),
          "-Ddeploy.args=a string, b string");
      checkStringFunction(functionName, "'hi', ' there'", "hi there");
    } finally {
      Statement statement = conn.createStatement();
      statement.execute(String.format("DROP FUNCTION IF EXISTS %s (string, string)", functionName));
    }
  }

  private static void inheritIO(final InputStream src, final PrintStream dest) {
    new Thread(
            new Runnable() {
              public void run() {
                Scanner sc = new Scanner(src);
                while (sc.hasNextLine()) {
                  dest.println(sc.nextLine());
                }
              }
            })
        .start();
  }

  // We pass "-Ddeploy.args="a string, b string"" as an unsplit arg to the runtest.sh script
  // This is so that the spaces in "-Ddeploy.args=a string, b string" doesn't cause this section to
  // be parsed as another lifecycle goal
  private void runTestWithArgs(String testName, boolean withArgs, String args, String unsplitArg) {
    // Start the process.
    ProcessBuilder processBuilder =
        new ProcessBuilder(
            runScriptPath, testName, String.valueOf(withArgs), stageName, args, unsplitArg);
    try {
      Process proc = processBuilder.start();
      inheritIO(proc.getInputStream(), System.out);
      inheritIO(proc.getErrorStream(), System.err);
      // wait for termination.
      proc.waitFor();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void runTest(String testName) {
    runTestWithArgs(testName, false, "", "");
  }

  private void checkStringFuncProc(
      String invocation, String functionName, String args, String expected) throws SQLException {
    Statement statement = conn.createStatement();
    ResultSet rs =
        statement.executeQuery(String.format("%s %s(%s)", invocation, functionName, args));
    rs.next();
    assertEquals(expected, rs.getString(1));
  }

  private void checkStringFunction(String functionName, String args, String expected)
      throws SQLException {
    checkStringFuncProc("SELECT", functionName, args, expected);
  }

  private void checkStringProcedure(String functionName, String args, String expected)
      throws SQLException {
    checkStringFuncProc("CALL", functionName, args, expected);
  }
}
