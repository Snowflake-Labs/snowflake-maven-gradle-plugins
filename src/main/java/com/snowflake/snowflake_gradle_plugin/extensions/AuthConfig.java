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

  public Map<String, String> getAuthMap() {
    Map<String, String> result = new HashMap<>();
    if (url != null) {
      result.put("url", url);
    }
    if (password != null) {
      result.put("password", password);
    }
    if (user != null) {
      result.put("user", user);
    }
    if (role != null) {
      result.put("role", role);
    }
    if (db != null) {
      result.put("db", db);
    }
    if (schema != null) {
      result.put("schema", schema);
    }
    return result;
  }
}
