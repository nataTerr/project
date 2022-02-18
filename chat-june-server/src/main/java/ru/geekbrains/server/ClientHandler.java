package ru.geekbrains.server;

import javax.sql.rowset.JdbcRowSet;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler {

    //работа с клиентом

    //запоминаем сокет, входящие/исходящие потоки, имя пользоателя
    private Server server; //ссылка на сервер, на котором сидим
    private Socket socket;
    private String userName;
    private DataInputStream in;
    private DataOutputStream out;

    public String getUserName() {
        return userName;
    }

    public ClientHandler(final Server server, Socket socket) {
        //обработчик клиентов
        try {
            this.server = server; //запомнили сервер
            this.socket = socket; //запомнили входящий сокет и открыли обработчики потоков для обмена
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                public void run() {
                    ClientHandler.this.logic();
                }
            }).start(); //запустили в потоке логику обработки клиентов
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logic() {
        //логика обработки клиентов
        try {
            while (!consumeAuthorizeMessage(in.readUTF())); //авторизация клиента
            while (consumeRegularMessage(in.readUTF())) ; //обработка сообщений от клиента
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Клиент " + userName + " отключился");
            server.unsubscribe(this); //удаление клиента из списка
            closeConnection(); //безопасное закрытие соединения
        }
    }


    private boolean consumeRegularMessage(String inputMessage) {
        //обработка сообщений от клиента
        if (inputMessage.startsWith("/")) {
            if (inputMessage.equals("/exit")) {
                //если клиент прислал /exit, то отпралем это же собщение ему для выхода и безопасного закрытия соединения
                sendMessage("/exit");
                return false;
            }
            if (inputMessage.startsWith("/w ")) {
                //для отправки личных сообщений между клиентами
                String[] tokens = inputMessage.split("\\s+", 3); // разбиваем на части: /w, клиент, сообщение
                server.sendPersonalMessage(this, tokens[1], tokens[2]); //отправка личных сообщений: отправитель, получатель, сообщение
            }
            return true;
        }
        server.broadcastMessage(userName + ": " + inputMessage); //отпраляем сообщения всем клиентом, если оно не начинается с /
        return true;
    }

    private boolean consumeAuthorizeMessage(String message) throws SQLException {
        //авторизация клиента
        if (message.startsWith("/auth ")) {
            //если начинается с /auth, разбиваем сообщение по пробелу
            String[] tokens = message.split("\\s+");
            if (tokens.length == 1) { //запрещаем вход без логина
                sendMessage("Укажите имя пользователя");
                return false;
            }
            if (tokens.length > 2) { //запрещаем ввод имя пользователя более 1 слова
                sendMessage("Имя пользователя должно состоять из 1 слова");
                return false;
            }
            String newUserName = JdbcAuth.getUserNickName(tokens[1]); //берем первый элемент сообщения - имя пользователя
            if (server.checkUsername(newUserName)) { //запрещаем вход под одинаковыми именами
                sendMessage("Логин уже занят");
                return false;
            }
            userName = newUserName;
            sendMessage("/authok " + userName); //отправляем команду, что авторизация прошла успешно
            sendMessage("Вы зашли в чат под именем " + userName);
            server.subscribe(ClientHandler.this);//добавляем клиента в список рассылки
            return true;
        } else {
            sendMessage("Необходимо авторизоваться");
            return false;
        }
    }


    public void sendMessage(String message) {
        //отпрака сообщений клиенту
        try {
            out.writeUTF(message);// клиенту отправляем его же сообщение
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        //безопасное закрытие соединения клиент-сервер
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

