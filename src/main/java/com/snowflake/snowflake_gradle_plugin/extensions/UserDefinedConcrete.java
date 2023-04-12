package com.snowflake.snowflake_gradle_plugin.extensions;

/**
 * Base class for concrete functions/procedures data class. Performs input validation from user configuration
 */
public abstract class UserDefinedConcrete implements com.snowflake.core.UserDefined {
    // Constructor used to parse function/argument supplied on the CLI
    public UserDefinedConcrete(String name, String args, String handler, String returns) {
        this(name, parseStringArgs(args), handler, returns);
    }
    public UserDefinedConcrete(String name, String[] args, String handler, String returns) {
        if (name == null) {
            throw new IllegalArgumentException(
                    String.format("name not defined for %s", getType()));
        } else if (handler == null) {
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
            if (arg.trim().split(" +").length != 2) {
                throw new IllegalArgumentException(
                        String.format(
                                "%s %s: arguments must be formatted as \"<variableName> <variableType>\"",
                                getType(), name));
            }
        }
        this.name = name;
        this.handler = handler;
        this.args = args;
        this.returns = returns;
    }

    // Parse function/procedure arguments string from the CLI into the expected Array format
    private static String[] parseStringArgs(String argsString) {
        if (argsString == null) {
            throw new IllegalArgumentException(
                    "The arguments of the function/procedure are missing. If there are no arguments, supply an empty string.");
        }
        if (argsString.length() == 0) {
            return new String[] {};
        }
        return argsString.split(",");
    }
    private String name;
    private String handler;
    private String returns;
    private String[] args;

    public String getInputs() {
        return String.join(", ", args);
    }

    public String getName() {
        return name;
    }

    public String getHandler() {
        return handler;
    }

    public String getReturns() {
        return returns;
    }
}
