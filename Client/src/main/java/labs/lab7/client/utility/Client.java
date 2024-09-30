package labs.lab7.client.utility;

import labs.lab7.common.exceptions.ServerConnectionException;
import labs.lab7.common.network.requests.Request;
import labs.lab7.common.network.responses.Response;
import labs.lab7.common.utility.Console;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client  {

    private final String host;
    private final int port;
    private final int reconnectionTimeout;
    private final int maxReconnectionAttempts;
    private final Console console;

    private SocketChannel socketChannel;

    public Client(String host, int port, int reconnectionTimeout, int maxReconnectionAttempts, Console console) {
        this.host = host;
        this.port = port;
        this.reconnectionTimeout = reconnectionTimeout;
        this.maxReconnectionAttempts = maxReconnectionAttempts;
        this.console = console;
    }

    public Response sendRequest(Request request) throws ServerConnectionException {
        try {
            if (!connect()) throw new IOException();
            writeObject(request);
            return readObject();
        } catch (ServerConnectionException e) {
            throw e;
        } catch (IOException e) {
            throw new ServerConnectionException("Нет соединения с сервером");
        } catch (ClassNotFoundException e) {
            throw new ServerConnectionException("Ошибка выполнения запроса");
        } finally {
            disconnect();
        }
    }

    private SocketChannel openSocketChannel() throws IOException {
        var socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(host, port));
        socketChannel.configureBlocking(false);
        return socketChannel;
    }

    public boolean connect() {
        if (socketChannel != null && socketChannel.isConnected()) return true;
        int reconnectionAttempts = 0;
        while (true) {
            try {
                if (reconnectionAttempts > 0) {
                    console.println("Повторная попытка подключения");
                }
                socketChannel = openSocketChannel();
                while (!socketChannel.finishConnect());
                return true;
            } catch (UnknownHostException e) {
                console.printError("Некорректный адрес сервера");
                return false;
            } catch (IOException e) {
                console.printError("Ошибка соединения с сервером");
                reconnectionAttempts++;
                try {
                    if (reconnectionAttempts >= maxReconnectionAttempts) {
                        console.printError("Превышено максимальное количество попыток соединения с сервером");
                        return false;
                    }
                    console.println("Повторная попытка через " + reconnectionTimeout / 1000 + " секунд");
                    Thread.sleep(reconnectionTimeout);
                } catch (InterruptedException ex) {
                    console.printError("Попытка соединения с сервером неуспешна");
                    return false;
                }
            }
        }
    }

    private Response readObject() throws IOException, ClassNotFoundException {
        ByteBuffer buffer = ByteBuffer.allocate(1024*10);
        while (true) {
            try {
                socketChannel.read(buffer);
                buffer.mark();
                byte[] buf = buffer.array();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buf);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                var response = (Response) objectInputStream.readObject();
                byteArrayInputStream.close();
                objectInputStream.close();
                return response;
            } catch (StreamCorruptedException | EOFException ignored) {}
        }
    }

    private void writeObject(Request request) throws ServerConnectionException {
        try(
            var byteArrayOutputStream = new ByteArrayOutputStream();
            var objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        ) {
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();
            byte[] bytes = byteArrayOutputStream.toByteArray();
            socketChannel.write(ByteBuffer.wrap(bytes));
        } catch (IOException e) {
            throw new ServerConnectionException("Ошибка при отправке запроса");
        }
    }

    public void disconnect() {
        try {
            socketChannel.close();
        } catch (IOException e) {
            console.printError("Нет подключения к серверу");
        }
    }
}
