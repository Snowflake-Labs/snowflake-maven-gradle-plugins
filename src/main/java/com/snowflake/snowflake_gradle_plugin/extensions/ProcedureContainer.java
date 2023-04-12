package com.snowflake.snowflake_gradle_plugin.extensions;

/**
 * Container class for users to specify their procedures for the CREATE PROCEDURE DDL executed by the
 * plugin
 */
public class ProcedureContainer extends UserDefinedContainer {
  public ProcedureContainer(String name) {
    super(name);
  }
  // Returns a concrete data class from user input to the container
  public ProcedureConcrete concrete() {
    return new ProcedureConcrete(getName(), getArgs(), getHandler(), getReturns());
  }
}
