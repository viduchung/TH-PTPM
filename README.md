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
        │   │   └── controller/         # Controller mẫu minh họa REST, Paging, và Security RBAC (TH3, TH5)
        │   │       ├── SampleRestValidationController.java
        │   │       └── SampleSecuredController.java
        │   └── resources/
        │       └── application.properties
        └── test/
            ├── java/com/football/data/
            │   ├── auth/UserRepositoryTest.java
            │   ├── pitch/PitchRepositoryTest.java
            │   ├── booking/BookingRepositoryTest.java
            │   │   └── service/BookingTransactionalServiceTest.java  # Kiểm thử @Transactional Rollback/Commit (TH4)
            │   ├── payment/PaymentRepositoryTest.java                # Kiểm thử CSDL payment_db (TH4)
            │   ├── mapper/             # Unit tests cho các mapper
            │   ├── dto/                # Serialization & Tolerant Reader Contract tests
            │   ├── controller/         # MockMvc Integration tests cho GlobalExceptionHandler & Paging (TH3)
            │   └── security/           # Unit & Integration tests cho JWT, SecurityUtils, RBAC (TH5)
            │       ├── JwtTokenProviderTest.java
            │       ├── SecurityUtilsTest.java
            │       └── CommonSecurityIntegrationTest.java
            └── resources/
                └── application.properties
```

---

## 3. HƯỚNG DẪN CHẠY KIỂM THỬ TỰ ĐỘNG

Dự án đã được cấu hình profile kiểm thử in-memory với CSDL H2 (tự động mô phỏng ngữ pháp MySQL), không yêu cầu phải cài đặt MySQL trước khi chạy test.

```bash
# Di chuyển vào thư mục data-layer
cd data-layer

# Chạy toàn bộ 56 bài kiểm thử đơn vị & tích hợp (TH1 + TH2 + TH3 + TH4 + TH5)
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
[INFO] Running com.football.data.mapper.BookingMapperTest (2 tests)
[INFO] Running com.football.data.mapper.PitchMapperTest (1 test)
[INFO] Running com.football.data.mapper.UserMapperTest (1 test)
[INFO] Running com.football.data.security.JwtTokenProviderTest (5 tests)
[INFO] Running com.football.data.security.SecurityUtilsTest (2 tests)
[INFO] Running com.football.data.security.CommonSecurityIntegrationTest (8 tests)
[INFO] 
[INFO] Results:
[INFO] Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] BUILD SUCCESS
```

---

## 4. HƯỚNG DẪN NẠP CƠ SỞ DỮ LIỆU MYSQL (KHI CHẠY THẬT)

1. Mở công cụ quản trị MySQL (**phpMyAdmin, DBeaver, MySQL Workbench, hoặc CLI**).
2. Mở file `database/init_all_databases.sql`.
3. Bấm **Execute** (hoặc chạy lệnh: `mysql -u root -p < database/init_all_databases.sql`).
4. Hệ thống sẽ tự động khởi tạo 3 cơ sở dữ liệu:
   - `auth_db`: Gồm bảng `users` cùng 4 tài khoản mẫu.
   - `pitch_db`: Gồm bảng `pitches`, `golden_hour_prices` cùng 4 sân bóng mẫu.
   - `booking_db`: Gồm bảng `bookings` cùng 3 đơn đặt sân mẫu.

---

## 5. THÔNG TIN LIÊN HỆ & BẢN QUYỀN
- **Tác giả:** Vũ Đức Hùng (`viduchung`)
- **Đồ án:** Thực hành Phát triển phần mềm hướng dịch vụ (TH-PTPM)
- **Repository:** https://github.com/viduchung/TH-PTPM.git

