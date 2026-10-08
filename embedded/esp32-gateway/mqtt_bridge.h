#pragma once

#include <Arduino.h>
#include "sensor_data.h"

void initMqttBridge();
void mqttBridgeLoop();
// Goi cho MOI frame LoRa hop le (ke ca khi du lieu khong doi) de xac nhan trang thai relay.
void mqttBridgePublishChangedFromLora(const SensorData_t& oldData, const SensorData_t& newData);
void mqttBridgeRequestNodeDataByInterval();
