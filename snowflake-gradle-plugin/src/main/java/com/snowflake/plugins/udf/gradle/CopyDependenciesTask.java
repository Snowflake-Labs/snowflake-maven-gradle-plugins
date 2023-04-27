package com.snowflake.plugins.udf.gradle;

import org.gradle.api.tasks.Copy;

/**
 * Task class that extends the Gradle Copy task to copy dependencies into a staging location for the
 * plugin
 */
public class CopyDependenciesTask extends Copy {
  public CopyDependenciesTask() {
    // Configure the source to the project's "runtime"  dependencies
    // This allows us to stage all the dependencies needed to run the client project functions on snowflake
    from(getProject().getConfigurations().getByName("runtimeClasspath"));
    into(
        getProject().getBuildDir()
            + SnowflakePlugin.libsString
            + SnowflakePlugin.dependenciesString);
  }
}
