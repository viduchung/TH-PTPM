# BÁO CÁO PHÂN TÍCH DỰ ÁN PTPM CŨ

> Người thực hiện: Nhật (booking-service) · Ngày lập: 15/11/2026
> Mục đích: chọn bản PTPM làm cơ sở cho workspace `C:\nhom13` và xác định những gì tái sử dụng được cho nhiệm vụ TH1–TH7.

## 1. Các bản PTPM tìm thấy trên máy

| # | Vị trí | Git | Trạng thái | Nội dung |
|---|---|---|---|---|
| A | `C:\New folder (2)\PTPM` | Có, nhánh `nhat` @ `d5a7299` (02/11) | 1 commit chưa push (báo cáo Word), **thiếu** commit `26a8de2` (chống đặt trùng đồng thời) | 65 file. Auth/Pitch chỉ có Entity + Repository, chưa có controller/service/security |
| B | `Downloads\PTPM-main\PTPM-main` | Có, nhánh `nhat` @ `26a8de2` (= `origin/nhat`) | Sạch | Booking đầy đủ. Auth/Pitch giống A (chưa có controller) |
| C | `Downloads\PTPM-main` | Không (bản ZIP của `main`) | File mới nhất 02/11 | **104 file**. Booking giống B, **Auth và Pitch đã có controller, service, security, DTO, exception**, Pitch có test và profile H2, có báo cáo Chương 1/3/4/5 của các thành viên khác |
| D | `Downloads\PTPM-main (1)\PTPM-main` | Không (ZIP) | 30/10 | Giống C, chỉ thiếu thư mục `.github` |
| E | `Downloads\booking-service\booking-service` | Không | 09/2026 | Bản Booking rời đời đầu, không còn phù hợp |
| F | `C:\ptpm` | – | Rỗng | – |
| – | `Downloads\QL-SanBong-PTPM-*` | – | – | Dự án khác (`football-booking-service`), không thuộc PTPM |

So sánh bằng `diff -rq` (không tính `target/`, `.git/`):
- B và C có `pitchos-booking-service` **giống hệt nhau**. C hơn B ở Auth, Pitch và tài liệu của các thành viên khác.
- A khác C ở chỗ: Booking của A thiếu khóa chống đặt trùng; Auth/Pitch của A chưa có tầng controller/service. Thứ A có mà C không có chỉ là `docs/BAO_CAO_CHUONG_2_NHAT.docx` và `docs/images/so-do-2-*.png`.

## 2. Bản được chọn làm cơ sở và lý do

**Chọn bản C (`C:\Users\admin\Downloads\PTPM-main`).**

1. Đây là bản duy nhất có **cả 3 service build và chạy được**: Auth phát token thật, Pitch có API thật. Booking cần hai service này để tích hợp.
2. Booking ở bản C đã có commit chống đặt trùng `26a8de2`, mới hơn bản A.
3. Có đủ tài liệu, Postman collection và Pitch Service giả lập (`demo/MockPitchService.java`).
4. Đã kiểm chứng: build gốc ở `C:\nhom13` **thành công cả 3 service** (`mvn -DskipTests package`), test gốc của Pitch **8/8 pass**.

Bản A có thêm bản Word Chương 2 và ảnh sơ đồ của Nhật. Các file này được chép **nguyên vẹn** vào `docs/tham-khao-ptpm-cu/` để tham khảo. Không file nào ở bản gốc bị sửa.

Cách sao chép: `tar` toàn bộ C, loại trừ thư mục con trùng lặp `PTPM-main/` và `target/`. Sau đó `diff -rq` xác nhận bản sao **giống hệt nguồn** (104 file).

## 3. Kiến trúc tổng thể PTPM

```text
Client (Postman)
   │  Bearer JWT
   ├──► pitchos-auth-service   :8081  → pitchos_user_db   (đăng ký, đăng nhập, phát JWT)
   ├──► pitchos-pitch-service  :8082  → pitchos_pitch_db  (CRUD sân, trạng thái sân)
   └──► pitchos-booking-service:8083  → pitchos_booking_db (đặt sân)
                │  OpenFeign GET /api/v1/pitches/{id}
                └──────────────► pitch-service
```

- Spring Boot 3.2.5, Java 17 (máy dev đang có JDK 21), Spring Cloud 2023.0.1, jjwt 0.11.5, MySQL 8.
- Database-per-Service: 3 schema MySQL riêng, chỉ lưu logical ID, **không có FK chéo** (`pitchos-database/*.sql` của Hùng).
- JWT dùng chung secret HS256. Hợp đồng token thực tế (lấy từ `JwtTokenProvider` của Auth):
  - `sub` = username
  - `userId` = id số
  - `role` ∈ {`admin`, `chusan`, `user`} (chữ thường)
  - `fullName`
  - `iat`, `exp` (mặc định 24 giờ)
- Mỗi service tự kiểm tra token: `JwtAuthenticationFilter` nạp quyền `ROLE_<role>`.
- **Không có** Eureka Server, API Gateway hay Spring Cloud LoadBalancer trong PTPM cũ.

## 4. Những chức năng đã hoàn thiện (đọc từ code thực tế)

### Booking Service (Nhật)

| Thành phần | Hiện trạng |
|---|---|
| `POST /api/v1/bookings` | Gọi Pitch qua OpenFeign, kiểm tra `hoat_dong`, tính `totalPrice = giá × phút / 60`, kiểm tra trùng lịch, lưu `cho_duyet`, trả 201 |
| `GET /api/v1/bookings/my-bookings` | Danh sách đơn của user lấy từ JWT, **không phân trang** |
| `PATCH /api/v1/bookings/{id}/status` | `chusan`/`admin` duyệt hoặc từ chối. Không cho đổi đơn đã `tu_choi` |
| Lỗi liên dịch vụ | `CustomErrorDecoder`: 404 từ Pitch → 404. `FeignException` (sập, timeout, 5xx) → 503 |
| Timeout Feign | connect 2s, read 3s |
| Chống đặt trùng | `countOverlappingBookings` (bỏ qua `tu_choi`) + `synchronized` theo `pitchId` **trong bộ nhớ**, chỉ đúng với 1 instance |
| Bảo mật | JWT filter. 401 trả **body rỗng**, 403 dùng mặc định của Spring |
| Test | **Không có test tự động nào** |

### Pitch Service (Linh): Provider của Booking
- `GET /api/v1/pitches` (lọc `pitchType`, `status`), `GET /api/v1/pitches/{id}` (công khai), `POST` và `PATCH /{id}/status` (`chusan`/`admin`).
- `PitchResponse`: `id, name, pitchType, pricePerHour, address, thumbnailUrl, googleMapUrl, status, createdAt`.
- **Không có** `ownerId` (chủ sân), **không có** API `/quote`, **không có** bảng giá giờ vàng, **không có** Eureka client.
- Có profile `h2` để chạy không cần MySQL.

### Auth Service (Hoàng)
- `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/users/me|{id}|`.
- Tự tạo 3 tài khoản demo `admin`, `chusan`, `user`.
- Không có profile H2. Cần MySQL để chạy.

## 5. Thành phần tái sử dụng cho nhiệm vụ Nhật

| Thành phần | Cách tái sử dụng |
|---|---|
| Entity `Booking`, `BookingRepository`, truy vấn trùng lịch | Giữ nguyên, mở rộng thêm cột và index |
| `PitchClient` (OpenFeign) + `CustomErrorDecoder` | **Giữ nguyên**. Thêm WebClient song song cho TH2, cấu hình để chọn bên nào |
| `GlobalExceptionHandler`, `ErrorResponse` | Giữ định dạng `{timestamp, status, error, message}`, chỉ **thêm** trường |
| `JwtUtil`, `JwtAuthenticationFilter`, `SecurityConfig` | Giữ hợp đồng token. Thêm phân quyền theo endpoint mới, trả 401/403 dạng JSON |
| `demo/MockPitchService.java` | Dùng để kiểm thử thực tế 2 instance Pitch (TH7) và tình huống Pitch sập (TH2) |
| Postman collection | Giữ nguyên request cũ, bổ sung request mới |

## 6. Những vấn đề cần chú ý khi làm nhiệm vụ Nhật

1. **Phân công lệch nhau giữa các nguồn.**
   - `BANG_PHAN_CONG_HANG_TUAN.xlsx` (Downloads, 09/11/2026) ghi: **Linh = booking-service (8081)**, **Nhật = pitchs-service (8082)**, auth 8080, Eureka 8761 do Hoàng cài, role `CUSTOMER/OWNER/ADMIN`, repo `Boyngoc/football-booking-service`.
   - Yêu cầu trực tiếp của người dùng: **Nhật = booking-service**.
   - Workspace này làm theo yêu cầu trực tiếp (booking-service). Cần nhóm xác nhận lại bảng phân công.
2. **Cổng.** PTPM cũ dùng Booking **8083** và Auth **8081**. Bảng mới dùng Booking **8081** và Auth **8080**. Nếu đổi Booking sang 8081 mà Auth vẫn để mặc định 8081 thì hai service **đụng cổng**. Vì vậy giữ mặc định 8083, đổi được qua `BOOKING_SERVICE_PORT`. Chờ nhóm thống nhất.
3. **Tên database.** PTPM dùng `pitchos_booking_db`, bảng mới ghi `booking_db`. Giữ mặc định cũ (khớp script của Hùng), đổi được qua `BOOKING_DB_NAME`.
4. **Tên role.** Auth thực tế phát `user/chusan/admin`, không phải `CUSTOMER/OWNER/ADMIN`. Booking giữ đúng role Auth đang phát, không tự đặt tên mới.
5. **Chủ sân.** Pitch Service chưa có `ownerId`, nên không thể kiểm tra "chủ sân chỉ duyệt đơn sân mình" một cách đầy đủ. Phần này phụ thuộc Pitch Service (BLOCKED một phần).
6. **API `/quote` và giá giờ vàng** thuộc phía Provider (Pitch Service), hiện chưa có.
7. **Eureka Server** chưa tồn tại, thuộc phần việc của Hoàng theo bảng tuần.
8. **Môi trường máy dev:** có JDK 21 và Maven 3.9.16; **không có MySQL, không có Docker**. Kiểm thử JPA tự động dùng H2 (chế độ MySQL), không được tính là kiểm thử MySQL thật.
9. `.env` bị commit trong PTPM cũ, nhưng giá trị trùng hoàn toàn với `.env.example` (giá trị dev mặc định, không có bí mật thật).
10. Tài liệu `TH1.pdf` (Downloads) là phiếu TH1 dùng ví dụ phòng khám. Nó xác nhận yêu cầu CRUD gồm GET danh sách, GET theo id, POST, PUT, DELETE, cùng mã 200/201/400/404 và Postman collection. Trên máy **không có** phiếu TH2–TH7. Nội dung TH2–TH7 lấy từ bảng phân công tuần và yêu cầu của người dùng.
