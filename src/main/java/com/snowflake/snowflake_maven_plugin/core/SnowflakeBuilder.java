package com.snowflake.snowflake_maven_plugin.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.snowflake.client.jdbc.SnowflakeConnectionV1;
import org.apache.maven.plugin.logging.Log;

/** Builder class to configure Snowflake object from properties file and argument map */
public class SnowflakeBuilder {
  public Map<String, String> options = new HashMap<>();
  public String url;
  private Log log;

  public SnowflakeBuilder(Log l) {
    log = l;
  }

  public SnowflakeBuilder config(String key, String val) {
    key = key.toLowerCase();
    if (key.equals("account")) {
      url = formatUrl(String.format("%s.snowflakecomputing.com", val));
    } else if (key.equals("url")) {
      url = formatUrl(val);
    } else {
      options.put(key, val);
    }
    return this;
  }

  public SnowflakeBuilder config(Map<String, String> options) {
    for (Map.Entry<String, String> entry : options.entrySet()) {
      config(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public SnowflakeBuilder configFile(String filepath) throws IOException {
    Properties temp = new Properties();
    temp.load(new FileInputStream(filepath));
    for (Map.Entry<Object, Object> entry : temp.entrySet()) {
      config(entry.getKey().toString(), entry.getValue().toString());
    }
    return this;
  }

  public Snowflake create() throws SQLException {
    Properties prop = jdbcConfig(options);
    log.info("Creating connection to snowflake at url: " + url);
    SnowflakeConnectionV1 conn = new SnowflakeConnectionV1(url, prop);
    log.info("Snowflake Session established!");
    return new Snowflake(log, conn);
  }

  // Format the user provided url to match JDBC Connection url
  private String formatUrl(String url) {
    url = url.trim();
    // append :443 if no port number specified
    String urlWithPort = "^.+:\\d+$";
    if (!url.matches(urlWithPort)) {
      url = url + ":443";
    }
    return "jdbc:snowflake://" + url;
  }

  private Properties jdbcConfig(Map<String, String> options) {
    Properties config = new Properties();

    // Set JDBC memory to 10G by default, it can be override by user config
    String client_memory_limit = "client_memory_limit";
    config.put(client_memory_limit, "10240");

    for (Map.Entry<String, String> option : options.entrySet()) {
      // TODO: Process private key according to:
      // https://docs.snowflake.com/en/user-guide/jdbc-configure
      config.put(option.getKey(), option.getValue());
    }
    /*
     * Add this config so that the JDBC connector validates the user-provided
     * options when intializing the connection.
     */
    config.put("CLIENT_VALIDATE_DEFAULT_PARAMETERS", true);

    // Turn on session heart beat
    config.put("CLIENT_SESSION_KEEP_ALIVE", true);

    // log JDBC memory limit
    log.info(String.format("set JDBC client memory limit to %s", config.get(client_memory_limit)));
    return config;
  }
}