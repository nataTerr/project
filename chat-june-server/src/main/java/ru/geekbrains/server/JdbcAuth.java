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
                    ");");
        } catch (SQLException e) {
            e.printStackTrace();
            //connection.rollback();
        }
    }

    public static void insert(String userName) throws SQLException {
        connect();
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO users(userName) VALUES (?, ?);");
            preparedStatement.setString(1, userName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static String getUserNickName(String userName) throws SQLException {
        String nickName = null;
        try {
            preparedStatement = connection.prepareStatement("SELECT nickName FROM users WHERE userName = ?;");
            preparedStatement.setString(1, userName);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                nickName = rs.getString(1);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nickName;
    }


    public static boolean changeUserNickname(String currentNickname, String newNickname) {
        try {
            preparedStatement = connection.prepareStatement("UPDATE users SET nickName = ? WHERE nickName = ?;");
            preparedStatement.setString(1, newNickname);
            preparedStatement.setString(2, currentNickname);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean deleteUser(String userName) {
        try {
            preparedStatement = connection.prepareStatement("DELETE FROM users WHERE userName = ?;");
            preparedStatement.setString(1, userName);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

}


