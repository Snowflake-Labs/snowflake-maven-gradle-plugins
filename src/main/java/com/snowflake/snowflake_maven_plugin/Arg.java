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

    /**
     * This factory method is used to parse UDF/procedure arguments supplied on the command line
     * e.g. mvn snowflake-udx:deploy ... -Ddeploy.args="a string, b string" ...
     * The input to this method will be "a string, b string"
     * And it will output and object array [Arg(a, string), Arg(b, string)]
     * We cannot make a new constructor or else Maven plugin will not be able to instantiate args object
     */
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