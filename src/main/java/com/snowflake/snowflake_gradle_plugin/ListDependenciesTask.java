package com.snowflake.snowflake_gradle_plugin;

import org.gradle.api.tasks.diagnostics.DependencyReportTask;

import java.io.File;

/**
 * Task class that extends the Gradle DependencyReportTask task to report implementation
 * dependencies as a file
 */
public class ListDependenciesTask extends DependencyReportTask {
  public ListDependenciesTask() {
    // Note: Gradle defines "implementation" dependencies as compile + runtime (non-test)
    // dependencies
    setConfiguration("implementation");
    setOutputFile(new File(getProject().getBuildDir() + "/libs/dependencies/dependency.log"));
  }
}
