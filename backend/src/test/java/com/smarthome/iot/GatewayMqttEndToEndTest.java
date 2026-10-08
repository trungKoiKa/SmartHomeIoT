package com.smarthome.iot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.smarthome.iot.domain.Device;
import com.smarthome.iot.domain.Sensor;
import com.smarthome.iot.domain.SensorData;
import com.smarthome.iot.repository.DeviceRepository;
import com.smarthome.iot.repository.SensorDataRepository;
import com.smarthome.iot.repository.SensorRepository;

/**
 * E2E: gateway (mô phỏng bằng 1 MQTT client) <-> broker thật <-> backend (MqttConfig thật) <-> MySQL tạm.
 * Chạy: mvnw test -Dtest=GatewayMqttEndToEndTest -De2e=true  (cần biến môi trường E2E_*, xem application-e2e.properties
 * và E2E_MQTT_GATEWAY_USER / E2E_MQTT_GATEWAY_PASS).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@EnabledIfSystemProperty(named = "e2e", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayMqttEndToEndTest {

    private static final Map<String, String> commandsSeen = new ConcurrentHashMap<>();
    private static MqttClient gateway;

    @Autowired
    MockMvc mvc;
    @Autowired
    SensorRepository sensorRepository;
    @Autowired
    SensorDataRepository sensorDataRepository;
    @Autowired
    DeviceRepository deviceRepository;

    @BeforeAll
    static void connectGatewaySimulator() throws Exception {
        gateway = new MqttClient(System.getenv().getOrDefault("E2E_MQTT_URL", "tcp://localhost:1884"),
                "e2e-gateway-sim");
        MqttConnectOptions o = new MqttConnectOptions();
        o.setCleanSession(true);
        o.setUserName(System.getenv("E2E_MQTT_GATEWAY_USER"));
        o.setPassword(System.getenv("E2E_MQTT_GATEWAY_PASS").toCharArray());
        gateway.connect(o);
        gateway.subscribe("smarthome/device/+/command", 1, (topic, msg) ->
                commandsSeen.put(topic, new String(msg.getPayload(), StandardCharsets.UTF_8)));
    }

    @AfterAll
    static void disconnect() throws Exception {
        if (gateway != null && gateway.isConnected()) {
            gateway.disconnect();
            gateway.close();
        }
    }

    private static void publish(String topic, String payload) throws Exception {
        gateway.publish(topic, payload.getBytes(StandardCharsets.UTF_8), 1, false);
    }

    private static void await(String what, BooleanSupplier cond) throws Exception {
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) {
            if (cond.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timeout cho: " + what);
    }

    private void seed() {
        String[][] sensors = { { "1", "Cảm biến ánh sáng", "LIGHT" }, { "2", "Cảm biến nhiệt độ", "TEMPERATURE" },
                { "3", "Cảm biến khí gas", "GAS" }, { "4", "Cảm biến độ ẩm", "HUMIDITY" } };
        if (sensorRepository.count() == 0) {
            for (String[] s : sensors) {
                Sensor sensor = new Sensor();
                sensor.setName(s[1]);
                sensor.setType(s[2]);
                sensor.setStatus("ON");
                sensorRepository.save(sensor);
            }
        }
        if (deviceRepository.count() == 0) {
            for (String n : List.of("Quạt", "Bóng đèn")) {
                Device d = new Device();
                d.setName(n);
                d.setStatus("OFF");
                deviceRepository.save(d);
            }
        }
    }

    private List<SensorData> dataOf(long sensorId) {
        return sensorDataRepository.findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    @Test
    @Order(1)
    void seedAndGatewayPublishesSensorData() throws Exception {
        seed();
        long s2 = dataOf(2).size();
        long s4 = dataOf(4).size();

        publish("smarthome/sensor/2/data", "{\"value\":30.50}"); // dạng gateway thật gửi
        publish("smarthome/sensor/4/data", "{\"value\":61.00}");

        await("sensor 2 có dòng mới", () -> dataOf(2).size() == s2 + 1);
        await("sensor 4 có dòng mới", () -> dataOf(4).size() == s4 + 1);
        assertEquals(30.5, dataOf(2).get(0).getValue());
        assertEquals(61.0, dataOf(4).get(0).getValue());
        assertFalse(dataOf(2).get(0).isAlert());
    }

    @Test
    @Order(2)
    void gatewayAlertIsStoredAsAlertRow() throws Exception {
        publish("smarthome/sensor/3/alert", "1");
        await("alert gas = true", () -> dataOf(3).stream().anyMatch(SensorData::isAlert));
        publish("smarthome/sensor/3/alert", "0");
        await("alert gas = an toàn", () -> !dataOf(3).isEmpty() && !dataOf(3).get(0).isAlert());
    }

    @Test
    @Order(3)
    void gatewayStatusUpdatesDeviceAndInvalidIsIgnored() throws Exception {
        Long id = deviceRepository.findAll().get(0).getId();
        publish("smarthome/device/" + id + "/status", "ON");
        await("device ON", () -> "ON".equals(deviceRepository.findById(id).get().getStatus()));

        publish("smarthome/device/" + id + "/status", "garbage");
        Thread.sleep(800);
        assertEquals("ON", deviceRepository.findById(id).get().getStatus());

        publish("smarthome/device/" + id + "/status", "OFF");
        await("device OFF", () -> "OFF".equals(deviceRepository.findById(id).get().getStatus()));
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "USER")
    void voiceCommandReachesGatewayAndUpdatesDb() throws Exception {
        List<Device> devices = deviceRepository.findAll();
        Long fan = devices.get(0).getId();
        commandsSeen.clear();

        mvc.perform(post("/client/voice/command").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"bật quạt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.devices[0].status").value("ON"));

        await("gateway nhận lệnh ON", () -> "ON".equals(commandsSeen.get("smarthome/device/" + fan + "/command")));
        assertEquals("ON", deviceRepository.findById(fan).get().getStatus());
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "USER")
    void voiceTurnOffAllSendsOneCommandPerDevice() throws Exception {
        List<Device> devices = deviceRepository.findAll();
        commandsSeen.clear();

        mvc.perform(post("/client/voice/command").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Tắt tất cả\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        for (Device d : devices) {
            await("gateway nhận OFF cho " + d.getId(),
                    () -> "OFF".equals(commandsSeen.get("smarthome/device/" + d.getId() + "/command")));
            assertEquals("OFF", deviceRepository.findById(d.getId()).get().getStatus());
        }
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "USER")
    void unknownVoiceCommandSendsNothing() throws Exception {
        commandsSeen.clear();
        mvc.perform(post("/client/voice/command").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"bật máy giặt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
        Thread.sleep(800);
        assertTrue(commandsSeen.isEmpty());
    }

    @Test
    @Order(7)
    void guestIsRedirectedToLogin() throws Exception {
        mvc.perform(post("/client/voice/command").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"bật quạt\"}"))
                .andExpect(status().is3xxRedirection());
        assertNotNull(deviceRepository.findAll());
    }
}
