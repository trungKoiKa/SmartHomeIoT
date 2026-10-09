package com.smarthome.iot.config;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.smarthome.iot.service.MqttMessageHandler;

@Configuration
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${mqtt.broker.url:}")
    private String brokerUrl;

    @Value("${mqtt.client.id:}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Value("${mqtt.topic.status:smarthome/device/+/status}")
    private String statusTopic;

    @Value("${mqtt.topic.sensor.data:smarthome/sensor/+/data}")
    private String sensorDataTopic;

    @Value("${mqtt.topic.sensor.alert:smarthome/sensor/+/alert}")
    private String sensorAlertTopic;

    @Bean
    public MqttClient mqttClient(MqttMessageHandler handler) {
        if (brokerUrl == null || brokerUrl.isBlank() || clientId == null || clientId.isBlank()) {
            log.warn("Missing mqtt.broker.url or mqtt.client.id - app continues without MQTT client");
            return null;
        }

        try {
            MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(60);
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password != null ? password.toCharArray() : new char[0]);
            }

            Set<String> subscriptions = new LinkedHashSet<>();
            subscriptions.add(statusTopic);
            subscriptions.add(sensorDataTopic);
            subscriptions.add(sensorAlertTopic);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    // cleanSession=true: subscription mất khi reconnect nên phải đăng ký lại mỗi lần kết nối
                    for (String topic : subscriptions) {
                        if (topic != null && !topic.isBlank()) {
                            try {
                                client.subscribe(topic, 1);
                                log.info("Subscribe -> {}", topic);
                            } catch (MqttException e) {
                                log.error("Subscribe {} failed - {}", topic, e.getMessage());
                            }
                        }
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Connection lost - {}", cause != null ? cause.getMessage() : "unknown");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
                    log.debug("<- topic: {} | payload: {}", topic, payload);

                    try {
                        handler.handle(topic, payload);
                    } catch (Exception e) {
                        log.error("Handle message error on {}", topic, e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // no-op
                }
            });

            client.connect(options);
            log.info("Connected to broker {}", brokerUrl);

            return client;

        } catch (MqttException e) {
            log.warn("Cannot connect broker {} - {} (app continues without MQTT client)", brokerUrl, e.getMessage());
            return null;
        }
    }
}
