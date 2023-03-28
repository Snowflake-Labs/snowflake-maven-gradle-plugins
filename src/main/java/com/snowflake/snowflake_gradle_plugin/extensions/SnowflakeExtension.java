package com.snowflake.snowflake_gradle_plugin.extensions;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;
import org.gradle.api.Project;

/** Gradle Extension class to store user config for the Snowflake Plugin */
@Setter
@Getter
public class SnowflakeExtension {
  public SnowflakeExtension(Project project) {
    this.project = project;
  }

  private Project project;
  /** The name of the stage to use or create */
  private String stage;

  private AuthConfig auth;

  // Setter function for Gradle API to project a closure from the user's build file
  public void setAuth(Closure closure) {
    this.auth = new AuthConfig();
    project.configure(auth, closure);
  }
}
