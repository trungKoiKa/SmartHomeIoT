#ifndef NODE_CONFIG_H
#define NODE_CONFIG_H

#include "main.h"
#include <stdint.h>

/* ============================================================
 * TIMING (ms)
 * ============================================================ */
#define DHT_READ_PERIOD_MS      2000U
#define ANALOG_READ_PERIOD_MS    200U
#define LCD_UPDATE_PERIOD_MS     500U
#define BUTTON_SCAN_PERIOD_MS     20U

/* ============================================================
 * HARDWARE
 * ============================================================ */
#define DHT_SENSOR_COUNT         2U    /* 2 DHT11: PA6, PA11 */
#define MQ2_SAMPLE_COUNT        10
#define LDR_SAMPLE_COUNT        10

/* ============================================================
 * LORA FRAME FORMAT  (phải khớp Gateway ESP32)
 * [0xAA][SensorData_t payload][XOR checksum][0x55]
 * ============================================================ */
#define FRAME_START_BYTE    0xAAU
#define FRAME_END_BYTE      0x55U

/* Lệnh text gateway gửi xuống để yêu cầu đọc dữ liệu */
#define SENDDATA_CMD        "senddata"

/* ============================================================
 * SensorData_t — PHẢI TRÙNG KHÍT VỚI GATEWAY ESP32
 * Thứ tự field, kiểu dữ liệu, packed — KHÔNG ĐƯỢC THAY ĐỔI
 * ============================================================ */
#pragma pack(push, 1)

typedef struct {
    float    temperature;       /* °C                             */
    float    humidity;          /* %RH                            */
    uint16_t gasValue;          /* MQ-2 quy doi 0-100 %           */
    uint16_t lightValue;        /* LDR quy doi 0-100 %            */
    uint16_t tempThreshold;     /* Ngưỡng nhiệt độ (°C * 1)       */
    uint16_t gasThreshold;      /* Ngưỡng gas (0-100 %)           */
    uint8_t  relayStatus;       /* bit0=relay1, bit1=relay2       */
    uint8_t  alertStatus;       /* 0 = OK, 1 = CẢNH BÁO          */
    uint8_t  autoMode;          /* 1 = tự động, 0 = tay           */
} SensorData_t;

typedef struct {
    uint8_t      startByte;
    SensorData_t payload;
    uint8_t      checksum;
    uint8_t      endByte;
} SensorFrame_t;

#pragma pack(pop)

/* Gateway ESP32 (sensor_data.h) khai báo struct này lần thứ hai: lệch kích thước là báo lỗi lúc build.
 * (typedef mảng âm kích thước: chạy được trên mọi trình biên dịch C, kể cả armcc cũ không có _Static_assert) */
typedef char SensorData_size_must_be_19_bytes[(sizeof(SensorData_t) == 19U) ? 1 : -1];

#endif /* NODE_CONFIG_H */
