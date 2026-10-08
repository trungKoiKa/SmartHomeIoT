#include "mqtt_bridge.h"

#include <strings.h>
#include <WiFi.h>
#include <PubSubClient.h>

#include "lora_receiver.h"

#if __has_include("secrets.h")
#include "secrets.h"
#else
#error "Thieu secrets.h: copy secrets.example.h thanh secrets.h va dien WiFi/MQTT"
#endif

// ID co dinh phai khop voi ban ghi sensors/devices trong DB cua backend.
#define SENSOR_ID_LIGHT 1
#define SENSOR_ID_TEMP 2
#define SENSOR_ID_GAS 3
#define SENSOR_ID_HUMI 4
#define DEVICE_ID_RELAY1 1
#define DEVICE_ID_RELAY2 2

// Topic (khop README / application.properties cua backend)
#define TOPIC_SENSOR_DATA "smarthome/sensor/%d/data"
#define TOPIC_SENSOR_ALERT "smarthome/sensor/%d/alert"
#define TOPIC_DEVICE_STATUS "smarthome/device/%d/status"
#define TOPIC_DEVICE_COMMAND_FILTER "smarthome/device/+/command"
#define TOPIC_GATEWAY_STATE "smarthome/gateway/state"
#define TOPIC_GATEWAY_AVAILABILITY "smarthome/gateway/availability"

static const unsigned long SENDDATA_INTERVAL_MS = 10000;
static const unsigned long NODE_RESPONSE_TIMEOUT_MS = 2000;

static WiFiClient wifiClient;
static PubSubClient mqtt(wifiClient);

static SensorData_t txShadow = {0.0f, 0.0f, 0, 0, 35, 60, 0, 0, false};
static unsigned long lastSendDataMs = 0;
static unsigned long lastWiFiRetry = 0;
static unsigned long lastMqttRetry = 0;
static bool waitingNode = false;
static bool hasPendingCmd = false;       // co thay doi tu MQTT cho gui xuong node
static bool confirmRelayPending = false; // vua gui lenh, cho frame cua node de bao status that
static bool hasNodeData = false;         // da nhan it nhat 1 frame that tu node
static bool needFullPublish = false;     // vua (re)connect MQTT, can day lai toan bo

// ---------------------------------------------------------------- publish helpers

static void publishValue(const char *fmt, int id, float value)
{
  char topic[48];
  char payload[32];
  snprintf(topic, sizeof(topic), fmt, id);
  snprintf(payload, sizeof(payload), "{\"value\":%.2f}", value);
  mqtt.publish(topic, payload);
  Serial.printf("[MQTT] %s <- %s\n", topic, payload);
}

static void publishAlert(uint8_t alert)
{
  char topic[48];
  snprintf(topic, sizeof(topic), TOPIC_SENSOR_ALERT, SENSOR_ID_GAS);
  mqtt.publish(topic, alert ? "1" : "0");
  Serial.printf("[MQTT] %s <- %d\n", topic, alert ? 1 : 0);
}

static void publishRelayStatus(int deviceId, bool on)
{
  char topic[48];
  snprintf(topic, sizeof(topic), TOPIC_DEVICE_STATUS, deviceId);
  mqtt.publish(topic, on ? "ON" : "OFF");
  Serial.printf("[MQTT] %s <- %s\n", topic, on ? "ON" : "OFF");
}

static void publishRelays(const SensorData_t &d)
{
  publishRelayStatus(DEVICE_ID_RELAY1, (d.relayStatus & 0x01) != 0);
  publishRelayStatus(DEVICE_ID_RELAY2, (d.relayStatus & 0x02) != 0);
}

// Trang thai phu (che do, nguong) - backend khong dung, phuc vu debug bang mosquitto_sub.
static void publishGatewayState()
{
  char payload[96];
  snprintf(payload, sizeof(payload),
           "{\"autoMode\":%d,\"tempThreshold\":%u,\"gasThreshold\":%u}",
           txShadow.autoMode ? 1 : 0, txShadow.tempThreshold, txShadow.gasThreshold);
  mqtt.publish(TOPIC_GATEWAY_STATE, payload);
}

static void publishAllState(const SensorData_t &d)
{
  publishValue(TOPIC_SENSOR_DATA, SENSOR_ID_TEMP, d.temperature);
  publishValue(TOPIC_SENSOR_DATA, SENSOR_ID_HUMI, d.humidity);
  publishValue(TOPIC_SENSOR_DATA, SENSOR_ID_GAS, d.gasValue);
  publishValue(TOPIC_SENSOR_DATA, SENSOR_ID_LIGHT, d.lightValue);
  publishAlert(d.alertStatus);
  publishRelays(d);
  publishGatewayState();
}

static void publishChangedState(const SensorData_t &o, const SensorData_t &n)
{
  if (o.temperature != n.temperature)
    publishValue(TOPIC_SENSOR_DATA, SENSOR_ID_TEMP, n.temperature);
  if (o.humidity != n.humidity)
    publishValue(TOPIC_SENSOR_DATA, SENSOR_ID_HUMI, n.humidity);
  if (o.gasValue != n.gasValue)
    publishValue(TOPIC_SENSOR_DATA, SENSOR_ID_GAS, n.gasValue);
  if (o.lightValue != n.lightValue)
    publishValue(TOPIC_SENSOR_DATA, SENSOR_ID_LIGHT, n.lightValue);
  if (o.alertStatus != n.alertStatus)
    publishAlert(n.alertStatus);
  if ((o.relayStatus & 0x01) != (n.relayStatus & 0x01))
    publishRelayStatus(DEVICE_ID_RELAY1, (n.relayStatus & 0x01) != 0);
  if ((o.relayStatus & 0x02) != (n.relayStatus & 0x02))
    publishRelayStatus(DEVICE_ID_RELAY2, (n.relayStatus & 0x02) != 0);
  if (o.autoMode != n.autoMode || o.tempThreshold != n.tempThreshold || o.gasThreshold != n.gasThreshold)
    publishGatewayState();
}

// ---------------------------------------------------------------- node link

static bool sendShadowToNode()
{
  bool ok = sendLoraSensorData(txShadow);
  Serial.println(ok ? "Gui frame LoRa OK" : "Gui frame LoRa FAIL");
  return ok;
}

static void requestNodeSendData()
{
  bool ok = sendLoraTextCommand("senddata");
  lastSendDataMs = millis();
  if (ok)
  {
    waitingNode = true;
  }
  Serial.println(ok ? "Da gui lenh senddata xuong node" : "Gui lenh senddata FAIL");
}

static void checkWaitingNodeTimeout()
{
  if (waitingNode && (millis() - lastSendDataMs >= NODE_RESPONSE_TIMEOUT_MS))
  {
    waitingNode = false;
    Serial.println("Qua 2s khong co du lieu node -> waitingNode = false");
  }
}

static void handlePendingCmd()
{
  if (waitingNode || !hasPendingCmd)
    return;

  hasPendingCmd = false;
  if (sendShadowToNode())
  {
    Serial.println("Da gui lenh MQTT xuong node");
    // Node chi tra frame khi nhan "senddata": hoi ngay de xac nhan relay thay vi cho chu ky 10s.
    requestNodeSendData();
  }
  else
  {
    // Gui that bai: khong con gi de cho node xac nhan.
    confirmRelayPending = false;
  }
}

// ---------------------------------------------------------------- MQTT command

static void setRelayBit(uint8_t mask, bool on)
{
  if (txShadow.autoMode)
  {
    // Lenh tu web/giong noi la chu dich cua nguoi dung -> chuyen sang che do tay.
    txShadow.autoMode = false;
    Serial.println("Lenh relay khi dang AUTO -> chuyen sang MANUAL");
  }

  if (on)
    txShadow.relayStatus |= mask;
  else
    txShadow.relayStatus &= (uint8_t)(~mask);

  hasPendingCmd = true;
  confirmRelayPending = true;
}

static void onMqttMessage(char *topic, byte *payload, unsigned int length)
{
  int deviceId = 0;
  char tail[16] = {0};
  if (sscanf(topic, "smarthome/device/%d/%15s", &deviceId, tail) != 2 || strcmp(tail, "command") != 0)
    return;

  char msg[8];
  unsigned int n = length < sizeof(msg) - 1 ? length : sizeof(msg) - 1;
  memcpy(msg, payload, n);
  msg[n] = '\0';

  bool on;
  if (strcasecmp(msg, "ON") == 0)
    on = true;
  else if (strcasecmp(msg, "OFF") == 0)
    on = false;
  else
  {
    Serial.printf("Bo qua command khong hop le: %s\n", msg);
    return;
  }

  Serial.printf("[MQTT] command device %d -> %s\n", deviceId, msg);
  if (deviceId == DEVICE_ID_RELAY1)
    setRelayBit(0x01, on);
  else if (deviceId == DEVICE_ID_RELAY2)
    setRelayBit(0x02, on);
  else
    Serial.printf("Bo qua command device %d (khong co relay)\n", deviceId);
}

// ---------------------------------------------------------------- connection tasks

static void connectWiFiTask()
{
  if (WiFi.status() == WL_CONNECTED)
    return;

  unsigned long now = millis();
  if (now - lastWiFiRetry < 5000)
    return;
  lastWiFiRetry = now;

  Serial.print("WiFi reconnecting to ");
  Serial.println(WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}

static void connectMqttTask()
{
  if (WiFi.status() != WL_CONNECTED || mqtt.connected())
    return;

  unsigned long now = millis();
  if (now - lastMqttRetry < 3000)
    return;
  lastMqttRetry = now;

  Serial.println("MQTT reconnecting...");
  // LWT: broker tu dat "offline" neu gateway rot ket noi dot ngot.
  bool ok = mqtt.connect(MQTT_CLIENT_ID, MQTT_USER, MQTT_PASS,
                         TOPIC_GATEWAY_AVAILABILITY, 1, true, "offline");
  if (!ok)
  {
    Serial.printf("MQTT connect FAIL, rc=%d\n", mqtt.state());
    return;
  }

  Serial.println("MQTT connected");
  mqtt.publish(TOPIC_GATEWAY_AVAILABILITY, "online", true);
  mqtt.subscribe(TOPIC_DEVICE_COMMAND_FILTER, 0);
  needFullPublish = true;
}

// ---------------------------------------------------------------- public API

void initMqttBridge()
{
  WiFi.mode(WIFI_STA);
  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  mqtt.setCallback(onMqttMessage);
  mqtt.setSocketTimeout(2);

  connectWiFiTask();
  connectMqttTask();
}

void mqttBridgeRequestNodeDataByInterval()
{
  if (millis() - lastSendDataMs < SENDDATA_INTERVAL_MS)
    return;

  requestNodeSendData();
}

void mqttBridgeLoop()
{
  connectWiFiTask();
  connectMqttTask();

  if (mqtt.connected())
  {
    mqtt.loop();

    if (needFullPublish && hasNodeData)
    {
      publishAllState(txShadow);
      needFullPublish = false;
    }
  }

  checkWaitingNodeTimeout();
  handlePendingCmd();
  mqttBridgeRequestNodeDataByInterval();
}

void mqttBridgePublishChangedFromLora(const SensorData_t &oldData, const SensorData_t &newData)
{
  waitingNode = false;

  // Lenh dang cho gui xuong node khong duoc bi frame cu cua node ghi de.
  uint8_t pendingRelay = txShadow.relayStatus;
  uint8_t pendingAuto = txShadow.autoMode;
  txShadow = newData;
  if (hasPendingCmd)
  {
    txShadow.relayStatus = pendingRelay;
    txShadow.autoMode = pendingAuto;
  }

  if (!mqtt.connected())
  {
    // Chua co MQTT: nho de day toan bo khi ket noi lai.
    hasNodeData = true;
    needFullPublish = true;
    return;
  }

  if (!hasNodeData)
  {
    // Frame that dau tien: so voi gia tri khoi tao vo nghia nen day toan bo.
    hasNodeData = true;
    publishAllState(newData);
    needFullPublish = false;
    confirmRelayPending = false;
    return;
  }

  publishChangedState(oldData, newData);

  if (confirmRelayPending)
  {
    // Vua dieu khien relay: luon bao trang thai that do node xac nhan de DB tu sua neu lenh that bai.
    confirmRelayPending = false;
    publishRelays(newData);
  }
}
