package com.erikxavi.server;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.json.JSONObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;

public class Server extends WebSocketServer {

    private Map<WebSocket, String> clients;
    private static ArrayList<Element> productos;
    private static ArrayList<String> tags = new ArrayList<>(Arrays.asList("Bebida", "Entrantes", "Burgers", "Smokehouse", "Grill", "Postres"));

    public Server(InetSocketAddress address) {
        super(address);
        clients = new ConcurrentHashMap<>();
        productos = new ArrayList<>();
    }

    public static void readXml(File xml) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xml);
            doc.getDocumentElement().normalize();
    
            NodeList arrayProducts = doc.getElementsByTagName("producto");
            System.out.println("Productos: " + arrayProducts.getLength());
    
            for (int cnt = 0; cnt < arrayProducts.getLength(); cnt++) {
                Node nodeProduct = arrayProducts.item(cnt);
                if (nodeProduct.getNodeType() == Node.ELEMENT_NODE) {
                    Element elm = (Element) nodeProduct;
    
                    // Extraemos el id y la categoría del producto
                    String id = elm.getAttribute("id");
                    String categoria = elm.getAttribute("categoria");
    
                    // Extraemos los subelementos nombre, descripcion, precio y foto
                    String nombre = obtenerValorElemento(elm, "nombre");
                    String descripcion = obtenerValorElemento(elm, "descripcion");
                    String precio = obtenerValorElemento(elm, "precio");
                    String foto = obtenerValorElemento(elm, "foto");
    
                    // Imprimimos los valores de cada producto para verificar
                    System.out.println("Producto ID: " + id);
                    System.out.println("  Nombre: " + nombre);
                    System.out.println("  Descripción: " + descripcion);
                    System.out.println("  Precio: " + precio);
                    System.out.println("  Foto: " + foto);
                    System.out.println("  Categoría: " + categoria);
    
                    // Añadir el elemento `elm` a la lista `productos`
                    productos.add(elm);
                }
            }
    
            System.out.println("Productos cargados: " + productos.size());
    
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("WebSocket client connected: " + conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("WebSocket client disconnected: " + conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject obj = new JSONObject(message);

        if (obj.has("type")) {
            String type = obj.getString("type");

            switch (type) {
                case "ping":
                    JSONObject msg = new JSONObject();
                    msg.put("message", "ping recieved");
                    msg.put("type", "ping");
                    conn.send(msg.toString());
                    break;
                case "bounce":
                    JSONObject msg1 = new JSONObject();
                    String line = obj.getString("message");
                    msg1.put("message", line);
                    msg1.put("type", "bounce");
                    conn.send(msg1.toString());
                    break;
                case "products":
                    JSONObject msg2 = new JSONObject();
                    String productos = printProducts();
                    msg2.put("message", productos);
                    msg2.put("type", "products");
                    conn.send(msg2.toString());
                    break;
                case "tags":
                    JSONObject msg3 = new JSONObject();
                    String categorias = printTags();
                    msg3.put("message", categorias);
                    msg3.put("type", "tags");
                    conn.send(msg3.toString());
                    break;
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
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

    private static String printProducts() {
        StringBuilder output = new StringBuilder();
    
        for (Element producto : productos) {
            String id = producto.getAttribute("id");
            String categoria = producto.getAttribute("categoria");
    
            // Verificamos y obtenemos los elementos internos de cada producto
            String nombre = obtenerValorElemento(producto, "nombre");
            String descripcion = obtenerValorElemento(producto, "descripcion");
            String precio = obtenerValorElemento(producto, "precio");
            String foto = obtenerValorElemento(producto, "foto");
    
            // Construimos la representación en texto de cada producto
            output.append("Product ID: ").append(id)
                  .append(" | Nombre: ").append(nombre)
                  .append(" | Descripcion: ").append(descripcion)
                  .append(" | Precio: ").append(precio)
                  .append(" | Foto: ").append(foto)
                  .append(" | Categoria: [").append(categoria).append("]\n");
        }
    
        return output.toString();
    }
    
    // Método auxiliar para obtener el valor de un subelemento de un producto
    private static String obtenerValorElemento(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return ""; // Retornamos cadena vacía si no existe el subelemento
    }

    private static String printTags() {
        JSONObject msg = new JSONObject();
        msg.put("type", "tags");
        msg.put("message", tags); // El array de tags se envía como JSON array
        return msg.toString();
    }
    

    public static String askSystemName() {
        StringBuilder resultat = new StringBuilder();
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            ProcessBuilder processBuilder;
            if (osName.contains("win")) {
                processBuilder = new ProcessBuilder("cmd.exe", "/c", "ver");
            } else {
                processBuilder = new ProcessBuilder("uname", "-r");
            }
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                resultat.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return "Error: El proceso ha finalizado con código " + exitCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
        return resultat.toString().trim();
    }

    public static void main(String[] args) {
        String systemName = askSystemName();

        Server server = new Server(new InetSocketAddress(3000));
        server.start();

        String userDir = System.getProperty("user.dir");
        File xml = new File(userDir, "PRODUCTES.XML");

        readXml(xml);

        Console console = System.console();
        if (console != null) {
            LineReader reader = LineReaderBuilder.builder().build();
            System.out.println("Server running. Type 'exit' to gracefully stop it.");

            try {
                while (true) {
                    String line = null;
                    try {
                        line = reader.readLine("> ");
                    } catch (UserInterruptException e) {
                        continue;
                    } catch (EndOfFileException e) {
                        break;
                    }

                    line = line.trim();

                    if (line.equalsIgnoreCase("exit")) {
                        System.out.println("Stopping server...");
                        try {
                            server.stop(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    } else {
                        System.out.println("Unknown command. Type 'exit' to stop server gracefully.");
                    }
                }
            } finally {
                System.out.println("Server stopped.");
                System.exit(0);
            }
        } else {
            System.out.println("Server running in non-interactive mode.");
        }
    }
}
