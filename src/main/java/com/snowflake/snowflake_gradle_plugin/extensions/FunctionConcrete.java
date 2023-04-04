package com.snowflake.snowflake_gradle_plugin.extensions;

import com.snowflake.core.UserDefined;

public class FunctionConcrete extends UserDefinedConcrete{
    public FunctionConcrete(String name, String args, String handler, String returns) {
        super(name, args, handler, returns);
    }
    public FunctionConcrete(String name, String[] args, String handler, String returns) {
        super(name, args, handler, returns);
    }
    @Override
    public String getType() {
        return UserDefined.function;
    }

}
