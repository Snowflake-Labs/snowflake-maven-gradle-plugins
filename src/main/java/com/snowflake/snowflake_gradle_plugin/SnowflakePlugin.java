package com.snowflake.snowflake_gradle_plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;

/**
 * Plugin class for Gradle Plugin
 * */
public class SnowflakePlugin implements Plugin<Project> {
  // Directory name for user project artifacts
  // This should match the user's configuration for project.libsDir
  public static final String libsString = "/libs";
  // Directory name to temporarily store user dependency artifacts before PUT them to Snowflake
  // stage
  public static final String dependenciesString = "/dependencies";

  @Override
  public void apply(Project project) {
    // Register tasks for the plugin
    TaskContainer tasks = project.getTasks();
    tasks.register("listDependenciesTask", ListDependenciesTask.class);
    tasks.create("copyDependenciesTask", CopyDependenciesTask.class);
  }
}
