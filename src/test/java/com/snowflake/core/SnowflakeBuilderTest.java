package com.snowflake.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SnowflakeBuilderTest {
  private LoggerMock log = new LoggerMock();

  @Test
  public void testConfig() {
    SnowflakeBuilder sb = new SnowflakeBuilder(log::info);
    // config should set options
    sb.config("name", "functionName");
    assertThat(sb.options, hasEntry("name", "functionName"));
    // config should overwrite existing options and use lowercase keys
    sb.config("NAME", "otherName");
    assertThat(sb.options, hasEntry("name", "otherName"));
    // config should set url and account with formatting
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
