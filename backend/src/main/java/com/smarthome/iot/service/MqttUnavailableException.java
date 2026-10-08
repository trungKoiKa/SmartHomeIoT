package com.smarthome.iot.service;

/** Ném khi cần gửi lệnh điều khiển nhưng MQTT chưa kết nối / publish thất bại. */
public class MqttUnavailableException extends RuntimeException {

    public MqttUnavailableException(String message) {
        super(message);
    }
}
