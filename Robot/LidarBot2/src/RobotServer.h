#ifndef __ROBOTSERVER_H
#define __ROBOTSERVER_H

#include <WiFi.h>
#include <WebSocketsServer.h>
#include <functional>  // Per usare std::function

class RobotServer {
    public:
        RobotServer(String ssid, String password);
        bool init();
        void handleClient();

        // Definisci un tipo di callback
        using MessageCallback = std::function<void(const String&)>;

        // Imposta il callback da chiamare quando viene ricevuto un messaggio
        void setMessageCallback(MessageCallback callback);
    private:
        String ssid;  // Sostituisci con il nome della tua rete Wi-Fi
        String password;  // Sostituisci con la password della tua rete Wi-Fi

        WebSocketsServer webSocket = WebSocketsServer(81);

        // Variabile per memorizzare il callback
        MessageCallback messageCallback;

        void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length);
};

#endif