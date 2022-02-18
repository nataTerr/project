package ru.geekbrains.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Server {
    //работа на сервере
    private List<ClientHandler> clients; //создаем список клиентов

    public Server() {

        try {
            JdbcAuth.connect();
            this.clients = new ArrayList<ClientHandler>();
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("Сервер запущен. Ожидаем подключение клиентов..");

            while (true) { //подключение клиентов
                final Socket socket = serverSocket.accept(); //ожидание подключения клиента
                System.out.println("Клиент подключился ");
                ClientHandler client = new ClientHandler(this, socket); //запускаем обработчик клиента, работаем с клиентом
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            JdbcAuth.disconnect();
            System.out.println("Server closed");
        }
    }

    public synchronized void subscribe(ClientHandler client) throws SQLException {
        //добаление нового клиента в список
        broadcastMessage("Добавлен новый клиент " + client.getUserName());
        clients.add(client); //добавляем клиента в список
        JdbcAuth.insert(client.getUserName());
        broadcastClientList(); //рассылка всем клиентам списка
    }

    public synchronized void unsubscribe(ClientHandler client) {
        //удаление клиента из списка
        clients.remove(client); //удаляем клиента из списка
        broadcastMessage("Клиент " + client.getUserName() + " вышел из чата");
        JdbcAuth.deleteUser(client.getUserName());
        broadcastClientList(); //рассылка всем клиентам списка
    }

    public void broadcastMessage(String message) {
        //рассылка сообщений всем авторизованным клиентам
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void broadcastClientList() {
        //собираем имена всех клиентов в строку и отправляем всем клиентам
        StringBuilder builder = new StringBuilder(clients.size() * 10);
        builder.append("/clients_list ");

        for (ClientHandler client : clients) {
            builder.append(client.getUserName()).append(" ");
        }
        String clientList = builder.toString();
        broadcastMessage(clientList);
    }

    public synchronized boolean checkUsername(String username) {
        //проверка логина на дублирование
        //  boolean res = true;
        for (ClientHandler client : clients) {
            if (username.equalsIgnoreCase(client.getUserName())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendPersonalMessage(ClientHandler sender, String receiverUsername, String message) {
        //отпрака личных сообщений
        if (sender.getUserName().equalsIgnoreCase(receiverUsername)) {
            sender.sendMessage("Нельзя отправлять личные сообщения самому себе");
            return;
        }
        for (ClientHandler client : clients) {
            if (client.getUserName().equalsIgnoreCase(receiverUsername)) {
                client.sendMessage("от " + sender.getUserName() + ": " + message);
                sender.sendMessage("пользователю " + receiverUsername + ": " + message);
                return;
            }
        }
        sender.sendMessage("Пользователь " + receiverUsername + " не в сети");
    }
}
