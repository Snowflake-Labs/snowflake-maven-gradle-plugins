package com.snowflake.plugins.udf.gradle.extensions;

import com.snowflake.plugins.udf.core.UserDefined;
import lombok.Getter;
import lombok.Setter;

/** Base class for representing users' Function and Procedure definitions */
@Setter
@Getter
public abstract class UserDefinedContainer implements UserDefined {
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

  // Throws an illegalArgumentException if the user defined function/procedure does not follow the
  // expected format. This is used instead of a constructor because Gradle Extension Container API
  // expects to set instance methods using setter methods
  public void throwIfNull() {
    if (handler == null) {
      throw new IllegalArgumentException(
          String.format("handler not defined for %s %s", getType(), name));
    } else if (returns == null) {
      throw new IllegalArgumentException(
          String.format("returns not defined for %s %s", getType(), name));
    } else if (args == null) {
      throw new IllegalArgumentException(
          String.format("args not defined for %s %s", getType(), name));
    }
    for (String arg : args) {
      if (arg.trim().split(" ").length != 2) {
        throw new IllegalArgumentException(
            String.format(
                "%s %s: arguments must be formatted as \"<variableName> <variableType>\"",
                getType(), name));
      }
    }
  }
}
