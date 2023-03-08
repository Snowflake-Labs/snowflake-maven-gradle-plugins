package com.snowflake.snowflake_maven_plugin.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.mockito.Mock;

public class SnowflakeBuilderTest {

  @Mock private Log log;

  @Test
  public void testConfig() {
    SnowflakeBuilder sb = new SnowflakeBuilder(log);
    // config should set options
    sb.config("name", "functionName");
    assertThat(sb.options, hasEntry("name", "functionName"));
    // config should overwrite existing options and use lowercase keys
    sb.config("NAME", "otherName");
    assertThat(sb.options, hasEntry("name", "otherName"));
    // config should set url and account with formatting
    sb.config("account", "myaccount");
    assertEquals(sb.url, "jdbc:snowflake://myaccount.snowflakecomputing.com:443");
    String[] urls =
        new String[] {
          "myaccount.snowflakecomputing.com",
          "  myaccount.snowflakecomputing.com",
          "myaccount.snowflakecomputing.com:443"
        };
    for (String url : urls) {
      sb.config("url", url);
      assertEquals("jdbc:snowflake://myaccount.snowflakecomputing.com:443", sb.url);
    }
    sb.config("url", "https://myaccount.snowflakecomputing.com");
    assertEquals(sb.url, "jdbc:snowflake://https://myaccount.snowflakecomputing.com:443");
  }
}
