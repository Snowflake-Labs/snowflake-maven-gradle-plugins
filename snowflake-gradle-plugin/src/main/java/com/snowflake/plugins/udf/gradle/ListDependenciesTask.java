package com.snowflake.plugins.udf.gradle;

import java.io.File;
import org.gradle.api.tasks.diagnostics.DependencyReportTask;

/**
 * Task class that extends the Gradle DependencyReportTask task to report implementation
 * dependencies as a file
 */
public class ListDependenciesTask extends DependencyReportTask {
  public ListDependenciesTask() {
    // Configure the source to the project's "runtime"  dependencies
    // This allows us to list all the dependencies needed to run the client project functions on snowflake
    setConfiguration("runtimeClasspath");
    setOutputFile(new File(getProject().getBuildDir() + "/libs/dependencies/dependency.log"));
  }
}
