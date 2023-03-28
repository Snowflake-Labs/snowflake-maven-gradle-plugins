package com.snowflake.snowflake_maven_plugin;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Base class for representing users' Function and Procedure definitions */
public abstract class UserDefined implements com.snowflake.core.UserDefined {
  public abstract String getType();

  public static final String procedure = "procedure";
  public static final String function = "function";
  public String name;
  public String handler;
  public String returns;

  public Arg[] args;

  public String getName() {
    return name;
  }

  public String getHandler() {
    return handler;
  }

  public String getReturns() {
    return returns;
  }

  /**
   * This factory method is only used to create an instance from CLI arguments We don't define a
   * constructor so that Maven can populate instance members from the POM
   */
  public static UserDefined create(
      String type, String name, String args, String handler, String returns) {
    UserDefined udx;
    if (type == null) {
      throw new IllegalArgumentException(
          "The type of the user defined creation must be specified. Type may be \"procedure\" or \"function\"");
    }
    switch (type.toLowerCase()) {
      case procedure:
        udx = new Procedure();
        break;
      case function:
        udx = new Function();
        break;
      default:
        throw new IllegalArgumentException(
            "The specified type is not recognized. The type of the user defined creation may be \"procedure\" or \"function\"");
    }
    udx.name = name;
    udx.handler = handler;
    udx.returns = returns;
    udx.args = Arg.fromString(args);
    return udx;
  }

  public String getInputs() {
    return String.join(
        ", ", Arrays.stream(args).map(arg -> arg.toString()).collect(Collectors.toList()));
  }

  public void throwIfNull() {
    // TODO: Refactor to use a Map for maven input and a separate constructor in snowflake core to
    // avoid this type of error checking
    if (name == null) {
      throw new IllegalArgumentException("name not defined");
    } else if (handler == null) {
      throw new IllegalArgumentException("handler not defined");
    } else if (returns == null) {
      throw new IllegalArgumentException("returns not defined");
    } else if (args == null) {
      throw new IllegalArgumentException("args not defined");
    }
    for (Arg arg : args) {
      if (arg.hasNull()) {
        throw new IllegalArgumentException("malformed arg");
      }
    }
  }
}
