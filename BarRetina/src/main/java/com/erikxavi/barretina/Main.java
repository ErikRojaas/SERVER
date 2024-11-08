package com.erikxavi.barretina;

public class Main {
    public static void main(String[] args) {
        // URL del servidor
        String serverUrl = "wss://barretina5.ieti.site:443"; 

        // Crear el cliente WebSocket y conectar
        Server server = new Server(serverUrl);

        try {
            // Conectar sincrónicamente
            server.connectBlocking(); // Este bloquea hasta que la conexión se establece
            System.out.println("Conectado al servidor en: " + serverUrl);

            // El mensaje se envía cuando se abra la conexión (en el método onOpen)
            // No necesitamos enviar nada aquí ya que el envío se maneja en onOpen()

        } catch (InterruptedException e) {
            System.err.println("Error al conectar al servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
