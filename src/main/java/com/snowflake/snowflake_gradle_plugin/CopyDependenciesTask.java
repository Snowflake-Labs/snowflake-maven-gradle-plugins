package com.snowflake.snowflake_gradle_plugin;

import org.gradle.api.tasks.Copy;

/**
 * Task class that extends the Gradle Copy task to copy dependencies into a staging location for the
 * plugin
 */
public class CopyDependenciesTask extends Copy {
  public CopyDependenciesTask() {
    getProject().getConfigurations().getByName("implementation").setCanBeResolved(true);
    // Configure the source and destination directories to the project's "implementation"
    // dependencies
    // Note: Gradle defines "implementation" dependencies as compile + runtime (non-test)
    // dependencies
    from(getProject().getConfigurations().getByName("implementation"));
    into(
        getProject().getBuildDir()
            + SnowflakePlugin.libsString
            + SnowflakePlugin.dependenciesString);
  }
}
