#pragma once

#include <Arduino.h>

// LoRa payload: 19 bytes
typedef struct __attribute__((packed)) {
  float    temperature;
  float    humidity;  
  uint16_t gasValue;      // 0-100 %
  uint16_t lightValue;    // 0-100 %
  uint16_t tempThreshold; // do C
  uint16_t gasThreshold;  // 0-100 %
  uint8_t  relayStatus;   // bit0: relay1, bit1: relay2
  uint8_t  alertStatus;   // 0/1
  uint8_t  autoMode;      // 1: tu dong, 0: dieu khien tay (cung kieu voi STM32)
} SensorData_t;

// Phai khop STM32 (node_config.h): lech kich thuoc se bao loi luc bien dich.
static_assert(sizeof(SensorData_t) == 19, "SensorData_t phai dung 19 byte, dong bo voi node STM32");
