package com.smarthome.iot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.smarthome.iot.config.SecurityConfiguration;
import com.smarthome.iot.controller.client.ClientController;
import com.smarthome.iot.domain.Device;
import com.smarthome.iot.service.DeviceService;
import com.smarthome.iot.service.MqttUnavailableException;
import com.smarthome.iot.service.RoomService;
import com.smarthome.iot.service.SensorDataService;
import com.smarthome.iot.service.SensorService;
import com.smarthome.iot.service.UserService;
import com.smarthome.iot.service.VoiceCommandService;

@WebMvcTest(ClientController.class)
@Import(SecurityConfiguration.class)
class ClientControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RoomService roomService;
    @MockitoBean
    SensorService sensorService;
    @MockitoBean
    SensorDataService sensorDataService;
    @MockitoBean
    DeviceService deviceService;
    @MockitoBean
    VoiceCommandService voiceCommandService;
    @MockitoBean
    UserService userService;

    private static final String VOICE_BODY = "{\"text\":\"bật đèn 1\"}";

    @Test
    void guestCannotUseVoiceCommand() throws Exception {
        mvc.perform(post("/client/voice/command").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(VOICE_BODY))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(voiceCommandService);
    }

    @Test
    void guestCannotToggleDevice() throws Exception {
        mvc.perform(post("/client/device/1/toggle").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        verifyNoInteractions(deviceService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void userVoiceCommandReturnsDevices() throws Exception {
        Device d = new Device();
        d.setId(1L);
        d.setStatus("ON");
        when(voiceCommandService.execute("bật đèn 1"))
                .thenReturn(new VoiceCommandService.Result(true, "Đã bật Đèn 1", "ON", List.of(d)));

        mvc.perform(post("/client/voice/command").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(VOICE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.devices[0].id").value(1))
                .andExpect(jsonPath("$.devices[0].status").value("ON"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void mqttDownGives503() throws Exception {
        when(voiceCommandService.execute(any())).thenThrow(new MqttUnavailableException("down"));

        mvc.perform(post("/client/voice/command").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(VOICE_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void postWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/client/voice/command")
                .contentType(MediaType.APPLICATION_JSON).content(VOICE_BODY))
                // thiếu CSRF token: app cấu hình invalidSessionUrl nên bị redirect thay vì 403; điều quan trọng là bị chặn
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(voiceCommandService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void userToggleCallsService() throws Exception {
        Device d = new Device();
        d.setId(1L);
        d.setStatus("ON");
        when(deviceService.toggleStatus(1L)).thenReturn(d);

        mvc.perform(post("/client/device/1/toggle").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON"));
        verify(deviceService).toggleStatus(1L);
    }
}
