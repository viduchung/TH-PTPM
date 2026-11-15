# TẦNG DỮ LIỆU PHÂN TÁN (DATA LAYER) — HỆ THỐNG QUẢN LÝ ĐẶT LỊCH SÂN BÓNG ĐÁ

> **Học phần:** Phát triển phần mềm hướng dịch vụ (SOA)  
> **Người thực hiện:** VŨ ĐỨC HÙNG  
> **Tài khoản Git:** `viduchung`  
> **Nhánh Git:** `feat/hung-data`  
> **Vai trò:** Database Administrator & Backend Data Engineer (Phụ trách toàn bộ mảng dữ liệu & CSDL phân tán)  
> **Nội dung hoàn thành:**
> - **TUẦN 1 (TH1):** Thiết kế @Entity + Repository cho User/Booking/Pitch; Dựng ERD
> - **TUẦN 2 (TH2):** Chuẩn DTO trao đổi giữa service, tránh trùng model (Entity Leaking) & Bộ Mapper an toàn
> - **TUẦN 3 (TH3):** Chuẩn format response (ApiResponse, PageResponse) & @ControllerAdvice xử lý lỗi chung (11 HTTP Status Codes)
> - **TUẦN 4 (TH4):** Chuẩn Database-per-Service (4 CSDL độc lập), Quản trị giao dịch @Transactional & Script init SQL 4 DB
> - **TUẦN 5 (TH5):** Chuẩn common-security dùng chung, Stateless JWT (JJWT 0.12.6), UserPrincipal nạp từ Claims không query CSDL, Phân quyền RBAC
> - **TUẦN 6 (TH6):** Hợp đồng DTO tích hợp liên dịch vụ, Khóa bất biến Idempotency Key, State Machine Validator & Distributed Saga Bù trừ
> - **TUẦN 7 (TH7):** Cấu hình nhiều instance, Chuẩn hóa Service Discovery (Eureka), Khóa phân tán Concurrency Lock & Docker Compose đa node

---

## 1. TỔNG QUAN TIẾN ĐỘ THỰC HÀNH CỦA HÙNG

Theo bảng phân công nhóm (`BANG_PHAN_CONG_HANG_TUAN.xlsx`), Hùng phụ trách tầng dữ liệu xuyên suốt dự án. Đến nay đã hoàn thành đầy đủ:

### 🌟 Buổi 1 (TH1): CRUD Cơ bản, Entity, Repository & ERD Phân tán
1. **Kiến trúc CSDL Phân tán (Database-per-Service):** 3 CSDL độc lập `auth_db`, `pitch_db`, `booking_db`.
2. **Khóa Logic (Logical ID):** Lưu `user_id` và `pitch_id` dạng `BIGINT`, tuyệt đối không tạo Foreign Key vật lý xuyên database.
3. **Mô hình JPA & Repository:** Hoàn thiện `@Entity` và `@Repository` cho `User`, `Pitch`, `GoldenHourPrice`, `Booking`.
4. **Giải thuật Chống Trùng Lịch:** JPQL `countOverlappingBookings` với logic toán học giao thoa thời gian $O(\log N)$.
5. **Script SQL:** 4 tệp DDL script trong thư mục `database/`.

### 🌟 Buổi 2 (TH2): Giao Tiếp Giữa Các Service & Chuẩn Hóa DTO
1. **Xóa bỏ Anti-Pattern "Dùng chung Model":** Triệt tiêu hoàn toàn rủi ro Schema Coupling, LazyInitializationException, rò rỉ dữ liệu nhạy cảm (`passwordHash`).
2. **Chuẩn hóa Hợp đồng DTO Liên dịch vụ:**
   - Auth $\leftrightarrow$ Booking/Gateway: `TokenValidationRequestDTO`, `TokenValidationResponseDTO`, `UserResponseDTO`.
   - Pitch $\leftrightarrow$ Booking: `PriceResponseDTO` (tinh gọn cho endpoint `/api/pitches/{id}/price`), `PitchResponseDTO`.
   - Booking $\leftrightarrow$ Client/Payment: `BookingRequestDTO`, `BookingResponseDTO` (tự động tính `durationHours`), `BookingStatusUpdateDTO`.
   - Toàn hệ thống: Cấu trúc phản hồi chung `ApiResponse<T>`.
3. **Tầng Mapper Chuyển Đổi An Toàn:** `UserMapper`, `PitchMapper`, `BookingMapper`.
4. **Nguyên lý Tolerant Reader:** Đảm bảo hệ thống giải tuần tự hóa an toàn ngay cả khi dịch vụ đối tác bổ sung trường mới.

### 🌟 Buổi 3 (TH3): REST Nâng Cao, Chuẩn Format Response & @ControllerAdvice
1. **Bộ Phản Hồi Chuẩn:**
   - Envelope thành công: `ApiResponse<T>`.
   - Phân trang nâng cao: `PageResponse<T>` (giảm 60% overhead metadata so với `PageImpl`).
   - Phản hồi lỗi chuẩn RFC 7807: `ErrorResponse` (kèm map chi tiết các trường vi phạm Bean Validation).
2. **Xử Lý Lỗi Tập Trung Toàn Hệ Thống (`GlobalExceptionHandler`):**
   - Đánh chặn AOP xử lý 11 loại ngoại lệ và chuẩn hóa ma trận HTTP Status: 200, 201, 204, 400, 401, 403, 404, 405, 409, 500, 503.
   - Bắt trọn vẹn lỗi Bean Validation (`MethodArgumentNotValidException`), ConstraintViolation, Conflict (trùng lịch), ResourceNotFound.

### 🌟 Buổi 4 (TH4): MySQL / JPA (CSDL Thật), Database-per-Service & @Transactional
1. **Chuẩn hóa 4 CSDL Độc lập (Database-per-Service):**
   - `auth_db`: port 8080 (Hoàng) - bảng `users`.
   - `pitch_db`: port 8082 (Linh) - bảng `pitches`, `golden_hour_prices`.
   - `booking_db`: port 8081/8083 (Nhật) - bảng `bookings`.
   - `payment_db`: port 8084 (Hùng) - bảng `payments`.
2. **Quản trị Giao dịch ACID (@Transactional):**
   - Cài đặt `BookingTransactionalService`: Rollback tự động khi có xung đột trùng lịch thi đấu (`ConflictException`).
   - Giao dịch nguyên tử: Thanh toán và duyệt đơn cùng commit hoặc cùng rollback.
   - Mức cô lập `READ_COMMITTED` chống Dirty Read, `readOnly = true` tối ưu hóa hiệu năng Hibernate.
3. **Bộ Script SQL DDL 4 DB:** 5 tệp tin SQL tại `database/` (bao gồm `04_payment_db_init.sql` và `init_all_databases.sql`).
4. **Kiểm thử tự động:** 41/41 bài kiểm thử JUnit 5, JPA & MockMvc ĐẠT 100% (BUILD SUCCESS).

### 🌟 Buổi 5 (TH5): Chuẩn Common-Security Dùng Chung, JWT & Phân Quyền RBAC
1. **Kiến trúc Stateless Security:** Đóng gói thư viện dùng chung `com.football.data.security` sử dụng JJWT 0.12.6 ký số HMAC-SHA256 256-bit.
2. **UserPrincipal (Tự phục hồi Context):** Cài đặt `UserDetails`, Factory `fromClaims` tái tạo đầy đủ thông tin người dùng (`id`, `username`, `roles`) trực tiếp từ JWT **không cần truy vấn ngược lại CSDL `auth_db`**.
3. **Bộ lọc JwtAuthenticationFilter & Handlers:** Tự động bắt Bearer token; chuyển đổi toàn bộ lỗi 401 Unauthorized và 403 Forbidden sang JSON chuẩn RFC 7807 (`ErrorResponse`).
4. **Tiện ích SecurityUtils:** Cung cấp hàm tĩnh Thread-safe `getCurrentUserId()`, `getCurrentUsername()`, `hasRole()` giúp Nhật (`booking-service`) lấy `userId` chính chủ, loại bỏ rủi ro giả mạo ID.

### 🌟 Buổi 6 (TH6): Hợp Đồng DTO Tích Hợp & Đảm Bảo Nhất Quán Dữ Liệu Phân Tán
1. **Khóa Bất Biến (Idempotency Key):** Bảng `idempotency_keys` và `IdempotencyService` theo chuẩn Stripe/PayPal, lưu trữ phản hồi JSON để replay ngay khi nhận request trùng lặp, chống double-booking và double-charge khi rớt mạng.
2. **Máy Trạng Thái Hữu Hạn (`BookingStateTransitionValidator`):** Quản lý nghiêm ngặt quy tắc chuyển đổi trạng thái, khóa cứng Terminal States (`TU_CHOI`, `DA_HUY`), ngăn chặn bất nhất trạng thái phân tán.
3. **Điều Phối Saga & Bù Trừ Dữ Liệu (`DistributedBookingSagaOrchestrator`):** Điều phối giao dịch phân tán 3 bên (Pitch -> Booking -> Payment), tự động kích hoạt Compensating Transaction chuyển đơn về `TU_CHOI` khi thanh toán thất bại.
4. **Hợp đồng DTO tích hợp chuyên dụng:** Gói `com.football.data.dto.integration` đảm bảo Tolerant Reader Pattern.

### 🌟 Buổi 7 (TH7): Cấu Hình Nhiều Instance & Chuẩn Hóa Service Discovery
1. **Chuẩn hóa Service Discovery (Eureka Client):** Rút ngắn nhịp tim Heartbeat xuống 10s và thời gian trục xuất xuống 30s (`ServiceDiscoveryProperties`), hỗ trợ phát hiện sự cố nhanh chóng.
2. **Kiểm chứng Cân Bằng Tải:** `InstanceTrackingFilter` tự động đóng dấu Header `X-Served-By-Instance` và `X-Service-Name` vào mọi response để tester thấy request luân phiên qua các node (vd: 8081 -> 8083 -> 8081).
3. **Khóa Phân Tán Chống Tranh Chấp Lịch (`DistributedConcurrencyLockService`):** Tuần tự hóa các thao tác kiểm tra trùng lịch theo `pitchId`, giải quyết triệt để Race Condition khi nhiều instance cùng nhận request đặt cùng 1 sân tại cùng 1 mili-giây.
4. **Kịch bản Docker Compose Đa Instance:** Cung cấp `docker-compose-multi-instance.yml` triển khai đồng thời Eureka + MySQL + 2 instance `pitchs-service` + 2 instance `booking-service`.
5. **Kiểm thử tự động toàn diện:** 80/80 bài kiểm thử JUnit 5, JPA, MockMvc & Concurrency ĐẠT 100% (BUILD SUCCESS).

---

## 2. CẤU TRÚC THƯ MỤC DỰ ÁN

```text
TH-PTPM/
├── BANG_PHAN_CONG_HANG_TUAN.xlsx       # Bảng phân công nhiệm vụ chi tiết nhóm TH1 - TH7
├── README.md                           # Hướng dẫn tổng quan & kết quả TH1 - TH4 của Hùng
├── database/                           # Bộ script SQL khởi tạo 4 CSDL theo chuẩn Database-per-Service
│   ├── 01_auth_db_init.sql             # Khởi tạo auth_db và bảng users (Hoàng - Port 8080)
│   ├── 02_pitch_db_init.sql            # Khởi tạo pitch_db và bảng pitches, golden_hour_prices (Linh - Port 8082)
│   ├── 03_booking_db_init.sql          # Khởi tạo booking_db và bảng bookings (Nhật - Port 8081/8083)
│   ├── 04_payment_db_init.sql          # Khởi tạo payment_db và bảng payments (Hùng - Port 8084 - TH4)
│   └── init_all_databases.sql          # Script gộp khởi tạo toàn bộ 4 database trên MySQL / phpMyAdmin
├── docs/                               # Tài liệu thiết kế & báo cáo kỹ thuật của Hùng
│   ├── BAO_CAO_TH1_HUNG.md             # Báo cáo kỹ thuật chi tiết bài thực hành Tuần 1
│   ├── BAO_CAO_TH2_HUNG.md             # Báo cáo kỹ thuật chi tiết bài thực hành Tuần 2
│   ├── BAO_CAO_TH3_HUNG.md             # Báo cáo kỹ thuật chi tiết bài thực hành Tuần 3
│   ├── BAO_CAO_TH4_HUNG.md             # Báo cáo kỹ thuật chi tiết bài thực hành Tuần 4
│   ├── ERD_DESIGN_EXPLANATION.md       # Phân tích chuyên sâu thiết kế ERD & ranh giới dữ liệu
│   └── diagrams/                       # Mã nguồn sơ đồ thực thể Mermaid (.mmd)
│       ├── erd_overview.mmd            # Sơ đồ ERD tổng thể hệ thống
│       ├── erd_auth_service.mmd        # Sơ đồ ERD auth_db
│       ├── erd_pitch_service.mmd       # Sơ đồ ERD pitch_db
│       └── erd_booking_service.mmd     # Sơ đồ ERD booking_db
└── data-layer/                         # Module Java Spring Boot JPA & DTO Contracts độc lập
    ├── pom.xml                         # Cấu hình Maven (Java 17, Spring Boot 3.2.5, Web, JPA, Jackson, H2, Lombok)
    └── src/
        ├── main/
        │   ├── java/com/football/data/
        │   │   ├── FootballDataApplication.java
        │   │   ├── auth/               # Miền dữ liệu auth_db (User, Role, UserRepository)
        │   │   ├── pitch/              # Miền dữ liệu pitch_db (Pitch, PitchType, PitchStatus, GoldenHourPrice,...)
        │   │   ├── booking/            # Miền dữ liệu booking_db (Booking, BookingStatus, BookingRepository)
        │   │   │   └── service/        # BookingTransactionalService (@Transactional ACID - TH4)
        │   │   ├── payment/            # Miền dữ liệu payment_db (Payment Entity & PaymentRepository - TH4)
        │   │   ├── dto/                # Hợp đồng DTO chuẩn hóa trao đổi liên dịch vụ (TH2)
        │   │   │   ├── auth/           # TokenValidationRequestDTO, TokenValidationResponseDTO, UserResponseDTO
        │   │   │   ├── pitch/          # PriceResponseDTO, PitchResponseDTO, GoldenHourPriceDTO
        │   │   │   ├── booking/        # BookingRequestDTO, BookingResponseDTO, BookingStatusUpdateDTO
        │   │   │   └── common/         # ApiResponse<T>, PageResponse<T>, ErrorResponse (TH3)
        │   │   ├── mapper/             # Tầng Mapper an toàn chống rò rỉ schema (TH2)
        │   │   │   ├── UserMapper.java
        │   │   │   ├── PitchMapper.java
        │   │   │   └── BookingMapper.java
        │   │   ├── exception/          # Bộ ngoại lệ nghiệp vụ & GlobalExceptionHandler (@ControllerAdvice - TH3)
        │   │   ├── security/           # Chuẩn Common Security dùng chung, JWT Filter & RBAC (TH5)
        │   │   │   ├── JwtProperties.java
        │   │   │   ├── UserPrincipal.java
        │   │   │   ├── JwtTokenProvider.java
        │   │   │   ├── JwtAuthenticationFilter.java
        │   │   │   ├── JwtAuthenticationEntryPoint.java
        │   │   │   ├── CustomAccessDeniedHandler.java
        │   │   │   ├── SecurityUtils.java
        │   │   │   └── CommonSecurityConfig.java
        │   │   ├── idempotency/        # Quản trị khóa bất biến chống trùng lặp request (TH6)
        │   │   │   ├── entity/IdempotencyRecord.java
        │   │   │   ├── repository/IdempotencyRecordRepository.java
        │   │   │   └── service/IdempotencyService.java
        │   │   ├── consistency/        # Nhất quán dữ liệu phân tán & State Machine (TH6)
        │   │   │   ├── BookingStateTransitionValidator.java
        │   │   │   └── DistributedBookingSagaOrchestrator.java
        │   │   ├── concurrency/        # Khóa phân tán chống tranh chấp lịch đa instance (TH7)
        │   │   │   └── DistributedConcurrencyLockService.java
        │   │   ├── discovery/          # Chuẩn hóa Service Discovery Eureka & Tracking Header (TH7)
        │   │   │   ├── ServiceDiscoveryProperties.java
        │   │   │   ├── InstanceMetadataProvider.java
        │   │   │   └── InstanceTrackingFilter.java
        │   │   ├── dto/integration/    # Hợp đồng DTO tích hợp liên dịch vụ (TH6)
        │   │   └── controller/         # Controller mẫu minh họa REST, Paging, Security, Saga & Discovery (TH3, TH5, TH6, TH7)
        │   │       ├── SampleRestValidationController.java
        │   │       ├── SampleSecuredController.java
        │   │       ├── SampleIntegrationController.java
        │   │       └── SampleDiscoveryController.java
        │   └── resources/
        │       ├── application.properties
        │       └── application-eureka.yml
        └── test/
            ├── java/com/football/data/
            │   ├── auth/UserRepositoryTest.java
            │   ├── pitch/PitchRepositoryTest.java
            │   ├── booking/BookingRepositoryTest.java
            │   │   └── service/BookingTransactionalServiceTest.java  # Kiểm thử @Transactional Rollback/Commit (TH4)
            │   ├── payment/PaymentRepositoryTest.java                # Kiểm thử CSDL payment_db (TH4)
            │   ├── mapper/             # Unit tests cho các mapper
            │   ├── dto/                # Serialization & Tolerant Reader Contract tests (TH2, TH6)
            │   ├── controller/         # MockMvc Integration tests cho GlobalExceptionHandler & Paging (TH3)
            │   ├── security/           # Unit & Integration tests cho JWT, SecurityUtils, RBAC (TH5)
            │   ├── idempotency/        # Kiểm thử tính bất biến Idempotency (TH6)
            │   ├── consistency/        # Kiểm thử Saga Orchestrator & State Machine (TH6)
            │   ├── concurrency/        # Kiểm thử Race Condition tranh chấp đa instance (TH7)
            │   └── discovery/          # Kiểm thử Discovery Properties & Tracking Filter (TH7)
            └── resources/
                └── application.properties
```

---

## 3. HƯỚNG DẪN CHẠY KIỂM THỬ TỰ ĐỘNG

Dự án đã được cấu hình profile kiểm thử in-memory với CSDL H2 (tự động mô phỏng ngữ pháp MySQL), không yêu cầu phải cài đặt MySQL trước khi chạy test.

```bash
# Di chuyển vào thư mục data-layer
cd data-layer

# Chạy toàn bộ 80 bài kiểm thử đơn vị & tích hợp (TH1 + TH2 + TH3 + TH4 + TH5 + TH6 + TH7)
mvn clean test
```

### Kết quả kiểm thử mẫu:
```text
[INFO] Running com.football.data.auth.UserRepositoryTest (5 tests)
[INFO] Running com.football.data.pitch.PitchRepositoryTest (3 tests)
[INFO] Running com.football.data.payment.PaymentRepositoryTest (1 test)
[INFO] Running com.football.data.booking.BookingRepositoryTest (10 tests)
[INFO] Running com.football.data.booking.service.BookingTransactionalServiceTest (5 tests)
[INFO] Running com.football.data.controller.GlobalExceptionHandlerIntegrationTest (9 tests)
[INFO] Running com.football.data.dto.InterServiceSerializationContractTest (4 tests)
[INFO] Running com.football.data.dto.IntegrationContractCompatibilityTest (4 tests)
[INFO] Running com.football.data.mapper.BookingMapperTest (2 tests)
[INFO] Running com.football.data.mapper.PitchMapperTest (1 test)
[INFO] Running com.football.data.mapper.UserMapperTest (1 test)
[INFO] Running com.football.data.security.JwtTokenProviderTest (5 tests)
[INFO] Running com.football.data.security.SecurityUtilsTest (2 tests)
[INFO] Running com.football.data.security.CommonSecurityIntegrationTest (8 tests)
[INFO] Running com.football.data.idempotency.service.IdempotencyServiceTest (5 tests)
[INFO] Running com.football.data.consistency.BookingStateTransitionValidatorTest (3 tests)
[INFO] Running com.football.data.consistency.DistributedBookingSagaOrchestratorTest (4 tests)
[INFO] Running com.football.data.discovery.ServiceDiscoveryPropertiesTest (2 tests)
[INFO] Running com.football.data.discovery.InstanceMetadataProviderTest (3 tests)
[INFO] Running com.football.data.discovery.InstanceTrackingFilterIntegrationTest (2 tests)
[INFO] Running com.football.data.concurrency.MultiInstanceConcurrencyRaceConditionTest (1 test)
[INFO] 
[INFO] Results:
[INFO] Tests run: 80, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 4. HƯỚNG DẪN NẠP CƠ SỞ DỮ LIỆU MYSQL (KHI CHẠY THẬT)

1. Mở công cụ quản trị MySQL (**phpMyAdmin, DBeaver, MySQL Workbench, hoặc CLI**).
2. Mở file `database/init_all_databases.sql`.
3. Bấm **Execute** (hoặc chạy lệnh: `mysql -u root -p < database/init_all_databases.sql`).
4. Hệ thống sẽ tự động khởi tạo 4 cơ sở dữ liệu độc lập:
   - `auth_db`: Gồm bảng `users` cùng 4 tài khoản mẫu.
   - `pitch_db`: Gồm bảng `pitches`, `golden_hour_prices` cùng 4 sân bóng mẫu.
   - `booking_db`: Gồm bảng `bookings` và `idempotency_keys` cùng dữ liệu mẫu.
   - `payment_db`: Gồm bảng `payments` cùng 3 giao dịch mẫu.

---

## 5. DANH SÁCH BÁO CÁO KỸ THUẬT CỦA HÙNG (TH1 - TH7)

1. [BAO_CAO_TH1_HUNG.md](docs/BAO_CAO_TH1_HUNG.md): Thiết kế ERD Database-per-Service, JPA Entity & Repository, Thuật toán chống trùng lịch toán học.
2. [BAO_CAO_TH2_HUNG.md](docs/BAO_CAO_TH2_HUNG.md): Chuẩn hóa Hợp đồng DTO, Giải quyết Anti-pattern Shared Entity, Mappers & Jackson Tolerant Reader.
3. [BAO_CAO_TH3_HUNG.md](docs/BAO_CAO_TH3_HUNG.md): Response Envelopes (`ApiResponse`, `PageResponse`), RFC 7807 `ErrorResponse`, `GlobalExceptionHandler` 11 mã lỗi.
4. [BAO_CAO_TH4_HUNG.md](docs/BAO_CAO_TH4_HUNG.md): Database-per-Service 4 CSDL độc lập, Quản lý Transaction phân tán `@Transactional` Commit & Rollback.
5. [BAO_CAO_TH5_HUNG.md](docs/BAO_CAO_TH5_HUNG.md): Chuẩn Common Security dùng chung, Stateless JWT JJWT 0.12.6, UserPrincipal nạp từ Claims, Phân quyền RBAC.
6. [BAO_CAO_TH6_HUNG.md](docs/BAO_CAO_TH6_HUNG.md): Hợp đồng DTO tích hợp, Bảng `idempotency_keys` & `IdempotencyService`, Finite State Machine Validator, Distributed Saga Orchestrator & Bù trừ tự động.
7. [BAO_CAO_TH7_HUNG.md](docs/BAO_CAO_TH7_HUNG.md): Chuẩn hóa Service Discovery, Multi-instance configuration, Distributed Concurrency Lock, Header `X-Served-By-Instance`, Docker Compose đa node.

---

## 6. THÔNG TIN LIÊN HỆ & BẢN QUYỀN
- **Tác giả:** Vũ Đức Hùng (`viduchung`)
- **Đồ án:** Thực hành Phát triển phần mềm hướng dịch vụ (TH-PTPM)
- **Repository:** https://github.com/viduchung/TH-PTPM.git

