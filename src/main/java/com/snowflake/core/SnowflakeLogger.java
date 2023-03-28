package com.snowflake.core;

/**
 * Allow plugins to pass their Maven/Gradle logger to core package using this interface
 */
public interface SnowflakeLogger {
  void info(String s);
}
