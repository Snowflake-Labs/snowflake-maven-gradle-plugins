package com.snowflake.plugins.udf.core;

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.snowflake.client.jdbc.SnowflakeConnectionV1;

public class UdxChecker {

  public UdxChecker(SnowflakeConnectionV1 conn) {
    this.conn = conn;
  }

  private SnowflakeConnectionV1 conn;

  private void checkStringFuncProc(
      String invocation, String functionName, String args, String expected) throws SQLException {
    Statement statement = conn.createStatement();
    ResultSet rs =
        statement.executeQuery(String.format("%s %s(%s)", invocation, functionName, args));
    rs.next();
    assertEquals(expected, rs.getString(1));
  }

  public void checkStringFunction(String functionName, String args, String expected)
      throws SQLException {
    checkStringFuncProc("SELECT", functionName, args, expected);
  }

  public void checkStringProcedure(String functionName, String args, String expected)
      throws SQLException {
    checkStringFuncProc("CALL", functionName, args, expected);
  }
}
