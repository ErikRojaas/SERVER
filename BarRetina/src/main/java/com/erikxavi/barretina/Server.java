package com.erikxavi.barretina;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class Server extends WebSocketClient {

    // Constructor
    public Server(String serverUri) {
        super(createUri(serverUri));
        // Ignorar SSL (solo para pruebas)
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            } }, new java.security.SecureRandom());
            setSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Crear la URI
    private static URI createUri(String serverUri) {
        try {
            return new URI(serverUri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("URI inválida: " + serverUri, e);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Conexión abierta con el servidor.");
        // Ahora que la conexión está abierta, podemos enviar el mensaje
        send("¡Conexión establecida con éxito!");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Mensaje recibido: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Conexión cerrada. Código: " + code + " Razón: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Error: " + ex.getMessage());
        ex.printStackTrace();
    }
}
