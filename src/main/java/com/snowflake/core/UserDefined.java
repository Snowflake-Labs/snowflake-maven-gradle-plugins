package com.snowflake.core;

/**
 * Allow plugins to pass their information about users' procedures and functions to Snowflake object
 * Snowflake object runs CREATE FUNCTION/CREATE PROCEDURE DDL using this information
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
