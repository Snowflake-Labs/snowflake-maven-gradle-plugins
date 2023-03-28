package com.snowflake.snowflake_gradle_plugin;

import static com.snowflake.snowflake_gradle_plugin.SnowflakePublishTask.mapDependenciesToStagePathsHelper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.junit.Test;

public class SnowflakePublishTaskTest {
  private String dependencyLogPath = "src/test/resources/gradle-dependency-log/";

  @Test
  public void testMapDependenciesToStagePaths() throws IOException {
    Map<String, String> expectedSimple = new HashMap<>();
    expectedSimple.put("scala-reflect-2.12.11.jar", "org/scala-lang/scala-reflect/2.12.11");
    expectedSimple.put("scala-compiler-2.12.11.jar", "org/scala-lang/scala-compiler/2.12.11");
    expectedSimple.put("snowpark-1.7.0.jar", "com/snowflake/snowpark/1.7.0");
    expectedSimple.put("scala-library-2.12.11.jar", "org/scala-lang/scala-library/2.12.11");
    expectedSimple.put("gson-2.7.jar", "com/google/code/gson/gson/2.7");
    expectedSimple.put("scala-xml_2.12-1.0.6.jar", "org/scala-lang/modules/scala-xml_2.12/1.0.6");
    verifyDependenciesToStagePaths("logSimple.txt", expectedSimple);

    Map<String, String> expectedComplex = new HashMap<>();
    expectedComplex.put(
        "testing-support-lib-0.1.jar", "com/android/support/test/testing-support-lib/0.1");
    expectedComplex.put("objenesis-2.1.jar", "org/objenesis/objenesis/2.1");
    expectedComplex.put("javawriter-2.1.1.jar", "com/squareup/javawriter/2.1.1");
    expectedComplex.put("junit-4.11.jar", "junit/junit/4.11");
    expectedComplex.put("powermock-reflect-1.5.6.jar", "org/powermock/powermock-reflect/1.5.6");
    expectedComplex.put("javassist-3.18.2-GA.jar", "org/javassist/javassist/3.18.2-GA");
    expectedComplex.put("hamcrest-integration-1.1.jar", "org/hamcrest/hamcrest-integration/1.1");
    expectedComplex.put("hamcrest-core-1.3.jar", "org/hamcrest/hamcrest-core/1.3");
    expectedComplex.put(
        "espresso-core-2.0.jar", "com/android/support/test/espresso/espresso-core/2.0");
    expectedComplex.put(
        "powermock-api-mockito-1.5.6.jar", "org/powermock/powermock-api-mockito/1.5.6");
    expectedComplex.put("junit-dep-4.10.jar", "junit/junit-dep/4.10");
    expectedComplex.put("powermock-core-1.5.6.jar", "org/powermock/powermock-core/1.5.6");
    expectedComplex.put(
        "powermock-api-support-1.5.6.jar", "org/powermock/powermock-api-support/1.5.6");
    expectedComplex.put(
        "powermock-module-junit4-common-1.5.6.jar",
        "org/powermock/powermock-module-junit4-common/1.5.6");
    expectedComplex.put(
        "powermock-module-junit4-1.5.6.jar", "org/powermock/powermock-module-junit4/1.5.6");
    verifyDependenciesToStagePaths("logComplex.txt", expectedComplex);
  }

  // Runs the mapDependenciesToStagePathsHelper on the input depdendency.log file and compares with
  // the expected result
  private void verifyDependenciesToStagePaths(String inputFile, Map<String, String> expectedResult)
      throws IOException {
    LineIterator it = null;
    Map<String, String> result = null;
    try {
      it = FileUtils.lineIterator(new File(dependencyLogPath + inputFile), "UTF-8");
      result = mapDependenciesToStagePathsHelper(it);
    } catch (IOException e) {
      fail("IO Exception: " + e);
    } finally {
      if (it != null) {
        it.close();
      }
    }
    assertEquals(expectedResult, result);
  }
}
