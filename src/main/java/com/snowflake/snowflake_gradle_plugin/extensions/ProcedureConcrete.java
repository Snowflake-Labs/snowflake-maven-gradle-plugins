package com.snowflake.snowflake_gradle_plugin.extensions;

import com.snowflake.core.UserDefined;

/**
 * Data class to parse user procedures for the CREATE PROCEDURE DDL executed by the
 * plugin
 */
public class ProcedureConcrete extends UserDefinedConcrete{
    public ProcedureConcrete(String name, String args, String handler, String returns) {
        super(name, args, handler, returns);
    }
    public ProcedureConcrete(String name, String[] args, String handler, String returns) {
        super(name, args, handler, returns);
    }

    @Override
    public String getType() {
        return UserDefined.procedure;
    }
}
