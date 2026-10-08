package com.smarthome.iot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.smarthome.iot.domain.SensorData;

@SuppressWarnings("unchecked")
class MqttMessageHandlerTest {

    private DeviceService deviceService;
    private SensorDataService sensorDataService;
    private MqttMessageHandler handler;

    @BeforeEach
    void setUp() {
        deviceService = mock(DeviceService.class);
        sensorDataService = mock(SensorDataService.class);
        ObjectProvider<DeviceService> devices = mock(ObjectProvider.class);
        ObjectProvider<SensorDataService> sensors = mock(ObjectProvider.class);
        when(devices.getObject()).thenReturn(deviceService);
        when(sensors.getObject()).thenReturn(sensorDataService);
        handler = new MqttMessageHandler(devices, sensors);
    }

    @Test
    void deviceStatusUpdatesDevice() {
        handler.handle("smarthome/device/1/status", "ON");
        verify(deviceService).updateStatus(1L, "ON");
    }

    @Test
    void sensorDataPlainNumber() {
        when(sensorDataService.saveData(eq(2L), anyDouble())).thenReturn(new SensorData());
        handler.handle("smarthome/sensor/2/data", "28.5");
        verify(sensorDataService).saveData(2L, 28.5);
    }

    @Test
    void sensorDataJson() {
        when(sensorDataService.saveData(eq(4L), anyDouble())).thenReturn(new SensorData());
        handler.handle("smarthome/sensor/4/data", "{\"value\":61}");
        verify(sensorDataService).saveData(4L, 61.0);
    }

    @Test
    void sensorDataInvalidPayloadIsDropped() {
        handler.handle("smarthome/sensor/2/data", "abc");
        handler.handle("smarthome/sensor/2/data", "{\"foo\":1}");
        verify(sensorDataService, never()).saveData(anyLong(), anyDouble());
    }

    @Test
    void alertOneIsAlert() {
        handler.handle("smarthome/sensor/3/alert", "1");
        verify(sensorDataService).saveAlert(eq(3L), eq(true), anyString(), eq(1.0));
    }

    @Test
    void alertZeroIsSafe() {
        handler.handle("smarthome/sensor/3/alert", "0");
        verify(sensorDataService).saveAlert(eq(3L), eq(false), anyString(), eq(0.0));
    }

    @Test
    void alertJsonWithoutValueKeepsAlert() {
        handler.handle("smarthome/sensor/3/alert", "{\"message\":\"Gas cao\"}");
        verify(sensorDataService).saveAlert(3L, true, "Gas cao", null);
    }

    @Test
    void badTopicsAreIgnored() {
        handler.handle("other/device/1/status", "ON");
        handler.handle("smarthome/device/x/status", "ON");
        handler.handle("smarthome/device/1", "ON");
        handler.handle("smarthome/room/1/data", "1");
        verifyNoInteractions(deviceService, sensorDataService);
    }

    @Test
    void unknownDeviceSubtopicIgnored() {
        handler.handle("smarthome/device/1/availability", "online");
        verify(deviceService, never()).updateStatus(anyLong(), anyString());
    }
}
