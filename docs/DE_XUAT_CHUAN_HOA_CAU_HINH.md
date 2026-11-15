# ĐỀ XUẤT CHUẨN HÓA CẤU HÌNH BOOKING SERVICE

> Ngày: 16/11/2026 · Phạm vi: chỉ Booking Service (`C:\nhom13`) · Giai đoạn này **chỉ phân tích và đề xuất**, không sửa code chính hay cấu hình.
> Bằng chứng thử nghiệm: `bang-chung-kiem-thu/chuan-hoa-cau-hinh-mysql-8.4.3.txt`.

## 0. Các quyết định đã chốt

| Quyết định | Hiện trạng trong code | Khớp? |
|---|---|---|
| CSDL mục tiêu `booking_db` | Mặc định `pitchos_booking_db`; đổi được qua `BOOKING_DB_NAME` | **Chưa khớp** (cần đổi mặc định) |
| Tương thích MySQL 8.0 | Mới kiểm thử trên **MySQL 8.4.3** | **Chưa có bằng chứng** trên 8.0 |
| Booking tự quản lý migration V2 | `db/V2__booking_th1_th7_mysql.sql` chạy lại an toàn, không cố định tên CSDL | Khớp (nhưng còn thiếu script V1, xem mục 3) |
| Hướng tới `ddl-auto=validate` | Đang là `update` | **Chưa khớp** (cần chuẩn bị trước khi đổi) |
| Không dùng Flyway / Liquibase | Không dùng | Khớp |
| Giữ fail-closed | `BOOKING_CHUSAN_WITHOUT_OWNER_INFO=false` | Khớp |
| WebClient mặc định | `PITCH_CLIENT_TYPE=webclient` | Khớp |
| Eureka là mục tiêu tích hợp | Có Eureka client, mặc định **tắt** (`EUREKA_ENABLED=false`) | Khớp về cơ chế; chờ địa chỉ Eureka và tên service của nhóm |

## 1. Tương thích MySQL 8.0

Máy hiện chỉ có MySQL **8.4.3** và **26.7**, **không có MySQL 8.0**. Mọi đánh giá dưới đây về 8.0 dựa trên tài liệu, **chưa chạy thật**.

| Thành phần | Cú pháp / tính năng | Bằng chứng hiện có |
|---|---|---|
| V2 | `SET @x := …`, `PREPARE` / `EXECUTE` / `DEALLOCATE PREPARE` | Chạy đúng trên 8.4.3, không cảnh báo |
| V2 | SQL động với `ALTER TABLE … MODIFY ENUM`, `ADD COLUMN`, `CREATE INDEX`, `ADD CONSTRAINT … UNIQUE` | Chạy đúng trên 8.4.3 |
| V2 | Truy vấn `information_schema.columns / statistics / tables`, `GROUP_CONCAT(… ORDER BY …)`, `DATABASE()` | Chạy đúng trên 8.4.3 |
| V2 | `SELECT COUNT(*) INTO @v2_dup FROM (…)` (`INTO` đặt trước `FROM`) | 8.4.3 **không cảnh báo** (`--show-warnings`). Chưa kiểm tra trên 8.0 |
| V2 | `CREATE TABLE IF NOT EXISTS … ENGINE = InnoDB` | 8.4.3 chỉ báo `Note 1050` (bảng đã có, bình thường) |
| V2 | Thêm cột vào bảng có dữ liệu (thuật toán do MySQL tự chọn) | Chạy đúng trên 8.4.3. Các bản 8.0 cũ có thể dùng thuật toán chậm hơn với bảng lớn |
| V2 | Tên bảng phân biệt hoa thường (`lower_case_table_names=0` trên Linux) | Mới chạy trên Windows (`=1`). Tên trong script đều viết thường nên dự kiến không ảnh hưởng, nhưng chưa có bằng chứng |
| Ứng dụng | Hibernate 6.4 `MySQLDialect`, `mysql-connector-j 8.3.0`, `SELECT … FOR UPDATE`, `READ_COMMITTED`, ENUM | Chạy đúng trên 8.4.3 (87/87 test, 2 instance). Chưa kiểm tra trên 8.0 |

**Kết luận:** chưa thấy cú pháp nào chắc chắn không chạy trên 8.0, nhưng **toàn bộ chưa có bằng chứng trên 8.0**. Muốn có bằng chứng, cần một MySQL 8.0 thử nghiệm riêng: `docker run mysql:8.0` hoặc bản ZIP MySQL 8.0 portable. **Việc tải hoặc cài cần bạn đồng ý.** Sau đó chạy lại đúng các kịch bản ở `bang-chung-kiem-thu/th4-mysql-8.4.3.txt`.

## 2. Chuyển sang `ddl-auto=validate`

Thử trên MySQL 8.4.3, chỉ ghi đè bằng tham số dòng lệnh `--spring.jpa.hibernate.ddl-auto=validate`, **không sửa file cấu hình**:

| CSDL | Kết quả |
|---|---|
| `booking_db` = script chung + V2 | **Khởi động được** |
| Do Hibernate tạo | **Khởi động được** |
| Script chung, **chưa chạy V2** | **Không khởi động**: `Schema-validation: missing column [idempotency_key]`. Tốt hơn hiện nay: lỗi lộ ra ngay khi khởi động thay vì hủy đơn trả 500 |
| CSDL **rỗng** | **Không khởi động**: `missing table [bookings]` |
| Đủ cột nhưng ENUM **thiếu `da_huy`** | **Khởi động được**: `validate` **không** kiểm tra danh sách giá trị ENUM, nên hủy đơn vẫn có thể trả 500 |

**Bộ test không bị ảnh hưởng:** `src/test/resources/application-test.properties` đặt `ddl-auto=create-drop` và được nạp bằng `@TestPropertySource` (độ ưu tiên cao hơn `application.properties`).

**Điều kiện trước khi đổi sang `validate`:**
1. Có **script tạo bảng ban đầu (V1) do Booking quản lý** cho CSDL mới. Với `validate`, Hibernate không còn tự tạo bảng; cách hiện tại "`CREATE DATABASE` rồi để Hibernate tạo bảng" sẽ ngừng hoạt động.
2. **V2 vẫn bắt buộc**, vì `validate` không phát hiện ENUM thiếu `da_huy`. Nên cân nhắc thêm kiểm tra ENUM lúc khởi động (`SchemaCompatibilityCheck`, đã hoãn theo yêu cầu).
3. Cập nhật README và quy trình triển khai theo thứ tự: V1 (nếu là CSDL mới) → V2 → khởi động.

Có thể bật `validate` mà không sửa file, bằng biến môi trường `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (cơ chế ghi đè chuẩn của Spring Boot). Tuy nhiên đợt thử nghiệm mới dùng **tham số dòng lệnh**; cách dùng biến môi trường chưa chạy thử.

## 3. CSDL `booking_db` và cách truyền tên

- Chuỗi kết nối: `jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${BOOKING_DB_NAME:pitchos_booking_db}?…`. **Đã chứng minh** đổi CSDL qua `BOOKING_DB_NAME` hoạt động: ứng dụng đã kết nối các CSDL `booking_db`, `r2_s5`, `ch_team_only`… chỉ bằng biến này.
- V2 không cố định tên CSDL: đã chạy trên `booking_db` và nhiều tên khác.
- **Cần đổi ở giai đoạn sau:** giá trị mặc định trong `application.properties` và `.env.example` thành `booking_db`.
- **Rủi ro:** CSDL đang chạy tên `pitchos_booking_db` (theo script chung) sẽ **không tự đổi tên**. MySQL không có lệnh đổi tên CSDL; cần dump/restore, hoặc tạm giữ `BOOKING_DB_NAME=pitchos_booking_db`. Script chung của Hùng **cố định** `pitchos_booking_db` và có `DROP TABLE IF EXISTS bookings`, nên **chạy lại script đó sẽ xóa dữ liệu**. Cần thống nhất với Hùng.
- Chuỗi kết nối có `useSSL=false&allowPublicKeyRetrieval=true`: chấp nhận được khi dev; production cần SSL (chờ Hùng).

## 4. Eureka và tên service

| Mục | Hiện trạng | Nhận xét |
|---|---|---|
| Bật / tắt | `EUREKA_ENABLED` (mặc định `false`), dùng cho cả `register` và `fetch` | Phù hợp: chạy độc lập được, khi tích hợp chỉ cần đổi biến |
| Địa chỉ | `EUREKA_URL`, mặc định `http://localhost:8761/eureka/` | Mặc định chỉ là giá trị cục bộ, **không phải địa chỉ của nhóm**. Khi bật phải đặt `EUREKA_URL` (chờ Hoàng cung cấp) |
| Tên đăng ký của Booking | `spring.application.name=pitchos-booking-service` (cố định trong file) | Có thể ghi đè bằng biến `SPRING_APPLICATION_NAME` mà không sửa code (chưa chạy thử cách này). Bảng phân công dùng tên khác → **chờ nhóm chốt** |
| Tên Pitch để tra cứu | `PITCH_SERVICE_ID`, mặc định `pitchos-pitch-service` | Phải trùng `spring.application.name` của Pitch. Bảng phân công ghi "pitchs-service" → **chờ người phụ trách Pitch xác nhận** |
| instance-id | `${spring.application.name}:${server.port}`, `prefer-ip-address=true` | Nhiều instance trên cùng máy phân biệt được bằng cổng (đã kiểm chứng) |
| Health check | **Không có actuator** | Eureka chỉ dựa vào heartbeat; trang trạng thái không mở được. Đề xuất (giai đoạn sau, cần đồng ý) thêm `spring-boot-starter-actuator` và `eureka.client.healthcheck.enabled=true` |
| Cân bằng tải | Spring Cloud LoadBalancer, round-robin; cache danh sách instance khoảng 35 giây; tải registry 5 giây | Instance mới có thể mất khoảng 40 giây mới nhận request |
| OpenFeign | Chỉ gọi URL cố định | Khi dùng Eureka phải dùng WebClient (đang là mặc định) |

## 5. Hợp đồng cần nhóm phối hợp

- **Auth (Hoàng):** [DE_XUAT_HOP_DONG_JWT_AUTH.md](DE_XUAT_HOP_DONG_JWT_AUTH.md)
- **Pitch:** [DE_XUAT_HOP_DONG_PITCH.md](DE_XUAT_HOP_DONG_PITCH.md)
- **CSDL (Hùng):** tên `booking_db` và cách chuyển dữ liệu từ `pitchos_booking_db`; MySQL 8.0; ai chạy V1/V2; user và quyền (cần `ALTER`, `INDEX`, `CREATE`); SSL; không chạy lại script chung trên CSDL có dữ liệu.
- **Hạ tầng (Hoàng):** địa chỉ Eureka, tên service chính thức, cổng (8083 hay 8081).

## 6. File dự kiến thay đổi ở giai đoạn tiếp theo (chưa sửa)

| File | Thay đổi | Điều kiện |
|---|---|---|
| `src/main/resources/application.properties` | Mặc định `BOOKING_DB_NAME` → `booking_db`; `ddl-auto` → `validate` | `validate` chỉ đổi sau khi có V1 và quy trình đã được nhóm đồng ý |
| `.env.example` | `BOOKING_DB_NAME=booking_db`; ghi chú `EUREKA_URL`, `PITCH_SERVICE_ID`, `SPRING_APPLICATION_NAME` | Cùng lúc với thay đổi trên |
| **Mới** `db/V1__booking_schema_mysql.sql` | Tạo bảng `bookings` và `pitch_locks` đúng như sau V2 (`CREATE TABLE IF NOT EXISTS`, không `DROP`) | Cần cho `validate` trên CSDL mới; kiểm thử trên MySQL thử nghiệm |
| `README.md`, `pitchos-booking-service/README.md`, `HOP_DONG_API_TICH_HOP.md`, các báo cáo | Quy trình V1 → V2 → khởi động; `booking_db`; Eureka | – |
| `security/JwtUtil.java`, `CurrentUser.java`, `SecurityConfig.java` (+ test) | Theo hợp đồng JWT cuối cùng (bỏ cơ chế dùng `sub` thay `userId`, ánh xạ tên role nếu khác) | Sau khi Hoàng xác nhận |
| `client/PitchGateway`, `WebClientPitchGateway`, `service/BookingAccessPolicy`, `BookingService` (+ test) | Lọc `GET /api/v1/bookings` cho chủ sân theo danh sách sân sở hữu | Sau khi Pitch có API |
| `pom.xml` (tùy chọn) | `spring-boot-starter-actuator` cho health check Eureka | Cần đồng ý thêm dependency |
| (tùy chọn) `config/SchemaCompatibilityCheck.java` | Kiểm tra ENUM có `da_huy` lúc khởi động | Đã hoãn theo yêu cầu |

## 7. Rủi ro khi chuyển sang cấu hình mới

| Rủi ro | Hệ quả | Giảm thiểu |
|---|---|---|
| Đổi mặc định sang `booking_db` khi dữ liệu đang ở `pitchos_booking_db` | Ứng dụng kết nối CSDL khác (rỗng) → mất "dữ liệu nhìn thấy" | Kế hoạch dump/restore với Hùng; tạm thời đặt `BOOKING_DB_NAME` rõ ràng |
| Bật `validate` khi chưa có V1 | CSDL mới không khởi động được | Viết V1 trước, kiểm thử V1 → V2 → `validate` |
| `validate` không kiểm tra ENUM | Hủy đơn trả 500 nếu quên V2 | Luôn chạy V2 (chạy lại an toàn); cân nhắc kiểm tra ENUM khi khởi động |
| MySQL 8.0 chưa kiểm thử | Có thể khác hành vi | Chạy lại bộ kịch bản trên MySQL 8.0 thử nghiệm |
| Tên role / claim JWT khác | Mọi request 401/403 | Chốt hợp đồng JWT trước khi tích hợp |
| Tên service Eureka lệch | Gọi Pitch trả 503 (không tìm thấy instance) | Chốt tên `spring.application.name` của cả hai bên |
| Pitch chưa có `ownerId` | Chủ sân bị chặn (fail-closed); chỉ admin duyệt được | Pitch bổ sung theo đề xuất |
| Chạy lại script chung của nhóm | `DROP TABLE IF EXISTS bookings` xóa dữ liệu | Chỉ dùng script chung cho CSDL mới; dùng V1/V2 của Booking |

## 8. Kết luận và bước tiếp theo

**Đã phù hợp:**
- fail-closed, WebClient mặc định, cơ chế bật Eureka bằng biến môi trường;
- truyền tên CSDL qua biến môi trường (đã chứng minh);
- V2 không cố định tên CSDL và chạy lại an toàn;
- bộ test độc lập với `ddl-auto` của cấu hình chính.

**Cần điều chỉnh:**
- mặc định `booking_db`;
- có V1 trước khi bật `validate`;
- kiểm thử MySQL 8.0;
- chốt hợp đồng JWT, Pitch và tên service Eureka.

**Bước tiếp theo đề xuất** (mỗi bước chờ bạn duyệt):
1. Được phép dựng MySQL 8.0 thử nghiệm và chạy lại toàn bộ kịch bản.
2. Viết V1, kiểm thử V1 → V2 → `validate` trên MySQL thử nghiệm.
3. Đổi mặc định `booking_db` và `validate` (sau bước 2 và sau khi Hùng đồng ý cách chuyển dữ liệu).
4. Gửi 2 hợp đồng đề xuất cho Hoàng và người phụ trách Pitch.
5. Chỉ sửa code JWT và Pitch sau khi hợp đồng được chốt.
