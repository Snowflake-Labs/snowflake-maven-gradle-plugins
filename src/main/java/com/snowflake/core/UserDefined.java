package com.snowflake.core;

public interface UserDefined {
  String procedure = "procedure";
  String function = "function";

  String getInputs();

  String getType();

  String getName();

  String getHandler();

  String getReturns();
}
