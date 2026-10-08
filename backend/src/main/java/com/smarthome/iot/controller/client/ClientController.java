package com.smarthome.iot.controller.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smarthome.iot.domain.Device;
import com.smarthome.iot.domain.Room;
import com.smarthome.iot.domain.Sensor;
import com.smarthome.iot.domain.SensorData;
import com.smarthome.iot.service.DeviceService;
import com.smarthome.iot.service.MqttUnavailableException;
import com.smarthome.iot.service.RoomService;
import com.smarthome.iot.service.SensorDataService;
import com.smarthome.iot.service.SensorService;
import com.smarthome.iot.service.VoiceCommandService;

@Controller
public class ClientController {

    private final RoomService roomService;
    private final SensorService sensorService;
    private final SensorDataService sensorDataService;
    private final DeviceService deviceService;
    private final VoiceCommandService voiceCommandService;

    public ClientController(RoomService roomService, SensorService sensorService,
            SensorDataService sensorDataService, DeviceService deviceService,
            VoiceCommandService voiceCommandService) {
        this.roomService = roomService;
        this.sensorService = sensorService;
        this.sensorDataService = sensorDataService;
        this.deviceService = deviceService;
        this.voiceCommandService = voiceCommandService;
    }

    @GetMapping("/client/room-list")
    public String getRoomList(Model model) {
        List<Room> rooms = this.roomService.getAllRoom();
        model.addAttribute("rooms", rooms);
        return "client/room/list";
    }

    @GetMapping("/client/room/{id}")
    public String getRoomDetail(Model model, @PathVariable Long id, Authentication authentication) {
        Room room = this.roomService.findById(id);
        model.addAttribute("room", room);

        if (room != null && room.getSensors() != null) {
            for (Sensor sensor : room.getSensors()) {
                List<SensorData> latestData = this.sensorDataService.getLatestData(sensor.getId());
                sensor.setLatestData(latestData);
            }
        }

        if (room != null) {
            List<Device> devices = this.deviceService.findByRoomId(id);
            model.addAttribute("devices", devices);
            model.addAttribute("deviceCount", devices.size());
        } else {
            model.addAttribute("deviceCount", 0);
        }

        boolean isLoggedIn = authentication != null && authentication.isAuthenticated();
        model.addAttribute("isLoggedIn", isLoggedIn);

        return "client/room/detail";
    }

    @GetMapping("/client/sensor/{id}")
    public String getSensorDetail(Model model, @PathVariable Long id) {
        Sensor sensor = this.sensorService.findById(id);
        
        // 1. Dữ liệu đo lường định kỳ
        List<SensorData> normalDataList = this.sensorDataService.getTop10NormalDataBySensor(id);
        // 2. Lịch sử cảnh báo
        List<SensorData> alertList = this.sensorDataService.getAlertsBySensor(id);

        model.addAttribute("sensor", sensor);
        model.addAttribute("dataList", normalDataList);
        model.addAttribute("dataCount", normalDataList.size());
        
        model.addAttribute("alertList", alertList);
        model.addAttribute("alertCount", alertList.size());
        
        return "client/sensor/detail";
    }

    @GetMapping("/client/sensor-list")
    public String getSensorList(Model model) {
        List<Room> rooms = this.roomService.getAllRoom();
        List<Sensor> sensors = new ArrayList<>();

        for (Room room : rooms) {
            if (room.getSensors() != null) {
                for (Sensor sensor : room.getSensors()) {
                    List<SensorData> latestData = this.sensorDataService.getLatestData(sensor.getId());
                    sensor.setLatestData(latestData);
                    sensors.add(sensor);
                }
            }
        }

        model.addAttribute("sensors", sensors);
        return "client/sensor/list";
    }

    @GetMapping("/client/device")
    public String getDeviceList(Model model, Authentication authentication) {
        List<Device> devices = this.deviceService.getAllDevice();
        List<Room> rooms = this.roomService.getAllRoom();

        boolean isLoggedIn = authentication != null && authentication.isAuthenticated();

        model.addAttribute("devices", devices);
        model.addAttribute("rooms", rooms);
        model.addAttribute("isLoggedIn", isLoggedIn);
        return "client/device/show";
    }

    @PostMapping("/client/device/{id}/toggle")
    @ResponseBody
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> toggleDevice(@PathVariable Long id) {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        try {
            Device device = this.deviceService.toggleStatus(id);
            if (device == null) {
                resp.put("success", false);
                resp.put("message", "Khong tim thay thiet bi");
                return ResponseEntity.status(404).body(resp);
            }
            resp.put("success", true);
            resp.put("id", device.getId());
            resp.put("status", device.getStatus());
            return ResponseEntity.ok(resp);
        } catch (MqttUnavailableException e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(503).body(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    public record VoiceRequest(String text) {
    }

    @PostMapping("/client/voice/command")
    @ResponseBody
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> voiceCommand(@RequestBody VoiceRequest request) {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        String text = request != null ? request.text() : null;
        if (text != null && text.length() > 200) {
            resp.put("success", false);
            resp.put("message", "Câu lệnh quá dài");
            return ResponseEntity.badRequest().body(resp);
        }

        try {
            VoiceCommandService.Result result = this.voiceCommandService.execute(text);
            resp.put("success", result.success());
            resp.put("message", result.message());
            if (result.success()) {
                resp.put("status", result.status());
                resp.put("devices", result.devices().stream()
                        .map(d -> java.util.Map.of("id", d.getId(), "status", d.getStatus()))
                        .toList());
            }
            // Không hiểu lệnh là lỗi người dùng (200 + success=false), không phải lỗi server
            return ResponseEntity.ok(resp);
        } catch (MqttUnavailableException e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(503).body(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }
}
