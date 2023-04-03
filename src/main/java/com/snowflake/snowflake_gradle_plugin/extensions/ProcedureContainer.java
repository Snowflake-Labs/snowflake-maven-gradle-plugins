package com.snowflake.snowflake_gradle_plugin.extensions;

/**
 * Data class for users to specify their procedures for the CREATE PROCEDURE DDL executed by the
 * plugin
 */
public class ProcedureContainer extends UserDefinedContainer {

  public ProcedureContainer(String name) {
    super(name);
  }

  @Override
  public String getType() {
    return UserDefinedContainer.procedure;
  }
}