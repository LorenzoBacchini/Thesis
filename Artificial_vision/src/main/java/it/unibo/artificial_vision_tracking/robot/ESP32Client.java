package it.unibo.artificial_vision_tracking.robot;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.logging.Logger;

/**
 * Class to manage the WebSocket client to communicate with the ESP32.
 */
public final class ESP32Client extends WebSocketClient {
    private static final Logger LOGGER = Logger.getLogger(ESP32Client.class.getName());
    /**
     * Constructor of the class.
     * @param serverUri
     */
    public ESP32Client(final URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(final ServerHandshake handshakedata) {
        LOGGER.info("Connected to server: " + getURI());
    }

    @Override
    public void onMessage(final String message) {
        LOGGER.info("Message received from server: " + message);
        // Gestione del messaggio ricevuto
    }

    @Override
    public void onClose(final int code, final String reason, final boolean remote) {
        LOGGER.info("Connection closed: " + reason);
    }

    @Override
    public void onError(final Exception ex) {
        LOGGER.warning(ex.getMessage());
    }

    /*
    public static void main(String[] args) {
        try {
            URI serverUri = new URI("ws://10.0.0.6:81"); // Sostituisci con l'IP dell'ESP32
            ESP32Client client = new ESP32Client(serverUri);
            client.connectBlocking();
            String message;
            message = DirectionEnum.MOVE_BACKWARD.toString();
            System.out.println("Invio messaggio al server");
            client.send(message);
            System.out.println("Messaggio inviato");
            Thread.sleep(5000);

            message = DirectionEnum.TURN_LEFT.toString();
            System.out.println("Invio messaggio al server");
            client.send(message);
            System.out.println("Messaggio inviato");
            Thread.sleep(5000);

            message = DirectionEnum.TURN_RIGHT.toString();
            System.out.println("Invio messaggio al server");
            client.send(message);
            System.out.println("Messaggio inviato");
            Thread.sleep(5000);

            message = DirectionEnum.TURN_LEFT.toString();
            System.out.println("Invio messaggio al server");
            client.send(message);
            System.out.println("Messaggio inviato");
            Thread.sleep(2500);

            message = DirectionEnum.MOVE_FORWARD.toString();
            System.out.println("Invio messaggio al server");
            client.send(message);
            System.out.println("Messaggio inviato");
            Thread.sleep(5000);

            // client.send("3, 3, 3, 3");
            // Thread.sleep(10000);
            // client.send("0, 0, 0, 0");
            // client.send("2, -2, 2, -2");
            // Thread.sleep(1500);
            // client.send("0, 0, 0, 0");
            // client.send("3, 3, 3, 3");
            // Thread.sleep(10000);
            // client.send("0, 0, 0, 0");
            // client.send("2, -2, 2, -2");
            // Thread.sleep(1500);
            // client.send("0, 0, 0, 0");
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */
}
