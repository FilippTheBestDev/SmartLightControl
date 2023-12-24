#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <SoftwareSerial.h>

SoftwareSerial USART_to_ATmega8A(3, 1); // RX, TX pins ESP8266
const char *ssid = "назва_мережі";
const char *password = "пароль_мережі";
IPAddress staticIP(192, 168, 1, 111);  // Cтатичний IP веб-серверу
IPAddress gateway(192, 168, 1, 1);
IPAddress subnet(255, 255, 255, 0);

ESP8266WebServer server(80);

bool lights_current[8] = { 0, 0, 0, 0, 0, 0, 0, 0 };

void setup() {
  Serial.begin(115200);
  USART_to_ATmega8A.begin(9600); // взаємодія з ATmega8A

  // Підключення до WiFi зі статичним IP
  WiFi.begin(ssid, password);
  WiFi.config(staticIP, gateway, subnet);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }

  Serial.println("Connected to WiFi");

  // Визначення маршрутів для API
  server.on("/turn-on", HTTP_POST, handleTurnOn);
  server.on("/turn-off", HTTP_POST, handleTurnOff);

  // Запуск веб-сервера
  server.begin();
}

void loop() {
  // Обробка клієнтських запитів
  server.handleClient();
}

void handleTurnOn() {
  int portNumber = -1;
  if(server.hasArg("plain")) {
    // Отримуємо POST дані
    String data = server.arg("plain");
    Serial.println("Received data: " + data);
    portNumber = data.toInt();
  }

  if(portNumber == -1)
    USART_to_ATmega8A.println("ONX");
  else {
    USART_to_ATmega8A.print("ON");
    USART_to_ATmega8A.print(portNumber);
    USART_to_ATmega8A.print("\n");
  }

  server.send(200, "application/json", "{\"status\":\"success\", \"message\":\"Light turned on\"}");
}

void handleTurnOff() {
  int portNumber = -1;
  if(server.hasArg("plain")) {
    // Отримуємо POST дані
    String data = server.arg("plain");
    Serial.println("Received data: " + data);
    portNumber = data.toInt();
  }
  
  if(portNumber == -1)
    USART_to_ATmega8A.println("OFFX");
  else {
    USART_to_ATmega8A.print("OFF");
    USART_to_ATmega8A.print(portNumber);
    USART_to_ATmega8A.print("\n");
  }

  server.send(200, "application/json", "{\"status\":\"success\", \"message\":\"Light turned off\"}");
}
