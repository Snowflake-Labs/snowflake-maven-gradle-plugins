package com.snowflake.plugins.udf.maven;

/**
 * A class to capture configuration for functions from users' POM file This name is chosen to allow
 * users to specify "<function> ... </function> in plugin config
 * https://maven.apache.org/guides/mini/guide-configuring-plugins.html
 */
public class Function extends UserDefined {

  @Override
  public String getType() {
    return UserDefined.function;
  }
}
