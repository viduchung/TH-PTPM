# HỆ THỐNG QUẢN LÝ ĐẶT LỊCH SÂN BÓNG ĐÁ (FOOTBALL BOOKING SYSTEM)

> **Học phần:** Phát triển phần mềm hướng dịch vụ (SOA)  
> **Ngôn ngữ & Nền tảng:** Java 17 · Spring Boot 3.2.5 · Spring Cloud 2023.0.1 (Eureka)  
> **Kiến trúc:** Microservices Architecture · Database-per-Service · JWT RBAC · Distributed Saga  
> **Repository:** [https://github.com/viduchung/TH-PTPM](https://github.com/viduchung/TH-PTPM)  
> **Nhánh chốt:** `main` (Tích hợp toàn diện từ các nhánh feature của 4 thành viên)

---

## 1. THÀNH VIÊN NHÓM & MẢNG PHỤ TRÁCH (4 THÀNH VIÊN)

Hệ thống được thiết kế và phát triển bởi nhóm 4 thành viên theo phân công chuyên trách tại [BANG_PHAN_CONG_HANG_TUAN.xlsx](BANG_PHAN_CONG_HANG_TUAN.xlsx):

| STT | Thành viên | Mảng / Dịch vụ phụ trách | Cổng (Port) | Cơ sở dữ liệu | Nhánh Git |
| :---: | :--- | :--- | :---: | :---: | :--- |
| **1** | **Hoàng (Nhóm trưởng)** | `auth-service` — Xác thực, phân quyền & Eureka Server | `8080` / `8761` | `auth_db` | `feat/hoang-auth-service` |
| **2** | **Linh** | `pitchs-service` — Quản lý sân bóng & báo giá giờ vàng | `8082` (Đa instance) | `pitch_db` | `feat/linh-pitchs-service` |
| **3** | **Hùng** | `data-layer` — Tầng dữ liệu phân tán & CSDL xuyên suốt | Thư viện & Module dùng chung | Chuẩn hóa 4 DB | `feat/hung-data` |
| **4** | **Nhật** | `booking-service` — Nghiệp vụ đặt sân & điều phối giao dịch | `8081` / `8083` | `booking_db` | `feat/nhat-booking-service` |

---

## 2. MA TRẬN NHIỆM VỤ THEO TỪNG BUỔI THỰC HÀNH (TH1 – TH7)

| Buổi | Chủ đề | Hoàng (`auth-service`) | Linh (`pitchs-service`) | Hùng (`data-layer` / CSDL) | Nhật (`booking-service`) |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **TH1** | **CRUD cơ bản & RESTful API** | CRUD tài khoản/người dùng; API đăng ký, đăng nhập cơ bản (`/register`, `/login`). | CRUD Sân bóng: thêm – sửa – xóa – danh sách sân bóng. | Thiết kế `@Entity` + `Repository` cho User, Pitch, Booking; dựng mô hình ERD phân tán; thuật toán chống trùng lịch. | CRUD Booking: đặt – sửa – hủy – xem lịch đặt sân theo thời gian. |
| **TH2** | **Giao tiếp giữa các service** | Xác thực liên service, truyền token (`/api/v1/auth/validate`, M2M service key). | Cung cấp API báo giá `/api/pitches/{id}/price` cho `booking-service` gọi. | Chuẩn hóa hợp đồng DTO trao đổi liên dịch vụ, Mapper an toàn chống rò rỉ Entity. | `booking-service` gọi `pitchs-service` kiểm tra sân trống và lấy giá (WebClient/RestTemplate). |
| **TH3** | **REST nâng cao (Lọc, phân trang, validation)** | Bean Validation đăng ký; chuẩn response lỗi 401 Unauthorized / 403 Forbidden. | Tìm kiếm, lọc sân theo loại (`type`) và trạng thái (`status`); phân trang `Pageable`; validation dữ liệu sân. | Chuẩn định dạng `ApiResponse`, `PageResponse`, `ErrorResponse` (RFC 7807) & `@ControllerAdvice` xử lý 11 mã lỗi. | Phân trang & lọc danh sách booking; validation ngày giờ bắt đầu/kết thúc. |
| **TH4** | **MySQL / JPA (CSDL thật)** | `auth_db`: bảng `users`, băm BCrypt, quan hệ vai trò enum `Role`. Fallback H2. | `pitch_db`: bảng `pitches` + bảng giá giờ vàng `golden_hour_prices` (quan hệ 1-N). | Chuẩn Database-per-Service 4 DB độc lập, quản lý giao dịch ACID `@Transactional`, script init SQL 4 DB. | `booking_db`: quan hệ booking – field – user theo Logical ID (không foreign key vật lý xuyên DB). |
| **TH5** | **Bảo mật JWT / Spring Security** | Phát hành & xác thực JWT (HS256); phân quyền RBAC (`CUSTOMER`, `OWNER`, `ADMIN`). | Bảo vệ API quản lý sân bóng (chỉ `OWNER`/`ADMIN` được thêm/sửa/xóa sân). | Chuẩn `common-security` dùng chung; `UserPrincipal` nạp từ Claims không query DB; tiện ích `SecurityUtils`. | Bảo vệ endpoint booking theo vai trò người dùng (CUSTOMER đặt sân, OWNER duyệt đơn). |
| **TH6** | **Tích hợp & tiêu thụ (Consumer/Provider)** | Bảo mật liên dịch vụ khi tích hợp: header `X-Internal-Service-Key` cấp quyền `ROLE_INTERNAL_SERVICE`. | **Provider:** API `/quote` tính toán chi tiết giá thuê sân theo phút và giờ vàng, công thức làm tròn chuẩn. | Hợp đồng DTO tích hợp; bảng `idempotency_keys` & `IdempotencyService`; State Machine; Distributed Saga bù trừ. | **Consumer:** gọi `pitchs-service` lấy báo giá; xử lý lỗi & Idempotency khi đặt lịch chống double-booking. |
| **TH7** | **Microservices & Service Registry (Eureka)** | Khởi tạo `eureka-server` (Port 8761); đăng ký `auth-service` vào Eureka Registry. | Đăng ký `pitchs-service` vào Eureka; chạy nhiều instance (`8082`, `8083`,...); cân bằng tải. | Cấu hình Discovery chuẩn (Heartbeat 10s, Eviction 30s); khóa phân tán Concurrency Lock; Header tracking instance. | Đăng ký `booking-service` vào Eureka; gọi `pitchs-service` qua service-name + Load Balancing Ribbon/Spring Cloud. |

---

## 3. KIẾN TRÚC TỔNG THỂ HỆ THỐNG

```
                             +-----------------------------------+
                             |     EUREKA SERVER (Port 8761)     |
                             |         (Service Registry)        |
                             +-----------------------------------+
                                   ^             ^             ^
                    Đăng ký &      |             |             |  Đăng ký &
                    Phát hiện      |             |             |  Phát hiện
                                   v             v             v
+--------------------------+   +-------------------+   +----------------------------+
|  auth-service (Port 8080)|   | pitchs-service    |   | booking-service (Port 8081)|
|  - Cấp phát JWT Token    |   | (Port 8082, 8083) |   | - Quản lý đặt lịch         |
|  - Xác thực & Phân quyền |   | - Quản lý sân     |   | - Consumer gọi pitchs      |
|  - Validate Token M2M    |   | - Báo giá /quote  |   | - Điều phối Saga           |
+--------------------------+   +-------------------+   +----------------------------+
            |                            |                           |
            v                            v                           v
     +--------------+             +--------------+            +--------------+
     |   auth_db    |             |   pitch_db   |            |  booking_db  |
     |   (MySQL)    |             |   (MySQL)    |            |   (MySQL)    |
     +--------------+             +--------------+            +--------------+
                                         ^
                                         |  Dùng chung chuẩn dữ liệu, DTO contracts,
                                         |  Common Security, Idempotency, Concurrency Lock
                                         v
                         +-------------------------------+
                         |          data-layer           |
                         |  (Tầng dữ liệu phân tán dùng  |
                         |   chung & bộ 80 bài Unit Test)|
                         +-------------------------------+
```

### Các Chuẩn Kỹ Thuật Dùng Chung (Cross-Cutting Standards)
1. **JWT Secret Key dùng chung:**  
   `FootballBookingAuthServiceSecretKey2026_ChangeMeInProductionPlease_0123456789`  
   Thuật toán: HMAC-SHA256 (JJWT 0.12.x). Mọi service tự giải mã và xác thực token stateless mà không cần gọi DB.
2. **Khóa liên dịch vụ nội bộ (Machine-to-Machine):**  
   Header `X-Internal-Service-Key: FootballInternalSecretServiceKey2026`  
   Được dùng khi các service backend gọi trực tiếp lẫn nhau với quyền `ROLE_INTERNAL_SERVICE`.
3. **Database-per-Service:**  
   Các database độc lập hoàn toàn, liên kết qua Logical ID (`userId`, `pitchId` kiểu `BIGINT`), không dùng Foreign Key vật lý xuyên CSDL.

---

## 4. CẤU TRÚC THƯ MỤC REPOSITORY

```text
TH-PTPM/
├── BANG_PHAN_CONG_HANG_TUAN.xlsx       # Bảng phân công chi tiết 4 thành viên (TH1 - TH7)
├── README.md                           # Tài liệu tổng quan toàn bộ dự án
│
├── eureka-server/                      # Service Registry (Port 8761 - Hoàng)
│   ├── pom.xml
│   └── src/main/resources/application.properties
│
├── auth-service/                       # Dịch vụ Xác thực & Phân quyền (Port 8080 - Hoàng)
│   ├── pom.xml
│   ├── postman/                        # Postman Collection kiểm thử API Auth
│   └── src/main/
│       ├── java/com/football/authservice/
│       │   ├── controller/             # AuthController (/register, /login, /validate, /me), UserController
│       │   ├── service/                # AuthService, UserService
│       │   ├── security/               # JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig
│       │   ├── model/                  # User, Role (CUSTOMER, OWNER, ADMIN, INTERNAL_SERVICE)
│       │   └── repository/             # UserRepository
│       └── resources/application.properties
│
├── pitchs-service/                     # Dịch vụ Quản lý Sân & Báo giá (Port 8082 - Linh)
│   ├── pom.xml
│   ├── postman/                        # Postman Collection kiểm thử API Pitch & Quote
│   └── src/main/
│       ├── java/com/football/pitchservice/
│       │   ├── controller/             # PitchController (CRUD, /search, /price, /quote, /instance-info)
│       │   ├── service/                # PitchService, PitchServiceImpl (Báo giá giờ vàng & phút lẻ)
│       │   ├── security/               # JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig (RBAC)
│       │   ├── entity/                 # Pitch, GoldenHourPrice (Quan hệ 1-N)
│       │   ├── dto/                    # PitchRequestDTO, PitchResponseDTO, PriceResponseDTO, QuoteResponseDTO
│       │   └── repository/             # PitchRepository, GoldenHourPriceRepository
│       └── resources/application.yml
│
├── data-layer/                         # Tầng dữ liệu phân tán & CSDL xuyên suốt (Hùng)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/football/data/
│       │   ├── auth/                   # Entity & Repository cho auth_db
│       │   ├── pitch/                  # Entity & Repository cho pitch_db
│       │   ├── booking/                # Entity & Repository cho booking_db, BookingTransactionalService
│       │   ├── payment/                # Entity & Repository cho payment_db (TH4)
│       │   ├── dto/                    # Chuẩn DTO liên dịch vụ (TH2, TH6)
│       │   ├── mapper/                 # EntityDtoMapper chống lộ Schema
│       │   ├── exception/              # GlobalExceptionHandler (@ControllerAdvice 11 mã lỗi)
│       │   ├── security/               # Common Security (UserPrincipal, JwtTokenProvider, SecurityUtils)
│       │   ├── idempotency/            # IdempotencyService & IdempotencyRecord (TH6)
│       │   ├── consistency/            # StateMachineValidator & DistributedBookingSagaOrchestrator (TH6)
│       │   ├── concurrency/            # DistributedConcurrencyLockService chống race condition (TH7)
│       │   └── discovery/              # ServiceDiscoveryProperties, InstanceTrackingFilter (TH7)
│       └── test/                       # 80 bài kiểm thử tự động JUnit 5 & MockMvc (BUILD SUCCESS 100%)
│
├── database/                           # Bộ script SQL khởi tạo 4 CSDL chuẩn Database-per-Service
│   ├── 01_auth_db_init.sql             # CSDL auth_db và bảng users
│   ├── 02_pitch_db_init.sql            # CSDL pitch_db, pitches, golden_hour_prices
│   ├── 03_booking_db_init.sql          # CSDL booking_db, bookings, idempotency_keys
│   ├── 04_payment_db_init.sql          # CSDL payment_db và payments
│   └── init_all_databases.sql          # Script chạy 1 lần khởi tạo toàn bộ 4 CSDL kèm dữ liệu mẫu
│
├── config/                             # Cấu hình Eureka dùng chung & Docker Compose đa instance
│   ├── eureka-client-common.yml
│   └── docker-compose-multi-instance.yml
│
└── docs/                               # 7 Báo cáo kỹ thuật chi tiết của Hùng (TH1 - TH7) & ERD
    ├── BAO_CAO_TH1_HUNG.md
    ├── BAO_CAO_TH2_HUNG.md
    ├── BAO_CAO_TH3_HUNG.md
    ├── BAO_CAO_TH4_HUNG.md
    ├── BAO_CAO_TH5_HUNG.md
    ├── BAO_CAO_TH6_HUNG.md
    ├── BAO_CAO_TH7_HUNG.md
    └── ERD_DESIGN_EXPLANATION.md
```

---

## 5. HƯỚNG DẪN CÀI ĐẶT & KHỞI CHẠY HỆ THỐNG

### Bước 1: Khởi tạo Cơ sở dữ liệu MySQL
1. Mở MySQL Server qua phpMyAdmin, MySQL Workbench hoặc Command Line:
   ```bash
   mysql -u root -p < database/init_all_databases.sql
   ```
2. Script sẽ tự động tạo đủ 4 database: `auth_db`, `pitch_db`, `booking_db`, `payment_db` cùng đầy đủ bảng và dữ liệu mẫu.
*(Lưu ý: Nếu chưa cài MySQL, cả `auth-service` và `pitchs-service` đều có sẵn profile H2 in-memory: `-Dspring-boot.run.profiles=h2`).*

---

### Bước 2: Khởi động Service Registry (Eureka Server - Port 8761)
```bash
cd eureka-server
mvn spring-boot:run
```
> Truy cập giao diện quản trị Eureka Dashboard tại: **http://localhost:8761**

---

### Bước 3: Khởi động Auth Service (Port 8080)
Mở một cửa sổ Terminal mới:
```bash
cd auth-service
mvn spring-boot:run
```
* Swagger UI tài liệu API: **http://localhost:8080/swagger-ui.html**
* Tài khoản mẫu (mật khẩu: `password123`):
  * Khách hàng: `customer01`, `customer02` (`ROLE_CUSTOMER`)
  * Chủ sân: `owner01` (`ROLE_FIELD_OWNER` / `ROLE_OWNER`)
  * Quản trị viên: `admin01` (`ROLE_ADMIN`)

---

### Bước 4: Khởi động Pitchs Service (Port 8082 hoặc Đa Instance)
Mở một cửa sổ Terminal mới:
```bash
cd pitchs-service

# Chạy Instance 1 (Port mặc định 8082)
mvn spring-boot:run

# (Tùy chọn TH7) Chạy thêm Instance 2 tại Port 8083 để kiểm thử cân bằng tải
java -jar target/pitchs-service-1.0.0.jar --server.port=8083
```
* Swagger UI tài liệu API: **http://localhost:8082/swagger-ui.html**
* Kiểm tra thông tin instance đang phục vụ: **http://localhost:8082/api/pitches/instance-info**

---

### Bước 5: Chạy Bộ 80 Bài Kiểm Thử Tự Động (Data Layer)
```bash
cd data-layer
mvn clean test
```
* Kết quả: **80/80 tests pass 100% (BUILD SUCCESS)**, bao gồm kiểm thử giao dịch ACID, JWT RBAC, Idempotency, Saga bù trừ và Concurrency Race Condition đa luồng.

---

## 6. DANH SÁCH BỘ SƯU TẬP POSTMAN & BÁO CÁO KỸ THUẬT

* **Postman Collections:**
  * `auth-service`: [auth-service/postman/auth-service.postman_collection.json](auth-service/postman/auth-service.postman_collection.json)
  * `pitchs-service`: [pitchs-service/postman/pitchs-service.postman_collection.json](pitchs-service/postman/pitchs-service.postman_collection.json)
* **Báo cáo kỹ thuật chi tiết theo tuần:**
  * [Báo cáo TH1](docs/BAO_CAO_TH1_HUNG.md) — Thiết kế ERD, Entity, Repository & Giải thuật chống trùng lịch
  * [Báo cáo TH2](docs/BAO_CAO_TH2_HUNG.md) — Chuẩn DTO liên dịch vụ & Tầng Mapper an toàn
  * [Báo cáo TH3](docs/BAO_CAO_TH3_HUNG.md) — Response Envelope, Phân trang & Xử lý lỗi 11 HTTP status codes
  * [Báo cáo TH4](docs/BAO_CAO_TH4_HUNG.md) — Chuẩn Database-per-Service 4 DB & Quản trị giao dịch ACID
  * [Báo cáo TH5](docs/BAO_CAO_TH5_HUNG.md) — Bảo mật Stateless JWT, UserPrincipal không query DB & Phân quyền RBAC
  * [Báo cáo TH6](docs/BAO_CAO_TH6_HUNG.md) — Khóa bất biến Idempotency, State Machine & Distributed Saga bù trừ
  * [Báo cáo TH7](docs/BAO_CAO_TH7_HUNG.md) — Service Discovery Eureka, Khóa phân tán Concurrency Lock & Header Tracking

---

## 7. BẢN QUYỀN & THÔNG TIN DỰ ÁN
* **Đề tài:** Hệ thống Quản lý Đặt lịch Sân bóng đá (SOA Project)
* **Nhóm thực hiện:** Nhóm 4 thành viên (Hoàng - Linh - Hùng - Nhật)
* **GitHub Repository:** [https://github.com/viduchung/TH-PTPM.git](https://github.com/viduchung/TH-PTPM.git)
