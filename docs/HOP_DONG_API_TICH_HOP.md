# HỢP ĐỒNG API TÍCH HỢP CỦA BOOKING SERVICE

> Tài liệu này mô tả **những gì Booking cần** từ các dịch vụ của thành viên khác. Nội dung lấy từ mã nguồn PTPM bản `Downloads\PTPM-main` (đọc ngày 15/11/2026). Không chứa mã nguồn của họ.
> Nếu Auth hoặc Pitch đổi hợp đồng, phải cập nhật Booking và tài liệu này.
> Tài liệu này mô tả hợp đồng **đang dùng**. Các thay đổi **đề xuất** (chờ xác nhận) nằm ở `DE_XUAT_HOP_DONG_JWT_AUTH.md` và `DE_XUAT_HOP_DONG_PITCH.md`.

## 1. Pitch Service (Provider) — Booking là Consumer

**Endpoint duy nhất Booking gọi:** `GET {PITCH_SERVICE_URL}/api/v1/pitches/{id}`

- Không cần token (trong PTPM, `GET /api/v1/pitches/**` là công khai).
- Mặc định `PITCH_SERVICE_URL=http://localhost:8082`. Khi bật TH7 (`PITCH_DISCOVERY_ENABLED=true`), Booking gọi `http://{PITCH_SERVICE_ID}` (mặc định `pitchos-pitch-service`) qua Eureka + LoadBalancer.

**Phản hồi 200** (Booking chỉ đọc các trường in đậm, trường khác bị bỏ qua):

```json
{
  "id": 1,
  "name": "Sân bóng Mini ...",
  "pitchType": "san_5",
  "pricePerHour": 300000.00,
  "address": "...",
  "status": "hoat_dong",
  "createdAt": "2026-10-01T08:00:00",
  "ownerId": 2
}
```

| Trường | Bắt buộc với Booking | Ý nghĩa |
|---|---|---|
| **`id`** | Có, phải trùng `{id}` đã gọi | Khác hoặc thiếu → Booking coi dữ liệu không hợp lệ (503) |
| **`name`** | Không | Hiển thị trong báo giá |
| **`pricePerHour`** | Có, > 0 | Tính `totalPrice = pricePerHour × phút / 60` |
| **`status`** | Có | `hoat_dong` mới đặt được; giá trị khác (ví dụ `bao_tri`) → 400 |
| **`ownerId`** | Bắt buộc để chủ sân thao tác (Pitch **hiện chưa có**) | Booking chỉ cho chủ sân (`chusan`) có `userId` trùng xem / duyệt / từ chối đơn của sân (TH5). **Thiếu `ownerId` → chủ sân bị từ chối (fail-closed)**; chỉ khi bật chế độ tương thích không an toàn `BOOKING_CHUSAN_WITHOUT_OWNER_INFO=true` (demo cục bộ) mới bỏ qua |

**Cách Booking xử lý phản hồi lỗi:**

| Phản hồi từ Pitch | Booking trả cho client | Thử lại? |
|---|---|---|
| 404 | 404 "Không tìm thấy sân bóng với pitchId đã gửi" | Không |
| 502 / 503 / 504, mất kết nối, quá thời gian chờ | 503 (sau tối đa `PITCH_MAX_RETRIES` lần thử lại) | Có, backoff |
| 500, mã khác, JSON hỏng, dữ liệu thiếu hoặc sai | 503 | Không |

Khi kiểm tra quyền chủ sân, mọi lỗi ở bảng trên đều **không** cấp quyền: đơn giữ nguyên trạng thái.

**Cần bổ sung phía Pitch để chủ sân dùng đầy đủ:** (1) trường `ownerId` trong `GET /api/v1/pitches/{id}`; (2) một API liệt kê các sân theo chủ sở hữu, để Booking lọc `GET /api/v1/bookings` cho chủ sân (hiện chủ sân nhận 403 ở API danh sách).

## 2. Auth Service — Booking chỉ kiểm tra JWT

Booking **không gọi HTTP** tới Auth. Booking tự kiểm tra chữ ký JWT bằng `JWT_SECRET`, vì vậy `JWT_SECRET` **phải trùng** secret mà Auth dùng để ký. Người chạy tự lấy giá trị từ người phụ trách Auth; repository không chứa secret này.

**Token Booking chấp nhận:**
- Header: `Authorization: Bearer <token>`
- Thuật toán HS256, key = `Base64.decode(JWT_SECRET)`

| Claim | Bắt buộc | Ý nghĩa với Booking |
|---|---|---|
| `userId` | Có (số) | Chủ đơn đặt; **không** lấy userId từ body hay query |
| `role` | Có | `user` (khách), `chusan` (chủ sân), `admin` |
| `exp` | Có | Hết hạn → 401 "Token đã hết hạn" |
| `sub`, `fullName`, `iat` | Không | Không dùng |

**API của Auth dùng để lấy token** (chỉ dùng trong Postman, Booking không gọi):
- `POST {authUrl}/api/v1/auth/login`, body `{"username": "...", "password": "..."}` → `200 {"accessToken": "...", "tokenType": "Bearer", "expiresIn": 86400, "userId": 3, "username": "...", "fullName": "...", "role": "user"}`
- `POST {authUrl}/api/v1/auth/register`, body `{"username" (4–50 ký tự), "email", "password" (≥ 6), "fullName", "phone" (tùy chọn)}`
- Mặc định PTPM: Auth chạy cổng 8081, có sẵn tài khoản demo `admin`, `chusan`, `user`. Mật khẩu demo do người phụ trách Auth cung cấp; không lưu trong repository này.

## 3. MySQL — CSDL riêng của Booking

- CSDL `BOOKING_DB_NAME` (mặc định `pitchos_booking_db`), gồm bảng `bookings` và `pitch_locks`.
- `user_id`, `pitch_id` chỉ là ID logic, **không có khóa ngoại** sang CSDL của Auth hay Pitch (Database-per-Service).
- Tạo mới: `CREATE DATABASE`, rồi Hibernate tự tạo bảng.
- CSDL đã tạo từ script chung của nhóm: **bắt buộc** chạy V2 trước khi khởi động Booking, chỉ định tên CSDL khi gọi lệnh (`mysql -u <user> -p --default-character-set=utf8mb4 <ten_csdl_booking> < pitchos-booking-service/db/V2__booking_th1_th7_mysql.sql`, không `--force`). V2 chạy lại an toàn, dừng với mã `V2_LOI_…` nếu cấu trúc không tương thích, không xóa dữ liệu. Cần quyền `ALTER`, `INDEX`, `CREATE`.
- Đã kiểm thử trên **MySQL 8.4.3** thử nghiệm (cả 2 cách, log `docs/bang-chung-kiem-thu/th4-mysql-8.4.3.txt`). **Chưa** kiểm thử MySQL 8.0 và MySQL của nhóm. Chờ Hùng xác nhận: tên CSDL, phiên bản MySQL, ai quản lý schema Booking / có dùng Flyway hay Liquibase, `ddl-auto`, user và quyền, SSL.

## 4. Eureka (TH7, tùy chọn)

| Mục | Giá trị |
|---|---|
| Booking đăng ký với tên | `pitchos-booking-service`, instance-id `pitchos-booking-service:{port}` |
| Booking tìm Pitch theo tên | `pitchos-pitch-service` (đổi bằng `PITCH_SERVICE_ID`) — Pitch phải đăng ký đúng tên này |
| Địa chỉ Eureka | `EUREKA_URL` (mặc định `http://localhost:8761/eureka/`) |
| Bật | `EUREKA_ENABLED=true` và `PITCH_DISCOVERY_ENABLED=true` |
| Giới hạn | Chế độ OpenFeign (`PITCH_CLIENT_TYPE=feign`) chỉ gọi URL cố định |
