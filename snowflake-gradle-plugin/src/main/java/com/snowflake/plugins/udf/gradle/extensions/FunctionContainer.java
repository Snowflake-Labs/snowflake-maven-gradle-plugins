package com.snowflake.plugins.udf.gradle.extensions;

/**
 * Container class for users to specify their functions for the CREATE FUNCTION DDL executed by the
 * plugin
 */
public class FunctionContainer extends UserDefinedContainer {
  public FunctionContainer(String name) {
    super(name);
  }
  // Returns a concrete data class from user input to the container
  public FunctionConcrete concrete() {
    return new FunctionConcrete(getName(), getArgs(), getHandler(), getReturns());
  }
}
