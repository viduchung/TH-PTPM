# TẦNG DỮ LIỆU PHÂN TÁN (DATA LAYER) — HỆ THỐNG QUẢN LÝ ĐẶT LỊCH SÂN BÓNG ĐÁ

> **Học phần:** Phát triển phần mềm hướng dịch vụ (SOA)  
> **Người thực hiện:** VŨ ĐỨC HÙNG  
> **Tài khoản Git:** `viduchung`  
> **Nhánh Git:** `feat/hung-data`  
> **Vai trò:** Database Administrator & Backend Data Engineer (Phụ trách toàn bộ mảng dữ liệu & CSDL phân tán)  
> **Nội dung hoàn thành:** **TUẦN 1 (TH1) — Thiết kế @Entity + Repository cho User/Booking/Pitch; Dựng ERD**

---

## 1. TỔNG QUAN NHIỆM VỤ TUẦN 1 CỦA HÙNG (TH1)

Theo bảng phân công nhóm (`BANG_PHAN_CONG_HANG_TUAN.xlsx`), Hùng phụ trách tầng dữ liệu xuyên suốt dự án. Trong buổi TH1, Hùng đã hoàn thành xuất sắc các nội dung:

1. **Thiết kế Kiến trúc CSDL Phân tán (Database-per-Service):**
   - Phân chia độc lập 3 CSDL vật lý: `auth_db`, `pitch_db`, `booking_db`.
   - Áp dụng triệt để giải pháp **Khóa Logic (Logical ID)** `user_id` và `pitch_id`, **tuyệt đối không tạo Foreign Key vật lý** giữa các database để tránh Schema Coupling và Deadlock chéo.
2. **Dựng Sơ đồ Thực thể Quan hệ (ERD):**
   - Xây dựng sơ đồ ERD tổng thể và chi tiết từng dịch vụ bằng định dạng Mermaid trực quan.
   - Biên soạn tài liệu phân tích ranh giới dữ liệu tại `docs/ERD_DESIGN_EXPLANATION.md`.
3. **Xây dựng Module Mô hình Thực thể (@Entity) & Repository (@Repository):**
   - Đặt trong module Maven độc lập `data-layer`.
   - Các Entity: `User`, `Role` (`auth_db`); `Pitch`, `PitchType`, `PitchStatus`, `GoldenHourPrice` (`pitch_db`); `Booking`, `BookingStatus` (`booking_db`).
   - Các Repository: `UserRepository`, `PitchRepository`, `GoldenHourPriceRepository`, `BookingRepository`.
4. **Giải thuật Chống Trùng Lịch Thi Đấu Độc Quyền:**
   - Cài đặt truy vấn JPQL `countOverlappingBookings` trong `BookingRepository` với logic toán học giao thoa thời gian `b.startTime < :endTime AND b.endTime > :startTime`.
   - Tối ưu hóa chỉ mục tổ hợp `(pitch_id, start_time, end_time)` cho tốc độ truy vấn $O(\log N)$.
   - Bỏ qua các đơn có trạng thái `TU_CHOI` hoặc `DA_HUY`.
5. **Bộ Tập Lệnh DDL Khởi Tạo CSDL MySQL:**
   - `database/01_auth_db_init.sql` (auth_db)
   - `database/02_pitch_db_init.sql` (pitch_db)
   - `database/03_booking_db_init.sql` (booking_db)
   - `database/init_all_databases.sql` (Tổng hợp khởi tạo toàn bộ CSDL chỉ với 1 cú click)
6. **Kiểm Thử Tự Động Toàn Diện:**
   - 17/17 Unit & Integration Test Cases ĐẠT (BUILD SUCCESS) trên cơ sở dữ liệu in-memory H2 (chế độ tương thích MySQL).

---

## 2. CẤU TRÚC THƯ MỤC DỰ ÁN

```text
TH-PTPM/
├── BANG_PHAN_CONG_HANG_TUAN.xlsx       # Bảng phân công nhiệm vụ chi tiết nhóm TH1 - TH7
├── README.md                           # Hướng dẫn tổng quan & kết quả tuần 1 của Hùng
├── database/                           # Bộ script SQL khởi tạo CSDL theo chuẩn Database-per-Service
│   ├── 01_auth_db_init.sql             # Khởi tạo auth_db và bảng users (Hoàng - Port 8080)
│   ├── 02_pitch_db_init.sql            # Khởi tạo pitch_db và bảng pitches, golden_hour_prices (Linh - Port 8082)
│   ├── 03_booking_db_init.sql          # Khởi tạo booking_db và bảng bookings (Nhật - Port 8081/8083)
│   └── init_all_databases.sql          # Script gộp khởi tạo toàn bộ 3 database trên MySQL / phpMyAdmin
├── docs/                               # Tài liệu thiết kế & báo cáo kỹ thuật của Hùng
│   ├── BAO_CAO_TH1_HUNG.md             # Báo cáo kỹ thuật chi tiết bài thực hành Tuần 1 của Hùng
│   ├── ERD_DESIGN_EXPLANATION.md       # Phân tích chuyên sâu thiết kế ERD & ranh giới dữ liệu
│   └── diagrams/                       # Mã nguồn sơ đồ thực thể Mermaid (.mmd)
│       ├── erd_overview.mmd            # Sơ đồ ERD tổng thể hệ thống
│       ├── erd_auth_service.mmd        # Sơ đồ ERD auth_db
│       ├── erd_pitch_service.mmd       # Sơ đồ ERD pitch_db
│       └── erd_booking_service.mmd     # Sơ đồ ERD booking_db
└── data-layer/                         # Module Java Spring Boot JPA & Repository độc lập
    ├── pom.xml                         # Cấu hình Maven (Java 17, Spring Boot 3.2.5, JPA, H2, Lombok)
    └── src/
        ├── main/
        │   ├── java/com/football/data/
        │   │   ├── FootballDataApplication.java
        │   │   ├── auth/               # Miền dữ liệu auth_db (User, Role, UserRepository)
        │   │   ├── pitch/              # Miền dữ liệu pitch_db (Pitch, PitchType, PitchStatus, GoldenHourPrice,...)
        │   │   └── booking/            # Miền dữ liệu booking_db (Booking, BookingStatus, BookingRepository)
        │   └── resources/
        │       └── application.properties
        └── test/
            ├── java/com/football/data/
            │   ├── auth/UserRepositoryTest.java
            │   ├── pitch/PitchRepositoryTest.java
            │   └── booking/BookingRepositoryTest.java   # 11 kịch bản test thuật toán chống trùng lịch
            └── resources/
                └── application.properties
```

---

## 3. HƯỚNG DẪN CHẠY KIỂM THỬ TỰ ĐỘNG

Dự án đã được cấu hình profile kiểm thử in-memory với CSDL H2 (tự động mô phỏng ngữ pháp MySQL), không yêu cầu phải cài đặt MySQL trước khi chạy test.

```bash
# Di chuyển vào thư mục data-layer
cd data-layer

# Chạy toàn bộ 17 bài kiểm thử đơn vị & tích hợp
mvn clean test
```

### Kết quả kiểm thử mẫu:
```text
[INFO] Running com.football.data.auth.UserRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.football.data.booking.BookingRepositoryTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.football.data.pitch.PitchRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results:
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
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

