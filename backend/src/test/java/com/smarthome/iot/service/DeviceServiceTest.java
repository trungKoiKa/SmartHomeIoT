package com.smarthome.iot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smarthome.iot.domain.Device;
import com.smarthome.iot.repository.DeviceRepository;

class DeviceServiceTest {

    private DeviceRepository repo;
    private MqttService mqtt;
    private DeviceService service;
    private Device device;

    @BeforeEach
    void setUp() {
        repo = mock(DeviceRepository.class);
        mqtt = mock(MqttService.class);
        service = new DeviceService(repo, mqtt);
        device = new Device();
        device.setId(1L);
        device.setStatus("OFF");
        when(repo.findById(1L)).thenReturn(Optional.of(device));
        when(repo.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void setStatusPublishesThenSaves() {
        when(mqtt.publishCommand(1L, "ON")).thenReturn(true);
        Device saved = service.setStatus(1L, "on");
        assertEquals("ON", saved.getStatus());
        verify(mqtt).publishCommand(1L, "ON");
        verify(repo).save(device);
    }

    @Test
    void setStatusDoesNotSaveWhenMqttDown() {
        when(mqtt.publishCommand(1L, "ON")).thenReturn(false);
        assertThrows(MqttUnavailableException.class, () -> service.setStatus(1L, "ON"));
        verify(repo, never()).save(any());
        assertEquals("OFF", device.getStatus());
    }

    @Test
    void setStatusRejectsInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> service.setStatus(1L, "TOGGLE"));
        verify(mqtt, never()).publishCommand(any(), any());
    }

    @Test
    void setStatusUnknownDeviceReturnsNull() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertNull(service.setStatus(9L, "ON"));
        verify(mqtt, never()).publishCommand(any(), any());
    }

    @Test
    void toggleFlipsStatus() {
        when(mqtt.publishCommand(1L, "ON")).thenReturn(true);
        assertEquals("ON", service.toggleStatus(1L).getStatus());
    }

    @Test
    void updateStatusAcceptsOnlyOnOff() {
        service.updateStatus(1L, "garbage");
        verify(repo, never()).save(any());

        service.updateStatus(1L, "on");
        verify(repo).save(device);
        assertEquals("ON", device.getStatus());
    }
}
