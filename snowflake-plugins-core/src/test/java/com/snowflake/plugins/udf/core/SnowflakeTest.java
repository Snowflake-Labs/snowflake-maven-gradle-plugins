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
    Snowflake sf = new Snowflake(log::info, conn);
    sf.uploadArtifact("myproject.jar", "mvn_stage");
    sf.uploadArtifact(" myproject.jar  ", " mvn_stage  ");
    verify(statement, times(2))
        .execute(
            "PUT file://myproject.jar @mvn_stage/libs OVERWRITE = true AUTO_COMPRESS ="
                + " FALSE PARALLEL = 4");
    sf.uploadArtifact("'file:///tmp/load data'", "@mvn_stage");
    verify(statement)
        .execute(
            "PUT 'file:///tmp/load data' @mvn_stage/libs OVERWRITE = true"
                + " AUTO_COMPRESS = FALSE PARALLEL = 4");
  }

  //  @Test
  //  public void testUploadDependencies() throws SQLException {
  //    Snowflake sf = new Snowflake(log::info, conn);
  //    Map<String, String> depsToStagePath = new HashMap<>();
  //    depsToStagePath.put("gson-2.10.jar", "com/google/gson/2.10/gson-2.10.jar");
  //    depsToStagePath.put("gson-2.11.jar", "com/google/gson/2.11/gson-2.11.jar");
  //    depsToStagePath.put("dep.jar", "com/google/dep/1.2.3/dep-1.2.3.jar");
  //    sf.uploadDependencies("target/dependency", "mystage", depsToStagePath);
  //    verify(statement)
  //        .execute(
  //            "PUT file://target/dependency/dep.jar"
  //                + " @mystage/com/google/dep/1.2.3/dep-1.2.3.jar OVERWRITE = false"
  //                + " AUTO_COMPRESS = FALSE PARALLEL = 4");
  //    verify(statement)
  //        .execute(
  //            "PUT file://target/dependency/gson-2.10.jar"
  //                + " @mystage/com/google/gson/2.10/gson-2.10.jar OVERWRITE = false"
  //                + " AUTO_COMPRESS = FALSE PARALLEL = 4");
  //    verify(statement)
  //        .execute(
  //            "PUT file://target/dependency/gson-2.11.jar"
  //                + " @mystage/com/google/gson/2.11/gson-2.11.jar OVERWRITE = false"
  //                + " AUTO_COMPRESS = FALSE PARALLEL = 4");
  //  }

  @Test
  public void testGetImportString() throws SQLException {
    Snowflake sf = new Snowflake(log::info, conn);
    Map<String, String> depsToStagePath = new HashMap<>();
    depsToStagePath.put("gson-2.10.jar", "com/google/gson/2.10/gson-2.10.jar");
    depsToStagePath.put("gson-2.11.jar", "com/google/gson/2.11/gson-2.11.jar");
    depsToStagePath.put("dep.jar", "com/google/dep/1.2.3/dep-1.2.3.jar");
    String importString = sf.getImportString("project.jar", "mystage", depsToStagePath);
    assertEquals(
        "'@mystage/libs/project.jar',"
            + " '@mystage/com/google/dep/1.2.3/dep-1.2.3.jar/dep.jar',"
            + " '@mystage/com/google/gson/2.10/gson-2.10.jar/gson-2.10.jar',"
            + " '@mystage/com/google/gson/2.11/gson-2.11.jar/gson-2.11.jar'",
        importString);
  }
}
