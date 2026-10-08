package com.smarthome.iot.service;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MqttService {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    private MqttClient mqttClient;

    @Value("${mqtt.topic.command:smarthome/device/%d/command}")
    private String commandTopicTemplate;

    @Autowired(required = false)
    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    /**
     * Gửi lệnh ON/OFF xuống gateway. Không retained: lệnh cũ không được phát lại
     * mỗi khi gateway kết nối lại (gateway tự publish trạng thái thật khi online).
     *
     * @return true nếu đã publish lên broker
     */
    public boolean publishCommand(Long deviceId, String command) {
        try {
            String topic = String.format(commandTopicTemplate, deviceId);
            return publish(topic, command, 1, false);
        } catch (MqttException e) {
            log.error("Publish command failed for device {}: {}", deviceId, e.getMessage());
            return false;
        }
    }

    public String getCommandTopicTemplate() {
        return commandTopicTemplate;
    }

    public boolean publish(String topic, String payload, int qos, boolean retained) throws MqttException {
        if (!isConnected()) {
            log.warn("MQTT client not connected - skip publish to {}", topic);
            return false;
        }

        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(qos);
        message.setRetained(retained);
        mqttClient.publish(topic, message);
        log.info("-> topic: {} | payload: {}", topic, payload);
        return true;
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }
}
