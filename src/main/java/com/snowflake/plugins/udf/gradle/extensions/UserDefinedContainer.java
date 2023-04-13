package com.snowflake.plugins.udf.gradle.extensions;

import lombok.Getter;
import lombok.Setter;

/** Base class users to configure Function and Procedure definitions */
@Setter
@Getter
public abstract class UserDefinedContainer {
  public UserDefinedContainer(String name) {
    this.name = name;
  }

  private String name;
  private String handler;
  private String returns;
  private String[] args;

  public abstract UserDefinedConcrete concrete();
}
