# 🏠 HomeSmartIoT

> Hệ thống nhà thông minh sử dụng **Spring Boot + MySQL + MQTT + ESP32/STM32/LoRa Gateway** để giám sát cảm biến và điều khiển thiết bị theo thời gian thực.

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?logo=java" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.12-green?logo=springboot" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql" alt="MySQL 8"/>
  <img src="https://img.shields.io/badge/MQTT-Eclipse%20Paho-purple" alt="MQTT Paho"/>
  <img src="https://img.shields.io/badge/JSP-JSTL-ff69b4" alt="JSP JSTL"/>
  <img src="https://img.shields.io/badge/license-MIT-brightgreen" alt="License"/>
</p>

---

## 📋 Mục lục

- [Giới thiệu](#-giới-thiệu)
- [Tính năng chính](#-tính-năng-chính)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Yêu cầu hệ thống](#️-yêu-cầu-hệ-thống)
- [Cài đặt và chạy dự án](#-cài-đặt-và-chạy-dự-án)
- [Thiết lập cơ sở dữ liệu](#-thiết-lập-cơ-sở-dữ-liệu)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Chức năng phần cứng](#-chức-năng-phần-cứng)
- [Định hướng phát triển](#-định-hướng-phát-triển)
- [Hướng dẫn đóng góp](#-hướng-dẫn-đóng-góp)
- [Quy ước code](#-quy-ước-code)
- [Thành viên nhóm](#-thành-viên-nhóm)
- [FAQ](#-faq)

---

## 📖 Giới thiệu

**HomeSmartIoT** là dự án xây dựng hệ thống nhà thông minh cho phép:

- Quản lý phòng trong nhà
- Theo dõi dữ liệu cảm biến theo thời gian thực
- Điều khiển thiết bị từ giao diện web
- Cảnh báo khi giá trị cảm biến vượt ngưỡng an toàn
- Kết nối phần cứng và backend qua **MQTT** (ESP32/LoRa Gateway)

Dự án hiện tại tập trung vào nền tảng web:

- **Spring Boot + Spring MVC + JSP** cho backend và giao diện
- **MySQL** lưu trữ dữ liệu hệ thống
- **MQTT (Eclipse Paho)** để publish/subscribe dữ liệu thiết bị
- Hỗ trợ phân quyền **Guest / User / Admin**

---

## ✨ Tính năng chính

| Module | Mô tả |
|---|---|
| 🔐 **Đăng nhập / Đăng ký** | Xác thực và tạo tài khoản người dùng |
| 👤 **Phân quyền** | `ADMIN` quản trị, `USER` sử dụng, `Guest` chỉ xem |
| 🏠 **Quản lý phòng** | CRUD phòng trong admin, hiển thị danh sách/chi tiết ở client |
| 🌡️ **Quản lý cảm biến** | Theo dõi cảm biến theo phòng, xem dữ liệu mới nhất |
| 💡 **Quản lý thiết bị** | CRUD thiết bị và gán phòng trong admin |
| 🎛️ **Điều khiển thiết bị** | Bật/tắt thiết bị từ client (chỉ User/Admin) |
| 🎙️ **Điều khiển giọng nói** | Nút micro ở trang thiết bị (Web Speech API, `vi-VN`): "bật đèn 1", "tắt quạt", "tắt tất cả"... |
| 📊 **Admin Dashboard** | Thống kê tổng quan users/rooms/sensors/devices |
| 🚨 **Cảnh báo** | Gateway báo `alert` (gas vượt ngưỡng) được lưu thành bản ghi `sensor_data` có cờ `alert` |
| 📡 **MQTT** | Backend subscribe dữ liệu/trạng thái từ gateway và publish lệnh điều khiển (xem bảng topic) |

---

## 📡 Kiến trúc hệ thống

```text
[Node IoT STM32: DHT11, MQ2, LDR, 2 relay]
        ▲ │  LoRa (UART, frame nhị phân 22 byte ↑ / lệnh điều khiển ↓)
        │ ▼
[ESP32 Gateway]  ◄── WiFi ──►  [MQTT Broker (Mosquitto)]
                                        ▲ │
                                        │ ▼  subscribe dữ liệu / publish lệnh
                               [Spring Boot Server] ◄── JPA ──► [MySQL]
                                        ▲ │
                                        │ ▼  HTTP (JSP + fetch JSON)
                          [Web UI: Admin + Client (nút micro giọng nói)]
```

### Luồng hoạt động

1. Gateway gửi `senddata` xuống node mỗi 10s; node trả frame LoRa chứa nhiệt độ, độ ẩm, gas, ánh sáng, trạng thái relay, cảnh báo.
2. Gateway publish lên broker MQTT (chỉ khi giá trị thay đổi; đẩy đủ khi vừa kết nối).
3. Backend (`MqttConfig` → `MqttMessageHandler`) parse topic/payload và lưu `sensor_data` / cập nhật trạng thái thiết bị.
4. Website hiển thị dữ liệu phòng/cảm biến/thiết bị.
5. Người dùng bật/tắt thiết bị bằng nút hoặc giọng nói (`VoiceCommandService`) → `DeviceService` publish `smarthome/device/{id}/command` → gateway gửi frame điều khiển xuống node → gateway publish lại trạng thái relay thật qua `.../status` để DB khớp với phần cứng.
6. Nếu MQTT không kết nối, backend trả 503 và **không** ghi trạng thái giả vào DB.

---

## 📡 Danh sách MQTT Topics (Catalog)

Dưới đây là danh sách các topic đang được sử dụng để giao tiếp giữa Server và ESP32 qua MQTT.

> **ID cố định:** gateway gắn cứng ID, nên bảng `sensors`/`devices` trong DB phải có đúng các ID này:
> sensor `1`=ánh sáng, `2`=nhiệt độ, `3`=gas, `4`=độ ẩm; device `1`=relay 1, `2`=relay 2.
> Lệnh điều khiển **không retained** (lệnh cũ không phát lại khi gateway kết nối lại); gateway tự đẩy lại trạng thái thật mỗi lần kết nối.
> Gateway dùng PubSubClient nên publish và subscribe ở QoS 0; backend publish lệnh QoS 1 và subscribe QoS 1.

| Chức năng | Luồng dữ liệu | Topic Pattern | Payload mẫu | Ý nghĩa |
|---|---|---|---|---|
| **Device Command** | Server &rarr; ESP32 | `smarthome/device/{id}/command` | `ON` / `OFF` | Lệnh điều khiển relay từ Server (nút web hoặc giọng nói). |
| **Device Status** | ESP32 &rarr; Server | `smarthome/device/{id}/status` | `ON` / `OFF` | Trạng thái relay thực tế đọc từ node sau mỗi frame LoRa. |
| **Sensor Data** | ESP32 &rarr; Server | `smarthome/sensor/{id}/data` | `{"value":28.5}` hoặc `28.5` | Dữ liệu đo của cảm biến (gateway gửi dạng JSON). |
| **Sensor Alert** | ESP32 &rarr; Server | `smarthome/sensor/3/alert` | `1` (nguy hiểm) / `0` (an toàn) | Cảnh báo gas vượt ngưỡng. |
| **Gateway Availability** | ESP32 &rarr; Broker | `smarthome/gateway/availability` | `online` / `offline` | Last Will: broker tự báo `offline` khi gateway rớt mạng. Backend chưa subscribe. |
| **Gateway State** | ESP32 &rarr; Broker | `smarthome/gateway/state` | JSON | Chế độ AUTO/MANUAL, ngưỡng... phục vụ debug. Backend chưa subscribe. |

### Giao diện web (client)

Giao diện sáng, kiểu Apple Home: Bootstrap 5.3 + `resources/client/css/smarthome.css` (token màu/khoảng cách), font Plus Jakarta Sans, bootstrap-icons.
Layout dùng chung ở `WEB-INF/view/client/layout/` (`header.jsp`, `footer.jsp`, thẻ `device-card.jsp`, `sensor-card.jsp`).

- Số liệu tự cập nhật mỗi 5 giây (`resources/client/js/live.js`): tải lại HTML của trang và thay các vùng `data-live-region`, không cần endpoint JSON; có nút Tạm dừng và tự dừng khi tab ẩn.
- Trang chi tiết cảm biến có biểu đồ Chart.js (`sensor-chart.js`) từ 10 lần đo gần nhất.
- Công tắc thiết bị và giọng nói nằm trong `device-control.js`.
- Trên điện thoại có thanh tab dưới cùng; mọi trang hỗ trợ bàn phím, `prefers-reduced-motion` và không chỉ dùng màu để báo trạng thái.
- Trang quản trị (`/admin`) vẫn dùng template SB Admin cũ.

## 📦 Công nghệ sử dụng

| Công nghệ | Vai trò |
|---|---|
| **Java 17** | Ngôn ngữ backend |
| **Spring Boot 3.5.12** | Nền tảng ứng dụng |
| **Spring MVC** | Controller + View |
| **Spring Security** | Xác thực và phân quyền |
| **Spring Session JDBC** | Quản lý session trong DB |
| **Spring Data JPA / Hibernate** | ORM dữ liệu |
| **MySQL 8** | Cơ sở dữ liệu |
| **JSP / JSTL / Bootstrap 5** | Giao diện web (client: smarthome.css; admin: SB Admin) |
| **Chart.js** | Biểu đồ lịch sử cảm biến |
| **Eclipse Paho MQTT** | MQTT client của backend (publish/subscribe) |
| **Mosquitto** | MQTT broker (cấu hình dev trong `infra/mosquitto`) |
| **PubSubClient (Arduino)** | MQTT client của gateway ESP32 |
| **Web Speech API** | Nhận dạng giọng nói trên trình duyệt (`vi-VN`) |
| **JUnit 5 / Mockito / H2** | Kiểm thử backend |
| **Git & GitHub** | Quản lý mã nguồn |

---

## ⚙️ Yêu cầu hệ thống

| Thành phần | Phiên bản khuyến nghị |
|---|---|
| Java JDK | **17** |
| Maven | **3.8+** |
| MySQL | **8.0+** |
| IDE | VS Code / IntelliJ IDEA / STS |
| MQTT Broker | Mosquitto / EMQX / HiveMQ |
| Gateway | Arduino IDE + ESP32 core + thư viện PubSubClient |
| Node | Keil MDK-ARM (STM32F103) |

---

## 🚀 Cài đặt và chạy dự án

### 1. Clone project

```bash
git clone https://github.com/your-username/HomeSmartIoT.git
cd HomeSmartIoT
```

### 2. Cấu hình (không commit mật khẩu)

`application.properties` chỉ chứa giá trị mặc định; thông tin nhạy cảm đặt trong
`backend/src/main/resources/application-local.properties` (đã `.gitignore`, mẫu: `application-local.properties.example`)
hoặc biến môi trường `DB_USERNAME`, `DB_PASSWORD`, `MQTT_BROKER_URL`, `MQTT_USERNAME`, `MQTT_PASSWORD`:

```properties
# application-local.properties
spring.datasource.username=root
spring.datasource.password=<mat-khau-db>
mqtt.broker.url=tcp://localhost:1883
mqtt.username=
mqtt.password=
```

Các thiết lập khác (`mqtt.enabled`, `mqtt.client.id`, topic...) nằm sẵn trong `application.properties`.
`mqtt.client.id` của backend (`homesmart`) phải **khác** client id của gateway.

> Lưu ý: nếu `mqtt.broker.url` để trống, app vẫn chạy nhưng MQTT sẽ không kết nối.

### 3. Broker MQTT dev (tuỳ chọn)

Cài Mosquitto, rồi từ thư mục gốc repo (PowerShell):

```powershell
infra\mosquitto\setup-users.ps1     # tạo infra/mosquitto/passwd (đã gitignore), in mật khẩu ngẫu nhiên 1 lần cho user backend/gateway - chép vào application-local.properties và secrets.h
infra\mosquitto\start-broker.ps1    # chạy broker ở cổng 1884, bắt buộc user/mật khẩu
```

Đặt `mqtt.broker.url=tcp://localhost:1884` và user/mật khẩu tương ứng vào `application-local.properties`.
Nếu gateway ở máy khác, mở cổng 1884 trên firewall và đặt `MQTT_HOST` trong `secrets.h` là IP máy chạy broker.

### 4. Chạy project

Nếu đã cài Maven:

```bash
mvn spring-boot:run
```

Nếu dùng Maven Wrapper (trong thư mục `backend`):

```bash
mvnw.cmd spring-boot:run
```

Chạy test:

```bash
mvnw.cmd test
```

Test mặc định dùng H2 và tắt MQTT. Test end-to-end với broker và MySQL thật (cần broker đang chạy, DB `smarthome_e2e`
và các biến môi trường `E2E_DB_PASSWORD`, `E2E_MQTT_BACKEND_PASS`, `E2E_MQTT_GATEWAY_USER`, `E2E_MQTT_GATEWAY_PASS`):

```bash
mvnw.cmd test -Dtest=GatewayMqttEndToEndTest -De2e=true
```

### 5. Truy cập ứng dụng

```text
http://localhost:8080
```

- Client: `/`
- Login: `/login`
- Register: `/register`
- Admin: `/admin`

---

## 🗄️ Thiết lập cơ sở dữ liệu

### Tạo database

```sql
CREATE DATABASE smarthome CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Một số bảng chính

- `users`
- `roles`
- `rooms`
- `sensors`
- `sensor_data`
- `devices`
- `spring_session`, `spring_session_attributes`

Cảnh báo không có bảng riêng: là cột `alert` của `sensor_data`.
Bảng tự tạo nhờ `ddl-auto=update`, nhưng **ID phải khớp gateway**: cần có sensor ID 1–4 và device ID 1–2
(tạo trong trang admin theo đúng thứ tự). Role `USER`, `ADMIN` cần có sẵn khi đăng ký/đăng nhập.

---

### Quan hệ dữ liệu cơ bản

roles 1 --- n users
rooms 1 --- n sensors
rooms 1 --- n devices
sensors 1 --- n sensor_data

---

## 📁 Cấu trúc dự án

```text
HomeSmartIoT/
├── backend/                         # Spring Boot + JSP + MQTT
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/smarthome/iot/
│   │   │   │   ├── config/          # Security, MqttConfig, WebMvc
│   │   │   │   ├── controller/
│   │   │   │   │   ├── admin/
│   │   │   │   │   └── client/
│   │   │   │   ├── domain/
│   │   │   │   │   └── dto/
│   │   │   │   ├── repository/
│   │   │   │   └── service/         # DeviceService, MqttService, MqttMessageHandler, VoiceCommandService...
│   │   │   │       └── validator/
│   │   │   ├── resources/
│   │   │   │   ├── application.properties
│   │   │   │   └── application-local.properties.example
│   │   │   └── webapp/
│   │   │       ├── WEB-INF/view/
│   │   │       │   ├── admin/
│   │   │       │   └── client/
│   │   │       └── resources/
│   │   └── test/java/com/smarthome/iot/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── embedded/
│   ├── stm32-node/                  # Firmware node cảm biến STM32
│   │   ├── Core/
│   │   │   ├── Inc/
│   │   │   └── Src/
│   │   ├── Drivers/
│   │   │   ├── CMSIS/
│   │   │   └── STM32F1xx_HAL_Driver/
│   │   ├── myLib/
│   │   │   ├── inc/
│   │   │   └── src/
│   │   ├── MDK-ARM/
│   │   ├── HomeSmart.ioc
│   │   └── README.md
│   └── esp32-gateway/               # Firmware gateway ESP32 (LoRa -> MQTT)
│       ├── Gateway_Esp32.ino
│       ├── mqtt_bridge.{h,cpp}
│       ├── lora_receiver.{h,cpp}
│       ├── lcd_display.{h,cpp}
│       ├── sensor_data.h
│       └── secrets.example.h
├── infra/
│   └── mosquitto/                   # Broker dev: mosquitto.conf, setup-users.ps1, start-broker.ps1
└── README.md
```

---

## 🔌 Chức năng phần cứng

> Firmware đã có trong repo tại `embedded/stm32-node`, sẵn sàng build/nạp và tích hợp trực tiếp với Gateway ESP32.


### Node cảm biến (STM32)

- Dùng `STM32F103` với kiến trúc state machine để chạy vòng lặp ổn định, không block.
- Đọc cảm biến định kỳ:
  - DHT11 x2 (nhiệt độ/độ ẩm, lấy giá trị trung bình nếu đọc hợp lệ)
  - MQ2 (gas) và LDR (ánh sáng) qua ADC, có lọc trung bình nhiều mẫu
- Điều khiển 2 relay + còi cảnh báo với 2 chế độ:
  - `AUTO`: tự động bật/tắt relay theo ngưỡng MQ2/LDR (có hysteresis chống nhấp nháy)
  - `MANUAL`: bật/tắt relay bằng nút nhấn hoặc lệnh điều khiển từ Gateway
- Hiển thị thông tin sensor, trạng thái relay và mode trên LCD I2C 16x2.
- Gửi telemetry qua LoRa theo chu kỳ (mặc định 10s/lần) hoặc gửi ngay khi giữ nút view.
- Gói dữ liệu theo frame nhị phân có `start byte`, `payload`, `XOR checksum`, `end byte` để tăng độ tin cậy truyền nhận.
- Hỗ trợ nhận frame điều khiển ngược từ Gateway (qua UART-LoRa), kiểm tra checksum trước khi áp dụng lệnh.

### Gateway (ESP32)

Mã nguồn: `embedded/esp32-gateway` (Arduino, thư viện **PubSubClient**).

- Nhận dữ liệu từ node qua LoRa, hỏi node bằng lệnh `senddata` mỗi 10s
- Chuyển tiếp lên MQTT broker (xem bảng topic): chỉ publish giá trị thay đổi, đẩy toàn bộ khi kết nối lại
- Nhận lệnh từ `smarthome/device/{id}/command`, đóng gói frame LoRa gửi xuống node rồi xác nhận lại trạng thái relay thật qua `.../status`
- Lệnh relay khi node đang `AUTO` sẽ chuyển node sang `MANUAL`
- Last Will `smarthome/gateway/availability` = `offline` khi gateway rớt mạng

Cấu hình: copy `embedded/esp32-gateway/secrets.example.h` thành `secrets.h` (đã `.gitignore`) và điền WiFi + MQTT.

> Struct `SensorData_t` được khai báo ở **hai nơi** (`stm32-node/myLib/inc/node_config.h` và `esp32-gateway/sensor_data.h`) và phải khớp từng byte (19 byte, packed). Cả hai có kiểm tra kích thước lúc biên dịch.

### Điều khiển bằng giọng nói

Đăng nhập → `/client/device` → bấm nút micro và nói lệnh. Trình duyệt nhận dạng (Chrome/Edge, cần HTTPS hoặc `localhost`),
gửi văn bản tới `POST /client/voice/command`; backend phân tích tiếng Việt (bật/mở, tắt/đóng, tên thiết bị, số "một/hai", "tất cả") rồi publish lệnh MQTT.
Tên thiết bị trong admin nên ngắn, dễ đọc (ví dụ "Đèn 1", "Quạt").

---

## 📈 Định hướng phát triển

- Thêm biểu đồ realtime cho sensor data
- Bổ sung notify qua Email/Telegram khi vượt ngưỡng
- Tách profile cấu hình `dev/staging/prod`
- Seed dữ liệu role mặc định (`USER`, `ADMIN`) và sensor/device ID cố định tự động
- Backend subscribe `smarthome/gateway/availability` để hiển thị gateway online/offline
- Dùng TLS cho MQTT khi triển khai ngoài mạng nội bộ

---

## 🤝 Hướng dẫn đóng góp

### Quy trình làm việc với Git

```bash
git checkout master
git pull origin master
git checkout -b feature/ten-tinh-nang
```

Sau khi code xong:

```bash
git add .
git commit -m "feat: thêm chức năng quản lý phòng"
git push origin feature/ten-tinh-nang
```

Sau đó tạo **Pull Request** lên nhánh `master`.

> Không nên push trực tiếp vào `master` khi làm việc nhóm.

---

## 📝 Quy ước code

### Commit message

Sử dụng format:

```text
<type>: <mô tả ngắn>
```

### Ví dụ

```text
feat: thêm chức năng quản lý thiết bị
fix: sửa lỗi phân quyền guest điều khiển thiết bị
docs: cập nhật README
refactor: tách service xử lý MQTT
```

### Quy ước đặt tên

| Thành phần | Quy ước | Ví dụ |
|---|---|---|
| Class | `PascalCase` | `UserController` |
| Method / Variable | `camelCase` | `handleLogin()` |
| Constant | `UPPER_SNAKE_CASE` | `MAX_SENSOR_VALUE` |
| Package | `lowercase` | `com.smarthome.iot` |
| Git branch | `kebab-case` | `feature/device-control` |

---

## 👥 Thành viên nhóm

| Họ tên | Mã Sinh Viên | Vai trò |
|---|---|---|
| Nguyễn Tiến Quân | B22DCVT425 | Trưởng nhóm |
| Hoàng Trung Anh | B22DCVT017 | Thành viên |
| Nguyễn Khánh Nam | B22DCVT361 | Thành viên |


---

## 🖼️ Ảnh giao diện

## 🖼️ Giao diện trang chủ

![Homepage](https://raw.githubusercontent.com/hoanganh04-11/miniproject_spring_mvc/master/src/main/webapp/resources/client/img/homepage.png)

---

## 🖼️ Giao diện admin

![Admin](https://raw.githubusercontent.com/hoanganh04-11/miniproject_spring_mvc/master/src/main/webapp/resources/client/img/admin.png)


---

## ❓ FAQ

<details>
  <summary><b>Lỗi port 8080 đã được sử dụng?</b></summary>

Kiểm tra process đang chiếm cổng:

```bash
netstat -ano | findstr :8080
```

Sau đó tắt process:

```bash
taskkill /PID <PID> /F
```

</details>

<details>
  <summary><b>Lỗi "mvn is not recognized"?</b></summary>

Bạn chưa cài Maven hoặc chưa thêm Maven vào `PATH`.

Có thể dùng luôn Maven Wrapper:

```bash
mvnw.cmd spring-boot:run
```

</details>

<details>
  <summary><b>Lỗi không kết nối được MySQL?</b></summary>

Kiểm tra lại:

- MySQL đã bật chưa
- Tên database đã đúng chưa
- Username/password đã đúng chưa
- Cổng MySQL có phải `3306` không

</details>

<details>
  <summary><b>Mqtt status báo disconnected?</b></summary>

Kiểm tra:

- `mqtt.enabled=true`
- `mqtt.broker.url` đã điền chưa (ví dụ `tcp://localhost:1883`, broker dev của repo dùng cổng `1884`)
- Broker MQTT có đang chạy không, user/mật khẩu có đúng không
- Firewall/network có chặn cổng broker không

Xem log backend: có dòng `Connected to broker` và `Subscribe -> smarthome/...` là đã kết nối.
Nếu MQTT mất kết nối, bật/tắt thiết bị sẽ trả lỗi 503.

Test nhanh bằng Mosquitto:

```bash
mosquitto_sub -h localhost -p 1884 -u <user> -P <pass> -t "smarthome/#" -v
```

</details>


---

<p align="center">
  Made with ❤️ by <strong>Nhóm 11 - D22_OOP_TEL_PTIT</strong>
</p>

