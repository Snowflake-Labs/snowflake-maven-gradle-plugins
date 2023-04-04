package com.snowflake.plugins.udf.gradle.extensions;

/**
 * Data class for users to specify their functions for the CREATE FUNCTION DDL executed by the
 * plugin
 */
public class FunctionContainer extends UserDefinedContainer {
  public FunctionContainer(String name) {
    super(name);
  }

  @Override
  public String getType() {
    return UserDefinedContainer.function;
  }
}