package com.bnpp.app.dao;

import java.sql.Connection;

import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Component
public class DynamicDataSourceManager {

    public static HikariDataSource buildDataSource(String connectionString) {
    	String [] formattedConnectionString = connectionString.split("@");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + formattedConnectionString[0]);
        config.setUsername(formattedConnectionString[1]);
        config.setPassword(formattedConnectionString[2]);
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(600000);
        config.setIdleTimeout(300000);
        return new HikariDataSource(config);
    }
    public static boolean testConnection(String connectionString) {
        HikariDataSource ds = null;
        try {
            ds = buildDataSource(connectionString);
            try (Connection connection = ds.getConnection()) {
                return connection != null && !connection.isClosed();
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (ds != null && !ds.isClosed()) {
                ds.close();
            }
        }
    }
}
