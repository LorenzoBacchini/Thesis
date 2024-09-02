package artificial_vision_tracking;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class ESP32Client extends WebSocketClient {

    public ESP32Client(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connesso al WebSocket Server");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Ricevuto dal server: " + message);
        // Gestione del messaggio ricevuto
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connessione chiusa: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
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
