package com.erikxavi.barretina;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ClientPruebas {

    public static void connectToServer() {
        // Asegúrate de que el servidor WebSocket esté corriendo en este URI
        String serverUri = "ws://barretina5.ieti.site:3000";

        // Crear el cliente HttpClient
        HttpClient client = HttpClient.newHttpClient();

        // Crear el WebSocket Builder
        WebSocket.Builder webSocketBuilder = client.newWebSocketBuilder();

        // Establecer el WebSocket de forma asíncrona
        CompletableFuture<WebSocket> webSocketFuture = webSocketBuilder
                .buildAsync(URI.create(serverUri), new WebSocket.Listener() {

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("Conexión exitosa al servidor WebSocket!");
                        // Enviar mensaje al servidor
                        webSocket.sendText("Prueba de conexión", true);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        // Recibir el mensaje del servidor
                        System.out.println("Mensaje recibido: " + data);
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        // Manejar errores de conexión
                        System.err.println("Error en la conexión WebSocket: " + error.getMessage());
                        error.printStackTrace();  // Imprimir más detalles del error
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        // Cuando la conexión se cierre
                        System.out.println("Conexión cerrada con código: " + statusCode + ", razón: " + reason);
                        return CompletableFuture.completedStage(null);
                    }
                }).toCompletableFuture();

        // Esperar a que la conexión sea establecida o se cierre
        try {
            webSocketFuture.get();  // Este bloque espera la conexión o cualquier error
        } catch (Exception e) {
            // Captura las excepciones si no se pudo conectar
            System.err.println("Error al conectar con el servidor WebSocket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Llamamos al método que establece la conexión
        connectToServer();
    }
}
