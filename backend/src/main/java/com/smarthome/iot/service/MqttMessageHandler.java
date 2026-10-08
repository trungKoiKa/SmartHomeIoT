package com.smarthome.iot.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.iot.domain.SensorData;

/**
 * Parse message MQTT nhận từ gateway rồi chuyển cho service tương ứng.
 * Service được lấy lười qua ObjectProvider để tránh vòng phụ thuộc
 * MqttClient -> DeviceService -> MqttService -> MqttClient.
 */
@Component
public class MqttMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ObjectProvider<DeviceService> deviceService;
    private final ObjectProvider<SensorDataService> sensorDataService;

    public MqttMessageHandler(ObjectProvider<DeviceService> deviceService,
            ObjectProvider<SensorDataService> sensorDataService) {
        this.deviceService = deviceService;
        this.sensorDataService = sensorDataService;
    }

    public void handle(String topic, String payload) {
        String[] parts = topic.split("/");
        if (parts.length < 4 || !"smarthome".equals(parts[0])) {
            log.warn("Unsupported topic format: {}", topic);
            return;
        }

        if ("device".equals(parts[1])) {
            handleDeviceTopic(parts, payload, topic);
        } else if ("sensor".equals(parts[1])) {
            handleSensorTopic(parts, payload, topic);
        } else {
            log.warn("Unhandled topic: {}", topic);
        }
    }

    private void handleDeviceTopic(String[] parts, String payload, String fullTopic) {
        Long deviceId = parseId(parts[2], "device");
        if (deviceId == null) {
            return;
        }

        if ("status".equals(parts[3])) {
            this.deviceService.getObject().updateStatus(deviceId, payload);
            return;
        }

        log.warn("Unhandled device topic: {}", fullTopic);
    }

    private void handleSensorTopic(String[] parts, String payload, String fullTopic) {
        Long sensorId = parseId(parts[2], "sensor");
        if (sensorId == null) {
            return;
        }

        SensorDataService service = this.sensorDataService.getObject();

        if ("data".equals(parts[3])) {
            parseSensorPayload(sensorId, payload, service);
        } else if ("alert".equals(parts[3])) {
            parseAlertPayload(sensorId, payload, service);
        } else {
            log.warn("Unhandled sensor topic: {}", fullTopic);
        }
    }

    private void parseAlertPayload(Long sensorId, String payload, SensorDataService service) {
        try {
            boolean isAlert = true;
            String message = "Cảnh báo vượt ngưỡng";
            Double value = null;

            if (payload.startsWith("{")) {
                Map<String, Object> data = OBJECT_MAPPER.readValue(payload, new TypeReference<Map<String, Object>>() {
                });
                Object msgObj = data.get("message");
                if (msgObj != null) {
                    message = msgObj.toString();
                }
                Object valueObj = data.get("value");
                if (valueObj != null) {
                    value = parseDouble(valueObj.toString());
                    if (value != null && value == 0.0) {
                        isAlert = false;
                        message = "An toàn";
                    }
                }
            } else {
                value = parseDouble(payload);
                if (value != null) {
                    if (value == 0.0) {
                        isAlert = false;
                        message = "Bình thường (Đã an toàn)";
                    } else if (value == 1.0) {
                        message = "Cảnh báo: Phát hiện sự cố!";
                    }
                }
            }

            SensorData saved = service.saveAlert(sensorId, isAlert, message, value);
            if (saved != null) {
                log.info("Alert saved - sensor={} isAlert={} message={} value={}", sensorId, isAlert, message, value);
            } else {
                log.warn("Alert save failed - sensor {} not found", sensorId);
            }
        } catch (Exception ex) {
            log.warn("Alert parse error sensor={} - {}", sensorId, ex.getMessage());
        }
    }

    private void parseSensorPayload(Long sensorId, String payload, SensorDataService service) {
        try {
            Double value;
            if (payload.startsWith("{")) {
                Map<String, Object> data = OBJECT_MAPPER.readValue(payload, new TypeReference<Map<String, Object>>() {
                });
                Object valueObj = data.get("value");
                if (valueObj == null) {
                    log.warn("Sensor {} missing value field", sensorId);
                    return;
                }
                value = parseDouble(valueObj.toString());
            } else {
                value = parseDouble(payload);
            }

            if (value == null) {
                log.warn("Sensor {} invalid value payload: {}", sensorId, payload);
                return;
            }

            if (service.saveData(sensorId, value) == null) {
                log.warn("Sensor {} not found, data dropped", sensorId);
                return;
            }
            log.info("Sensor {} -> value={}", sensorId, value);
        } catch (Exception ex) {
            log.warn("Sensor {} parse payload error: {}", sensorId, ex.getMessage());
        }
    }

    private Long parseId(String text, String type) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            log.warn("Invalid {} id in topic: {}", type, text);
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
