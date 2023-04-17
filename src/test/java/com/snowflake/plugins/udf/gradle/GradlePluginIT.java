package com.snowflake.plugins.udf.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import net.snowflake.client.jdbc.SnowflakeConnectionV1;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.GradleRunner.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GradlePluginIT {
  private String itProjectsDir = "./src/it/";
  private String propertiesFilePath = itProjectsDir + "profile.properties";
  private SnowflakeConnectionV1 conn;
  private String stageName =
      "SNOWFLAKE_GRADLE_PLUGIN_INTEGRATION_TEST_"
          + Math.abs(new Random(System.nanoTime()).nextInt());

  @Before
  public void setup() throws SQLException, IOException {
    Properties prop = new Properties();
    prop.load(new FileInputStream(propertiesFilePath));
    conn = new SnowflakeConnectionV1("jdbc:snowflake://" + prop.get("URL").toString(), prop);
  }

  @After
  public void cleanupEach() throws SQLException {
    conn.createStatement().execute("DROP STAGE IF EXISTS " + stageName);
  }

  @Test
  public void testSimpleUdfIt() throws Exception {
    String functionName = "simpleUdfItMyStringConcat";
    String testName = "simple-udf-it";
    Map<String, String> envVars = new HashMap<>();
    envVars.put("SNOWFLAKEPLUGINSTAGE", stageName);

    GradleRunner runner = GradleRunner.create()
            .withProjectDir(new File(itProjectsDir + testName))
            .withArguments("snowflakeDeploy", "--info")
            .withPluginClasspath()
            .withEnvironment(envVars);
    
      runner.build();


    //        try {
    //            runTest(testName);
    //            checkStringFunction(functionName, "'hi', ' there'", "hi there");
    //        } finally {
    //            Statement statement = conn.createStatement();
    //            statement.execute(String.format("DROP FUNCTION IF EXISTS %s (string, string)",
    // functionName));
    //        }
  }
}
