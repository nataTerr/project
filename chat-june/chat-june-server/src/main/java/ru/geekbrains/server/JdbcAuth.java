package ru.geekbrains.server;

import java.sql.*;

public class JdbcAuth {
    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement preparedStatement;

    public static void disconnect() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:java.db");
            System.out.println("Connected to the database");
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTable() throws SQLException {
        try {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "userName TEXT" +
                    "NOT NULL" +
                    "UNIQUE" +
                    ");");
        } catch (SQLException e) {
            e.printStackTrace();
            //connection.rollback();
        }
    }

    public static void insert(String userName) {
        try (final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users(userName) VALUES (?)")) {
            preparedStatement.setString(1, userName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static String getUserName(String userName){
        try (final PreparedStatement preparedStatement = connection.prepareStatement("SELECT userName FROM users WHERE userName = ?")) {
            preparedStatement.setString(1, userName);
            final ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
               return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}


