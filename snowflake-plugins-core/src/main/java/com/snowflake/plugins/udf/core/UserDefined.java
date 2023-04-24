package com.snowflake.plugins.udf.core;

/**
 * Allow plugins to pass their information about users' procedures and functions to Snowflake class
 * Snowflake class runs CREATE FUNCTION/CREATE PROCEDURE DDL using this information
 */
public interface UserDefined {
  String procedure = "procedure";
  String function = "function";

  String getInputs();

  String getType();

  String getName();

  String getHandler();

  String getReturns();
}
