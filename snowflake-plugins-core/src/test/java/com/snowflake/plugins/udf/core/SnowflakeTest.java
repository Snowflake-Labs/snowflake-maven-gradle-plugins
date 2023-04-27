package com.snowflake.plugins.udf.core;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import net.snowflake.client.jdbc.SnowflakeConnectionV1;
import org.junit.Before;
import org.junit.Test;

public class SnowflakeTest {
  private SnowflakeConnectionV1 conn = mock(SnowflakeConnectionV1.class);
  private Statement statement = mock(Statement.class);
  private LoggerMock log = new LoggerMock();

  @Before
  public void before() throws SQLException {
    reset(conn, statement);
    when(conn.createStatement()).thenReturn(statement);
    when(statement.execute(anyString())).thenReturn(true);
  }

  @Test
  public void testUploadArtifact() throws SQLException {
    Snowflake sf = new Snowflake(log::info, conn, "mvn_stage", "myproject.jar", new HashMap<>());
    sf.uploadArtifact("hi/myproject.jar");
    sf = new Snowflake(log::info, conn, " mvn_stage  ", "myproject.jar", new HashMap<>());
    sf.uploadArtifact("hi/myproject.jar");
    verify(statement, times(2))
        .execute(
            "PUT file://hi/myproject.jar @mvn_stage/libs OVERWRITE = true AUTO_COMPRESS ="
                + " FALSE PARALLEL = 4");
    sf = new Snowflake(log::info, conn, "@mvn_stage", "tmp/load data", new HashMap<>());
    sf.uploadArtifact("'file:///tmp/load data'");
    verify(statement)
        .execute(
            "PUT 'file:///tmp/load data' @mvn_stage/libs OVERWRITE = true"
                + " AUTO_COMPRESS = FALSE PARALLEL = 4");
  }

  @Test
  public void testUploadDependencies() throws SQLException {
    Map<String, String> depsToStagePath = new HashMap<>();
    depsToStagePath.put("gson-2.10.jar", "com/google/gson/2.10/gson-2.10.jar");
    depsToStagePath.put("gson-2.11.jar", "com/google/gson/2.11/gson-2.11.jar");
    depsToStagePath.put("dep.jar", "com/google/dep/1.2.3/dep-1.2.3.jar");
    Snowflake sf = spy(new Snowflake(log::info, conn, "stage", "artifact", depsToStagePath));
    doNothing().when(sf).uploadDependencyIfExists(anyString(), anyString(), anyString());
    sf.uploadDependencies("target/dependency");

    verify(sf)
        .uploadDependencyIfExists(
            "target/dependency/dep.jar", "com/google/dep/1.2.3/dep-1.2.3.jar", "dep.jar");
    verify(sf)
        .uploadDependencyIfExists(
            "target/dependency/gson-2.10.jar",
            "com/google/gson/2.10/gson-2.10.jar",
            "gson-2.10.jar");
    verify(sf)
        .uploadDependencyIfExists(
            "target/dependency/gson-2.11.jar",
            "com/google/gson/2.11/gson-2.11.jar",
            "gson-2.11.jar");
  }

  @Test
  public void testUploadFiles() throws SQLException {
    Snowflake sf = new Snowflake(log::info, conn, "mystage", "artifact", new HashMap<>());
    sf.uploadFiles("target/dependency/dep.jar", "com/google/dep/1.2.3/dep-1.2.3.jar", false);
    sf.uploadFiles("target/dependency/gson-2.10.jar", "com/google/gson/2.10/gson-2.10.jar", false);
    sf.uploadFiles("target/dependency/gson-2.11.jar", "com/google/gson/2.11/gson-2.11.jar", false);
    verify(statement)
        .execute(
            "PUT file://target/dependency/dep.jar"
                + " @mystage/com/google/dep/1.2.3/dep-1.2.3.jar OVERWRITE = false"
                + " AUTO_COMPRESS = FALSE PARALLEL = 4");
    verify(statement)
        .execute(
            "PUT file://target/dependency/gson-2.10.jar"
                + " @mystage/com/google/gson/2.10/gson-2.10.jar OVERWRITE = false"
                + " AUTO_COMPRESS = FALSE PARALLEL = 4");
    verify(statement)
        .execute(
            "PUT file://target/dependency/gson-2.11.jar"
                + " @mystage/com/google/gson/2.11/gson-2.11.jar OVERWRITE = false"
                + " AUTO_COMPRESS = FALSE PARALLEL = 4");
  }

  @Test
  public void testGetImportString() throws SQLException {
    Map<String, String> depsToStagePath = new HashMap<>();
    depsToStagePath.put("gson-2.10.jar", "com/google/gson/2.10/gson-2.10.jar");
    depsToStagePath.put("gson-2.11.jar", "com/google/gson/2.11/gson-2.11.jar");
    depsToStagePath.put("dep.jar", "com/google/dep/1.2.3/dep-1.2.3.jar");
    Snowflake sf = new Snowflake(log::info, conn, "mystage", "project.jar", depsToStagePath);

    String importString = sf.getImportString();
    assertEquals(
        "'@mystage/libs/project.jar',"
            + " '@mystage/com/google/dep/1.2.3/dep-1.2.3.jar/dep.jar',"
            + " '@mystage/com/google/gson/2.10/gson-2.10.jar/gson-2.10.jar',"
            + " '@mystage/com/google/gson/2.11/gson-2.11.jar/gson-2.11.jar'",
        importString);
  }
}
