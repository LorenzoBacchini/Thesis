#include "RobotServer.h"

RobotServer::RobotServer(String ssid, String password) {
  this->ssid = ssid;
  this->password = password;
}

bool RobotServer::init() {
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("Connesso al WiFi!");
  webSocket.begin();
  webSocket.onEvent([this](uint8_t num, WStype_t type, uint8_t *payload, size_t length) {
    this->webSocketEvent(num, type, payload, length);
  });

  // Ottieni e stampa l'indirizzo IP assegnato
  Serial.print("Indirizzo IP dell'ESP32: ");
  Serial.println(WiFi.localIP());
  return true;
}

void RobotServer::handleClient() {
  webSocket.loop();
}

void RobotServer::setMessageCallback(MessageCallback callback) {
    // Assegna il callback fornito dal codice principale
    messageCallback = callback;
}

void RobotServer::webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  String message; 
  switch(type) {
    case WStype_TEXT:
      message = String((char *)payload);
      Serial.printf("Ricevuto: %s\n", message.c_str());
      // Se è stato impostato un callback, chiamalo con il messaggio ricevuto
      if (messageCallback) {
          messageCallback(message);
      }
      break;
    case WStype_DISCONNECTED:
      Serial.printf("[%u] Disconnesso!\n", num);
      if (messageCallback) {
          Serial.printf("Chiamato il callback con STOP\n");
          String stop = "STOP";
          messageCallback(stop);
      }
      break;
    case WStype_CONNECTED:
      Serial.printf("[%u] Connesso!\n", num);
      webSocket.sendTXT(num, "Benvenuto al webSocket1 Server!");
      break;
  }
}