# Booking Service — Nhật (TH1–TH7)

Phần đóng góp của **Nhật** (đưa lên nhánh riêng của Nhật trong repository chung của nhóm): **Booking Service** do Nhật thực hiện trong dự án PitchOS (hệ thống đặt sân bóng đá mini, kiến trúc microservices), cùng tài liệu và bằng chứng kiểm thử cho các buổi thực hành TH1–TH7.

> Phần này **không** chứa mã nguồn Auth Service, Pitch Service hay script CSDL chung của các thành viên khác. Booking **không chạy được độc lập hoàn toàn**: khi chạy thật, nó cần MySQL, Pitch Service (qua HTTP) và token do Auth Service phát hành. Riêng **bộ test tự động chạy độc lập** (H2 trong bộ nhớ và Pitch giả lập).

## 1. Phạm vi TH1–TH7

| Buổi | Nội dung | Trạng thái |
|---|---|---|
| TH1 | CRUD Booking: đặt, xem, sửa, hủy (hủy mềm), duyệt, từ chối | DONE |
| TH2 | Gọi Pitch Service bằng WebClient; timeout; xử lý 404/500/503; giữ OpenFeign cũ | DONE |
| TH3 | Phân trang, sắp xếp, lọc (trạng thái, sân, ngày); validation; lỗi JSON thống nhất | DONE |
| TH4 | JPA/MySQL: transaction, khóa theo sân chống đặt trùng, index, khóa lạc quan | PARTIAL: đã kiểm thử trên **MySQL 8.4.3** thử nghiệm (87/87 test, 2 instance cùng CSDL, V2 chạy lại an toàn); **chưa** kiểm thử MySQL 8.0 / MySQL của nhóm, chờ Hùng xác nhận tên CSDL và quy trình migration |
| TH5 | JWT, phân quyền `user`/`chusan`/`admin`, quyền sở hữu đơn | PARTIAL: chính sách chủ sân đã **fail-closed** và có test, nhưng chưa xác minh với Pitch thật (Pitch chưa trả `ownerId` → chủ sân bị chặn) |
| TH6 | Consumer/Provider, retry giới hạn, báo giá, Idempotency-Key, request đồng thời | DONE (phía Booking) |
| TH7 | Eureka client, gọi Pitch theo tên dịch vụ, Spring Cloud LoadBalancer | PARTIAL: cần Eureka Server của nhóm và Pitch có Eureka client |

Chi tiết và bằng chứng: [`docs/BAO_CAO_HOAN_THANH_NHAT_TH1_TH7.md`](docs/BAO_CAO_HOAN_THANH_NHAT_TH1_TH7.md). Hợp đồng API với Auth, Pitch, MySQL, Eureka: [`docs/HOP_DONG_API_TICH_HOP.md`](docs/HOP_DONG_API_TICH_HOP.md). Báo cáo đóng gói: [`docs/BAO_CAO_DONG_GOI_NHIEM_VU_NHAT.md`](docs/BAO_CAO_DONG_GOI_NHIEM_VU_NHAT.md).

## 2. Công nghệ

Java 17 (đã kiểm thử bằng JDK 21) · Spring Boot 3.2.5 · Spring Web MVC · Spring Data JPA (Hibernate 6) · Spring Security + JJWT 0.11.5 · Spring WebFlux `WebClient` · Spring Cloud 2023.0.1 (OpenFeign, Netflix Eureka Client, LoadBalancer) · MySQL 8 · Lombok · Maven · JUnit 5, Spring Boot Test, H2 (chỉ dùng cho test).

## 3. Cấu trúc

```text
.
├── pitchos-booking-service/
│   ├── src/main/java/com/pitchos/booking/   controller, service, client (Pitch), security (JWT), entity, repository, dto, exception
│   ├── src/test/                            87 test (unit + tích hợp, H2 + Pitch giả lập)
│   ├── db/V2__booking_th1_th7_mysql.sql     nâng cấp schema cho CSDL tạo từ script chung của nhóm
│   ├── demo/MockPitchService.java           Pitch Service giả lập (chạy bằng `java`, không cần Maven)
│   ├── postman/                             collection kiểm thử thủ công
│   ├── pom.xml
│   └── README.md                            đặc tả API và kịch bản demo
├── docs/                                    báo cáo TH1–TH7, hợp đồng API tích hợp, bằng chứng kiểm thử
├── .env.example                             biến môi trường mẫu (placeholder)
└── .gitignore
```

## 4. Yêu cầu môi trường

- JDK 17 trở lên, Maven 3.9+ (Maven tải dependency từ Maven Central, **không** có module nội bộ).
- Chỉ để chạy test: không cần gì thêm.
- Để chạy thật: MySQL 8, Pitch Service (HTTP `GET /api/v1/pitches/{id}`) và Auth Service (để lấy token).
- TH7 (tùy chọn): Eureka Server (mặc định `http://localhost:8761/eureka/`).

## 5. Cấu hình biến môi trường

```bash
cp .env.example .env          # Windows: copy .env.example .env
# Sửa .env: MYSQL_USERNAME, MYSQL_PASSWORD, JWT_SECRET (BẮT BUỘC, trùng với Auth Service), PITCH_SERVICE_URL...
```

- Booking tự đọc `.env` (ở gốc repo hoặc thư mục service) khi khởi động. Biến môi trường của hệ điều hành được ưu tiên hơn.
- `JWT_SECRET` **không có giá trị mặc định**: thiếu thì ứng dụng dừng khi khởi động.
- Không commit `.env`; nó đã có trong `.gitignore`.
- `BOOKING_CHUSAN_WITHOUT_OWNER_INFO` mặc định `false` (an toàn). Giá trị `true` là **chế độ tương thích không an toàn** (mọi chủ sân thao tác mọi đơn khi Pitch chưa có `ownerId`): chỉ dùng khi demo cục bộ với Pitch cũ / giả lập, **không** dùng trong tích hợp thật.
- Múi giờ nghiệp vụ cố định `Asia/Ho_Chi_Minh` (không phụ thuộc múi giờ máy hay container).

## 6. Build và test

```bash
cd pitchos-booking-service
mvn clean package        # biên dịch + chạy 87 test + tạo target/pitchos-booking-service-0.0.1-SNAPSHOT.jar
mvn test                 # chỉ chạy test
mvn test -Dtest=Th6IdempotencyConcurrencyIntegrationTest   # chạy một lớp test
```

Bộ test mặc định dùng H2 (chế độ MySQL; riêng `Th4BookingRepositoryJpaTest` dùng H2 nhúng của `@DataJpaTest`), một Pitch Service giả lập là HTTP server thật trong JDK, và secret JWT riêng cho test. Toàn bộ 87 test cũng đã chạy **đạt trên MySQL 8.4.3** thử nghiệm bằng cách trỏ cấu hình test sang MySQL (cách làm và log: `docs/bang-chung-kiem-thu/th4-mysql-8.4.3.txt`). Test kiểm tra index/khóa ngoại dùng JDBC `DatabaseMetaData` nên chạy được trên cả H2 và MySQL.

## 7. Chạy Booking Service

1. **CSDL MySQL** (chỉ CSDL của Booking):
   - CSDL mới: `CREATE DATABASE pitchos_booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`. Hibernate (`ddl-auto=update`) tự tạo bảng `bookings` và `pitch_locks`.
   - CSDL đã tạo từ script chung của nhóm: chạy V2 **trước** khi khởi động (thêm trạng thái `da_huy`, các cột, index và bảng mới). **Bắt buộc**: nếu thiếu `da_huy`, hủy đơn sẽ lỗi 500.
     ```bash
     # <ten_csdl_booking> do người chạy chỉ định (script không cố định tên CSDL); KHÔNG dùng --force
     mysql -u <user> -p --default-character-set=utf8mb4 <ten_csdl_booking> < pitchos-booking-service/db/V2__booking_th1_th7_mysql.sql
     ```
     V2 **chạy lại an toàn** (phần đã có thì bỏ qua), kiểm tra cấu trúc trước khi sửa và dừng với mã lỗi `V2_LOI_…` nếu không tương thích; không xóa dữ liệu.
   - Cả hai cách đã kiểm thử trên **MySQL 8.4.3** thử nghiệm; **chưa** kiểm thử trên MySQL 8.0 và MySQL của nhóm.
2. **Pitch Service** chạy ở `PITCH_SERVICE_URL` (mặc định `http://localhost:8082`). Khi chưa có, có thể tập dượt bằng Pitch giả lập:
   ```bash
   java pitchos-booking-service/demo/MockPitchService.java      # cổng 8082
   ```
3. **Booking:**
   ```bash
   cd pitchos-booking-service
   mvn spring-boot:run                                          # hoặc: java -jar target/pitchos-booking-service-0.0.1-SNAPSHOT.jar
   ```
   PowerShell, ví dụ đổi cổng: `$env:BOOKING_SERVICE_PORT="8093"; mvn spring-boot:run`

## 8. Kết nối các service khác (hợp đồng giữ nguyên, chi tiết ở `docs/HOP_DONG_API_TICH_HOP.md`)

| Service | Cách Booking dùng | Cấu hình |
|---|---|---|
| Auth Service | Không gọi HTTP. Chỉ kiểm tra chữ ký JWT (HS256) do Auth phát. Token cần claim `userId` (số) và `role` ∈ `user`/`chusan`/`admin` | `JWT_SECRET` trùng Auth |
| Pitch Service | `GET /api/v1/pitches/{id}`, cần JSON có `id`, `name`, `pricePerHour`, `status` (`hoat_dong`/`bao_tri`), tùy chọn `ownerId` | `PITCH_SERVICE_URL` hoặc Eureka (`PITCH_DISCOVERY_ENABLED=true`, `PITCH_SERVICE_ID`) |
| MySQL | CSDL riêng `pitchos_booking_db`, không có khóa ngoại sang CSDL khác | `MYSQL_*`, `BOOKING_DB_NAME` |
| Eureka Server | Đăng ký `pitchos-booking-service`, tra cứu `pitchos-pitch-service` | `EUREKA_ENABLED=true`, `EUREKA_URL` |

Lỗi khi gọi Pitch: sân không tồn tại → 404; Pitch tắt, timeout, 5xx hoặc dữ liệu sai → 503 (không tạo đơn); sân bảo trì → 400.

## 9. Kiểm thử bằng Postman

Import `pitchos-booking-service/postman/PitchOS-Booking.postman_collection.json`:

1. Đặt biến `bookingUrl`, `authUrl`, `demoPassword` (mật khẩu tài khoản demo của Auth, **không** lưu vào repo).
2. Chạy thư mục **0** để tự lấy token. Collection không chứa token còn hiệu lực; `tokenExpired` là token đã hết hạn, chỉ dùng cho ca 401.
3. Thư mục **1–3**: kịch bản demo và kiểm thử cũ (thư mục 3 cần tắt Pitch). Thư mục **4**: các ca TH1–TH7.
4. Các request dùng token chủ sân (ca 14, "TH5 - Chủ sân xem chi tiết đơn") trả **403** khi Pitch chưa trả `ownerId` (fail-closed) và 200 khi Pitch trả `ownerId` trùng chủ sân.

API chính (đầy đủ ở `pitchos-booking-service/README.md` mục 5 và 7):
- `POST /api/v1/bookings`, có header `Idempotency-Key` tùy chọn
- `GET /api/v1/bookings/my-bookings`
- `GET /api/v1/bookings?status=&pitchId=&date=&page=&size=&sort=`
- `GET|PUT|DELETE /api/v1/bookings/{id}`
- `PATCH /api/v1/bookings/{id}/cancel`
- `PATCH /api/v1/bookings/{id}/status`
- `GET /api/v1/bookings/quote`

## 10. Giới hạn hiện tại

- **TH4:** đã kiểm thử thực tế trên **MySQL 8.4.3** (instance thử nghiệm riêng): 87/87 test; 2 instance Booking cùng CSDL (20 request đồng thời → 1 đơn); `SELECT … FOR UPDATE` thật (request chờ khóa 3 giây); rollback khi vi phạm UNIQUE; V2 chạy 3 lần liên tiếp không lỗi, dữ liệu giữ nguyên. **Chưa** kiểm thử trên MySQL 8.0 và MySQL của nhóm. Chờ Hùng: tên CSDL (`pitchos_booking_db` hay `booking_db`), quy trình migration (script chung hay V2, có dùng Flyway/Liquibase không), `ddl-auto`, user và quyền CSDL, SSL.
- **TH5:** chính sách chủ sân là **fail-closed** (mặc định `BOOKING_CHUSAN_WITHOUT_OWNER_INFO=false`): chủ sân chỉ xem / duyệt / từ chối đơn khi Pitch trả `ownerId` trùng `userId` trong token; Pitch lỗi hoặc không trả `ownerId` → không cấp quyền. Pitch hiện **chưa trả `ownerId`** nên trong tích hợp thật chủ sân đang bị chặn (chỉ admin duyệt được), và `GET /api/v1/bookings` của chủ sân luôn 403 vì chưa có API lấy danh sách sân theo chủ sở hữu. Chưa xác minh quyền sở hữu với Pitch/Auth thật.
- **TH7:** đã kiểm chứng với một Eureka Server **tạm** và 2 instance Pitch (Pitch được đăng ký thủ công qua REST API của Eureka). Nhóm chưa có Eureka Server chung; Pitch chưa có Eureka client. Chế độ OpenFeign chỉ gọi URL cố định.
- Port (8083 hay 8081) và tên CSDL (`pitchos_booking_db` hay `booking_db`) chờ nhóm thống nhất, đổi được qua biến môi trường.
