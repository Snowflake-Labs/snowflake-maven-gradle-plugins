package com.snowflake.snowflake_maven_plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to capture user configuration of function/procedure arguments from the POM file
 */
public class Arg {
    public String name;
    public String type;
    @Override
    public String toString() {
        return name + " " + type;
    }

    public boolean hasNull() {
        return name == null || type == null;
    }

    // Cannot make a new constructor or else Maven plugin will not be able to instantiate args object
    // TODO: Make a new inner class with a constructor and use a map to read from maven CLI/POM
    public static Arg[] fromString(String argsString) {
        if (argsString == null) {
            throw new IllegalArgumentException("The arguments of the function/procedure are missing. If there are no arguments, supply an empty string.");
        }
        if (argsString.length() == 0) {
            return new Arg[]{};
        }
        String[] stringArgs = argsString.split(",");
        List<Arg> result = new ArrayList<>();

        for (String stringArg : stringArgs) {
            stringArg = stringArg.trim();
            String[] stringArgSplit = stringArg.split(" +");
            if (stringArgSplit.length != 2) {
                throw new IllegalArgumentException(String.format("Malformed argument: %s, from args: %s", stringArg, argsString));
            }
            Arg arg = new Arg();
            arg.name = stringArgSplit[0];
            arg.type = stringArgSplit[1];
            result.add(arg);
        }
        return result.toArray(new Arg[result.size()]);
    }
}