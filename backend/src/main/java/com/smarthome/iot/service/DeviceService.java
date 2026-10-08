package com.smarthome.iot.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.smarthome.iot.domain.Device;
import com.smarthome.iot.repository.DeviceRepository;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository deviceRepository;
    private final MqttService mqttService;

    public DeviceService(DeviceRepository deviceRepository, MqttService mqttService) {
        this.deviceRepository = deviceRepository;
        this.mqttService = mqttService;
    }

    public List<Device> getAllDevice() {
        return this.deviceRepository.findAll();
    }

    public Device createDevice(Device device) {
        return this.deviceRepository.save(device);
    }

    public Device findById(Long id) {
        return this.deviceRepository.findById(id).orElse(null);
    }

    public Device handleSaveDevice(Device device) {
        return this.deviceRepository.save(device);
    }

    public void deleteADevice(Long id) {
        this.deviceRepository.deleteById(id);
    }

    public long countDevice() {
        return this.deviceRepository.count();
    }

    public long countDeviceByStatus(String status) {
        return this.deviceRepository.countByStatus(status);
    }

    public List<Device> findByRoomId(Long roomId) {
        return this.deviceRepository.findByRoomId(roomId);
    }

    public Device toggleStatus(Long id) {
        Device device = this.deviceRepository.findById(id).orElse(null);
        if (device == null) {
            return null;
        }

        return setStatus(id, "ON".equals(device.getStatus()) ? "OFF" : "ON");
    }

    /**
     * Đặt trạng thái thiết bị (idempotent) và gửi lệnh xuống gateway.
     * Chỉ ghi DB sau khi lệnh đã được publish lên broker.
     *
     * @return thiết bị đã lưu, hoặc null nếu không tìm thấy
     * @throws IllegalArgumentException   nếu status không phải ON/OFF
     * @throws MqttUnavailableException   nếu không gửi được lệnh
     */
    public Device setStatus(Long id, String status) {
        String normalized = normalizeStatus(status);
        if (normalized == null) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + status);
        }

        Device device = this.deviceRepository.findById(id).orElse(null);
        if (device == null) {
            return null;
        }

        if (!this.mqttService.publishCommand(id, normalized)) {
            throw new MqttUnavailableException("Không gửi được lệnh tới thiết bị (MQTT chưa kết nối)");
        }

        device.setStatus(normalized);
        return this.deviceRepository.save(device);
    }

    /** Cập nhật trạng thái thật do gateway báo về; bỏ qua payload không phải ON/OFF. */
    public void updateStatus(Long id, String status) {
        String normalized = normalizeStatus(status);
        if (normalized == null) {
            log.warn("Ignore invalid status '{}' for device {}", status, id);
            return;
        }

        Device device = this.deviceRepository.findById(id).orElse(null);
        if (device != null) {
            device.setStatus(normalized);
            this.deviceRepository.save(device);
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        String s = status.trim().toUpperCase();
        return ("ON".equals(s) || "OFF".equals(s)) ? s : null;
    }
}
