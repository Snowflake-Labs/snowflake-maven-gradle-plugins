package com.snowflake.snowflake_gradle_plugin.extensions;

import lombok.Getter;
import lombok.Setter;

/** Base class for representing users' Function and Procedure definitions */
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

  public abstract  UserDefinedConcrete concrete();
}
