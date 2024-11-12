package com.erikxavi.barretina;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.json.JSONArray;
import org.json.JSONObject;

public class Server extends WebSocketServer {

    private Map<WebSocket, String> clients = new ConcurrentHashMap<>();
    private static Map<String, JSONObject> selectableObjects = new ConcurrentHashMap<>();

    public Server(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("WebSocket client connected.");
        clients.put(conn, "Client_" + conn.getRemoteSocketAddress().toString());
        sendClientsList();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String clientName = clients.get(conn);
        clients.remove(conn);
        System.out.println("WebSocket client disconnected: " + clientName);
        sendClientsList();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject msgObj = new JSONObject(message);
        String type = msgObj.optString("type");

        switch (type) {
            case "ping":
                handlePing(conn);
                break;
            case "products":
                handleProducts(conn);
                break;
            case "tags":
                handleTags(conn);
                break;
            case "bounce":
                handleBounce(conn, msgObj.optString("message"));
                break;
            default:
                System.out.println("Unrecognized message type: " + type);
                break;
        }
    }

    private void handlePing(WebSocket conn) {
        JSONObject response = new JSONObject();
        response.put("type", "pong");
        response.put("message", "pong");
        conn.send(response.toString());
    }

    private void handleProducts(WebSocket conn) {
        JSONObject response = new JSONObject();
        response.put("type", "products");
        response.put("message", "Lista de productos"); // Aquí puedes personalizar la lista de productos
        conn.send(response.toString());
    }

    private void handleTags(WebSocket conn) {
        JSONObject response = new JSONObject();
        response.put("type", "tags");
        response.put("message", "Lista de tags"); // Aquí puedes personalizar la lista de tags
        conn.send(response.toString());
    }

    private void handleBounce(WebSocket conn, String message) {
        JSONObject response = new JSONObject();
        response.put("type", "bounce");
        response.put("message", message);
        broadcastMessage(response.toString(), conn);
    }

    private void broadcastMessage(String message, WebSocket sender) {
        for (Map.Entry<WebSocket, String> entry : clients.entrySet()) {
            WebSocket conn = entry.getKey();
            if (conn != sender) {
                try {
                    conn.send(message);
                } catch (WebsocketNotConnectedException e) {
                    System.out.println("Client " + entry.getValue() + " not connected.");
                    clients.remove(conn);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendClientsList() {
        JSONArray clientList = new JSONArray();
        for (String clientName : clients.values()) {
            clientList.put(clientName);
        }

        Iterator<Map.Entry<WebSocket, String>> iterator = clients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WebSocket, String> entry = iterator.next();
            WebSocket conn = entry.getKey();
            String clientName = entry.getValue();

            JSONObject response = new JSONObject();
            response.put("type", "clients");
            response.put("id", clientName);
            response.put("list", clientList);

            try {
                conn.send(response.toString());
            } catch (WebsocketNotConnectedException e) {
                System.out.println("Client " + clientName + " not connected.");
                iterator.remove();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendServerSelectableObjects() {
        JSONObject response = new JSONObject();
        response.put("type", "serverSelectableObjects");
        response.put("selectableObjects", selectableObjects);

        broadcastMessage(response.toString(), null);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
        setConnectionLostTimeout(100);
    }

    public static String askSystemName() {
        StringBuilder result = new StringBuilder();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("uname", "-r");
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return "Error: Process exited with code " + exitCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
        return result.toString().trim();
    }

    public static void main(String[] args) {
        Server server = new Server(new InetSocketAddress(3000));
        server.start();

        LineReader reader = LineReaderBuilder.builder().build();
        System.out.println("Server running. Type 'exit' to stop it.");

        try {
            while (true) {
                String line;
                try {
                    line = reader.readLine("> ");
                } catch (UserInterruptException | EndOfFileException e) {
                    break;
                }

                if ("exit".equalsIgnoreCase(line.trim())) {
                    System.out.println("Stopping server...");
                    server.stop(1000);
                    break;
                } else {
                    System.out.println("Unknown command. Type 'exit' to stop server gracefully.");
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Server stopped.");
        }
    }
}
