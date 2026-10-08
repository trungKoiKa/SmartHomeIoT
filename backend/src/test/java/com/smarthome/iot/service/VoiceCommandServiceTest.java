package com.smarthome.iot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smarthome.iot.domain.Device;
import com.smarthome.iot.domain.Room;

class VoiceCommandServiceTest {

    private DeviceService deviceService;
    private VoiceCommandService service;

    private static Device device(long id, String name, String roomName) {
        Device d = new Device();
        d.setId(id);
        d.setName(name);
        d.setStatus("OFF");
        if (roomName != null) {
            Room r = new Room();
            r.setName(roomName);
            d.setRoom(r);
        }
        return d;
    }

    @BeforeEach
    void setUp() {
        deviceService = mock(DeviceService.class);
        service = new VoiceCommandService(deviceService);
        when(deviceService.getAllDevice()).thenReturn(List.of(
                device(1, "Đèn 1", "Phòng khách"),
                device(2, "Đèn 2", "Phòng ngủ"),
                device(3, "Quạt", "Phòng khách")));
        when(deviceService.setStatus(anyLong(), anyString())).thenAnswer(inv -> {
            Device d = new Device();
            d.setId(inv.getArgument(0));
            d.setName("Thiết bị " + inv.getArgument(0));
            d.setStatus(inv.getArgument(1));
            return d;
        });
    }

    @Test
    void turnOnByDigit() {
        var r = service.execute("Bật đèn 1");
        assertTrue(r.success());
        assertEquals("ON", r.status());
        verify(deviceService).setStatus(1L, "ON");
        verify(deviceService, never()).setStatus(2L, "ON");
    }

    @Test
    void numberWordsAreUnderstood() {
        assertTrue(service.execute("tắt đèn hai").success());
        verify(deviceService).setStatus(2L, "OFF");
    }

    @Test
    void openAndCloseSynonyms() {
        service.execute("mở quạt");
        verify(deviceService).setStatus(3L, "ON");
        service.execute("đóng quạt");
        verify(deviceService).setStatus(3L, "OFF");
    }

    @Test
    void turnOffEverything() {
        var r = service.execute("Tắt tất cả");
        assertTrue(r.success());
        assertEquals("OFF", r.status());
        verify(deviceService).setStatus(1L, "OFF");
        verify(deviceService).setStatus(2L, "OFF");
        verify(deviceService).setStatus(3L, "OFF");
    }

    @Test
    void turnOnEverything() {
        service.execute("bật tất cả thiết bị");
        verify(deviceService).setStatus(1L, "ON");
        verify(deviceService).setStatus(3L, "ON");
    }

    @Test
    void turnOffAllLightsOnlyTouchesLights() {
        service.execute("tắt tất cả đèn");
        verify(deviceService).setStatus(1L, "OFF");
        verify(deviceService).setStatus(2L, "OFF");
        verify(deviceService, never()).setStatus(3L, "OFF");
    }

    @Test
    void unknownDeviceDoesNotSendAnything() {
        var r = service.execute("bật máy giặt");
        assertFalse(r.success());
        verify(deviceService, never()).setStatus(anyLong(), anyString());
    }

    @Test
    void unknownAllTargetDoesNotTurnOffEverything() {
        var r = service.execute("tắt tất cả máy giặt");
        assertFalse(r.success());
        verify(deviceService, never()).setStatus(anyLong(), anyString());
    }

    @Test
    void missingActionIsRejected() {
        assertFalse(service.execute("đèn 1").success());
        assertFalse(service.execute("bật tắt đèn 1").success());
        assertFalse(service.execute("   ").success());
        assertFalse(service.execute(null).success());
        verify(deviceService, never()).setStatus(anyLong(), anyString());
    }

    @Test
    void sameNameInTwoRoomsNeedsRoomOrFails() {
        when(deviceService.getAllDevice()).thenReturn(List.of(
                device(1, "Đèn", "Phòng khách"),
                device(2, "Đèn", "Phòng ngủ")));

        var ambiguous = service.execute("bật đèn");
        assertFalse(ambiguous.success());
        verify(deviceService, never()).setStatus(anyLong(), anyString());

        var ok = service.execute("bật đèn phòng ngủ");
        assertTrue(ok.success());
        verify(deviceService).setStatus(2L, "ON");
    }

    @Test
    void mqttFailurePropagates() {
        when(deviceService.setStatus(any(), any())).thenThrow(new MqttUnavailableException("down"));
        assertThrows(MqttUnavailableException.class, () -> service.execute("bật đèn 1"));
    }
}
