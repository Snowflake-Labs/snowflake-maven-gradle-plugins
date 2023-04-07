package com.snowflake.snowflake_gradle_plugin.extensions;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Data class for users to specify their authentication details to the Snowflake Plugin Uses
 * getter/setter methods instead of a constructor for Gradle Plugin API to set values
 */
@Setter
@Getter
public class AuthConfig {
  private String propertiesFile;
  private String url;
  private String password;
  private String user;
  private String role;
  private String db;
  private String schema;

  // Return a map with all object properties
  public Map<String, String> getAuthMap() {
    Map<String, String> result = new HashMap<>();
    result.put("url", url);
    result.put("password", password);
    result.put("user", user);
    result.put("role", role);
    result.put("db", db);
    result.put("schema", schema);
    return result;
  }
}
