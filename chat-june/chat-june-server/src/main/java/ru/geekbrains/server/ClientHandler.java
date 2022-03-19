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

            server.getClientExecutorService().execute(() -> {
                System.out.println(Thread.currentThread().getName());
                ClientHandler.this.logic();
            });

           /* new Thread(new Runnable() {
                public void run() {
                    ClientHandler.this.logic();
                }
            }).start(); //запустили в потоке логику обработки клиентов

            */
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public void logic() {
        //логика обработки клиентов
        try {

            while (true) {
                String message = in.readUTF();
                if (message.startsWith("/auth ")) {
                    consumeAuthorizeMessage(message);
                    //load();//авторизация клиента
                }
                if (message.startsWith("/reg ")) {
                    registrationAuth(message); //регистрация клиента
                }
                consumeRegularMessage(message);
            }

            //while (consumeRegularMessage(message)) ; //обработка сообщений от клиента
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

    public void load() {
        try {
            String message = server.loadHistory(userName);
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void consumeRegularMessage(String inputMessage) throws IOException {
        //обработка сообщений от клиента
        if (inputMessage.startsWith("/")) {
            if (inputMessage.equals("/exit")) {
                //если клиент прислал /exit, то отпралем это же собщение ему для выхода и безопасного закрытия соединения
                sendMessage("/exit");
                //    return false;
            }
            if (inputMessage.startsWith("/w ")) {
                //для отправки личных сообщений между клиентами
                String[] tokens = inputMessage.split("\\s+", 3); // разбиваем на части: /w, клиент, сообщение
                server.sendPersonalMessage(this, tokens[1], tokens[2]); //отправка личных сообщений: отправитель, получатель, сообщение
                // server.saveHistory(tokens[3]);
            }
            //   return true;
        }
        server.broadcastMessage(userName + ": " + inputMessage); //отпраляем сообщения всем клиентом, если оно не начинается с /
        // return true;
    }

    public void consumeAuthorizeMessage(String message) throws SQLException {
        //авторизация клиента
        // if (message.startsWith("/auth ")) {
        //если начинается с /auth, разбиваем сообщение по пробелу
        String[] tokens = message.split("\\s+");

        if (tokens.length == 1) { //запрещаем вход без логина
            sendMessage("Укажите имя пользователя");
            return;
            //   return false;
        }
        if (tokens.length > 2) { //запрещаем ввод имя пользователя более 1 слова
            sendMessage("Имя пользователя должно состоять из 1 слова");
            return;
            //  return false;
        }
        String newUserName = tokens[1]; //берем первый элемент сообщения - имя пользователя

        if (JdbcAuth.getUserName(newUserName) == null) {
            sendMessage("Необходимо пройти регистрацию");
            //  return false;
        } else {
            userName = newUserName;
            sendMessage("/authok " + userName); //отправляем команду, что авторизация прошла успешно
            sendMessage("Вы зашли в чат под именем " + userName);
            server.subscribe(ClientHandler.this);//добавляем клиента в список рассылки
        }
//            if (JdbcAuth.getUserName(newUserName) == null) { //добавляем в БД нового пользователя
//                JdbcAuth.insert(newUserName);
//            }


        // return true;
    }
//        else {
//            sendMessage("Необходимо авторизоваться");
//            return false;
//        }
    //}

    public void registrationAuth(String message) throws SQLException {
        //if (message.startsWith("/reg ")) {
        //если начинается с /reg, разбиваем сообщение по пробелу
        String[] tokens = message.split("\\s+");

        if (tokens.length == 1) { //запрещаем вход без логина
            sendMessage("Укажите имя пользователя");
            return;
            // return false;
        }
        if (tokens.length > 2) { //запрещаем ввод имя пользователя более 1 слова
            sendMessage("Имя пользователя должно состоять из 1 слова");
            return;
            //return false;
        }
        String newUserName = tokens[1];
        if (JdbcAuth.getUserName(newUserName) == null) { //добавляем в БД нового пользователя
            JdbcAuth.insert(newUserName);
            userName = newUserName;
            sendMessage("/authok " + userName); //отправляем команду, что авторизация прошла успешно
            sendMessage("Вы зашли в чат под именем " + userName);
            server.subscribe(ClientHandler.this);//добавляем клиента в список рассылки
        } else {
            sendMessage("Такой пользователь зарегистрирован, выполните вход в систему");
            //  return false;
        }
        // }
        //return true;
    }

    public void sendMessage(String message) {
        //отпрака сообщений клиенту
        try {
            out.writeUTF(message);// клиенту отправляем его же сообщение
            // server.saveHistory(message, userName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
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

