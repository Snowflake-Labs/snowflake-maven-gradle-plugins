package com.snowflake.plugins.udf.gradle;

import com.snowflake.plugins.udf.gradle.extensions.FunctionContainer;
import com.snowflake.plugins.udf.gradle.extensions.ProcedureContainer;
import com.snowflake.plugins.udf.gradle.extensions.SnowflakeExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;

/** Plugin class for Gradle Plugin */
public class SnowflakePlugin implements Plugin<Project> {
  // Directory name for user project artifacts
  // This should match the user's configuration for project.libsDir
  public static final String libsString = "/libs";
  // Directory name to temporarily store user dependency artifacts before PUT them to Snowflake
  // stage
  public static final String dependenciesString = "/dependencies";

  @Override
  public void apply(Project project) {
    // Register extensions for the plugin
    ExtensionContainer extension = project.getExtensions();
    extension.create("snowflake", SnowflakeExtension.class);
    extension.add("functions", project.container(FunctionContainer.class));
    extension.add("procedures", project.container(ProcedureContainer.class));

    // Register tasks for the plugin
    TaskContainer tasks = project.getTasks();
    tasks.create("listDependenciesTask", ListDependenciesTask.class);
    tasks.create("copyDependenciesTask", CopyDependenciesTask.class);
    tasks
        .create(
            "snowflakeDeploy",
            SnowflakeDeployTask.class,
            tasks.getByName("listDependenciesTask").getOutputs().getFiles().iterator().next())
        .dependsOn("copyDependenciesTask")
        .dependsOn("listDependenciesTask");
  }
}
