package ru.geekbrains.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {
    @FXML
    TextArea chatWindow; //многострочное текстовое поле для вывода сообщений в чате

    @FXML
    TextField messageWindow; //однострочное текстовое поле для написания сообщения серверу

    @FXML
    HBox authPanel, msgPanel; //панели для аторизации, ввода сообщений

    @FXML
    TextField userName; //однострочное текстовое поле для авторизации

    @FXML
    ListView<String> clientListView; //список клиентов

    @FXML
    TextField userNameField; //однострочное текстовое поле для вывода логина

    private Socket socket; //соединение с сервером
    private DataInputStream in; //входящий поток для получения сообщений
    private DataOutputStream out; //исходящий поток для отправки сообщений
    private String username; //имя авторизованного клиента

    public void sendMessage() {
        //метод для отправки сообщений на сервер
        try {
            out.writeUTF(messageWindow.getText()); //с однострочного текстового поля отправляем сообщение по исходящему потоку
            messageWindow.clear();//текстовое окно очищаем
            messageWindow.requestFocus();//перекидываем курсор на текстовое окно
        } catch (IOException e) {
            e.printStackTrace();
            messageError("Невозможно отправить сообщение на сервер");
        }
    }

    public void sendCloseRequest() {
        //метод для закрытия клиента
        try {
            if (out != null) {
                out.writeUTF("/exit");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void auth() {
        //метод авторизации пользователя
        connect(); //открытие подключения к серверу
        try {
            out.writeUTF("/auth " + userName.getText()); //в окне для авторизации пишем /auth имя пользователя введенное клиентом
            userName.clear(); //очищаем поле
        } catch (IOException e) {
            messageError("Невозможно отправить запрос авторизации на сервер");
        }
    }

    public void connect() {
        //соединение с сервером
        if (socket != null && !socket.isClosed()) { //проверка на активное соединение, чтобы не открывать несколько соединений одному пользователю
            return;
        }
        try {
            socket = new Socket("localhost", 8189); //подключение к серверу
            in = new DataInputStream(socket.getInputStream()); //открываем входящий и исходящий потоки
            out = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(new Runnable() { //запускаем поток с чтением сообщений с сервера
                public void run() {
                    readMessageFromServer(); //чтение сообщений от сервера и их обработка
                }
            });
            readThread.start();
        } catch (IOException e) {
            messageError("Невозможно подключиться к серверу");
        }
    }

    private void readMessageFromServer() {
        //чтение и обработка сообщений от сервера
        try {
            while (true) {
                String inputMessage = in.readUTF(); //ждем сообщения от сервера и складываем в строку
                if (inputMessage.equals("/exit")) { //если пришло "/exit", закрыаем соединение
                    closeConnection();
                }
                if (inputMessage.startsWith("/authok ")) {
                    //если авторизация прошла успешно то получаем имя пользоателя, скрываем панель авторизации и показываем окно чата
                    String[] tokens = inputMessage.split("\\s+"); //приходит "/authok логин", разделяем по пробелу и запоминаем логин
                    username = tokens[1];
                    setAuth(true); //скрываем панель аторизации и показываем окно чата
                    useUserName(); //прописываем имя пользователя в интерфейсе
                    break;
                }
                chatWindow.appendText(inputMessage + "\n");//добавляем сообщение в чат
            }
            while (true) {
                final String inputMessage = in.readUTF(); //ждем сообщения от сервера и складываем в строку
                if (inputMessage.startsWith("/")) {
                    if (inputMessage.equals("/exit")) {
                        break;
                    }
                    if (inputMessage.startsWith("/clients_list ")) {
                        //создаем окно списка клиентов
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                String[] tokens = inputMessage.split("\\s+");
                                clientListView.getItems().clear();
                                for (int i = 1; i < tokens.length; i++) {
                                    clientListView.getItems().add(tokens[i]);
                                }
                            }
                        });
                    }
                    continue;
                }
                chatWindow.appendText(inputMessage + "\n");//добавляем сообщение в чат, кроме тех что начинаются с /
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    public void messageError(String message) {
        //сообщения об ошибках
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    private void closeConnection() {
        //метод для закрытия соединения сервер-клиент
        setAuth(false);
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

    public void setAuth(boolean auth) {
        //скрытие панели авторизации и открытие окна чата
        msgPanel.setVisible(auth);
        msgPanel.setManaged(auth);
        authPanel.setVisible(!auth);
        authPanel.setManaged(!auth);
        clientListView.setVisible(auth);
        clientListView.setManaged(auth);
        userNameField.setVisible(auth);
        userNameField.setManaged(auth);

        /*
        if(auth) {
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
            authPanel.setVisible(false);
            authPanel.setManaged(false);
        } else {
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            authPanel.setVisible(true);
            authPanel.setManaged(true);
        }

         */
    }

    public void clientsListDoubleClick(MouseEvent mouseEvent) {
        //отпрака личных сообщений по двойному щелчку мыши на клиенте
        if (mouseEvent.getClickCount() == 2) { //получили двойной клик мыши
            String selectedUser = clientListView.getSelectionModel().getSelectedItem();//получаем выбранного клиента, кому отправить сообщение
            messageWindow.setText("/w " + selectedUser + " ");//посылаем сообщение вида /w отправитель сообщение серверу для обработки
            messageWindow.requestFocus();
            messageWindow.selectEnd();
        }
    }

    public void useUserName() {
        //выод логина на интерфейс
        userNameField.setText("Ваш логин " + username);
    }
}
