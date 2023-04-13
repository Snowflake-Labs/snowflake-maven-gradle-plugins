package com.snowflake.plugins.udf.maven;

/**
 * A class to capture configuration for procedures from users' POM file This name is chosen to allow
 * users to specify "<procedure> ... </procedure> in plugin config
 * https://maven.apache.org/guides/mini/guide-configuring-plugins.html
 */
public class Procedure extends UserDefined {

  @Override
  public String getType() {
    return procedure;
  }
}
