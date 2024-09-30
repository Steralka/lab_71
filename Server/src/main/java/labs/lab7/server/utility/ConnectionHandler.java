package labs.lab7.server.utility;

import labs.lab7.common.network.requests.Request;
import labs.lab7.common.utility.Console;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ConnectionHandler implements Runnable {

    private static final int POOL_SIZE = 4;

    private final RequestHandler requestHandler;
    private final Console console;
    private final ServerSocket serverSocket;
    private boolean running = true;

    private final ExecutorService fixedPoolResponse;

    public ConnectionHandler(RequestHandler requestHandler, Console console, ServerSocket serverSocket) {
        this.requestHandler = requestHandler;
        this.console = console;
        this.serverSocket = serverSocket;
        fixedPoolResponse = Executors.newFixedThreadPool(POOL_SIZE);
    }

    @Override
    public void run() {
        while (running) {
            handleClient(serverSocket);
        }
    }

    private void handleClient(ServerSocket serverSocket) {
        while (!serverSocket.isClosed()) {
            try(Socket socket = serverSocket.accept()) {
                handleRequests(socket);
            } catch (IOException e) {
                console.printError("Ошибка подключения");
            }
        }
    }

    private void handleRequests(Socket socket) {
        while (!socket.isClosed()) {
            try {
                ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
                Request request = (Request) reader.readObject();
                var future = new FutureTask<>(requestHandler.handleAsync(request));
                new Thread(future).start();
                fixedPoolResponse.submit(() -> {
                    while (!future.isDone()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            future.cancel(true);
                            return;
                        }
                    }
                    try {
                        var response = future.get();
                        writer.writeObject(response);
                        writer.flush();
                    } catch (InterruptedException | ExecutionException e) {
                        console.printError("Ошибка при выполнении запроса");
                    } catch (IOException ignored) {}
                });
            } catch (EOFException ignored) {}
            catch (IOException e) {
                break;
            } catch (ClassNotFoundException e) {
                console.printError("Ошибка при чтении запроса");
            }
        }
    }

    public void stop() {
        running = false;
    }
}
