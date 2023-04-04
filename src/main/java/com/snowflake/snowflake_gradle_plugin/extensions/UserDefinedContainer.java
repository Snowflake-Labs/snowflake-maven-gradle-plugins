package com.snowflake.snowflake_gradle_plugin.extensions;

import lombok.Getter;
import lombok.Setter;

/** Base class for representing users' Function and Procedure definitions */
@Setter
@Getter
public abstract class UserDefinedContainer implements com.snowflake.core.UserDefined {
  public UserDefinedContainer(String name) {
    this.name = name;
  }

  public abstract String getType();

  private String name;
  private String handler;
  private String returns;
  private String[] args;

  public String getInputs() {
    return String.join(", ", args);
  }
}
