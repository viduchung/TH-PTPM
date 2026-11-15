# KẾ HOẠCH TRIỂN KHAI NHIỆM VỤ NHẬT TH1–TH7 (BOOKING SERVICE)

> Phạm vi: chỉ sửa `pitchos-booking-service/` và thêm tài liệu trong `docs/`. Không sửa Auth, Pitch hay script SQL của Hùng.

## Nguyên tắc tương thích
- **Giữ nguyên** 3 API cũ: `POST /api/v1/bookings`, `GET /api/v1/bookings/my-bookings`, `PATCH /api/v1/bookings/{id}/status`. Giữ nguyên body, mã trạng thái và quyền.
- **Giữ nguyên** OpenFeign `PitchClient`. WebClient được thêm song song và chọn bằng `pitch.client.type` (`webclient` mặc định, `feign` để dùng lại cách cũ).
- **Giữ** định dạng lỗi `{timestamp, status, error, message}`, chỉ **thêm** `path`, `details`.
- **Giữ** cổng mặc định 8083 và DB mặc định `pitchos_booking_db`. Đổi được qua `BOOKING_SERVICE_PORT`, `BOOKING_DB_NAME`.
- **Giữ** hợp đồng JWT của Auth (`userId`, `role` = `user|chusan|admin`).

## Bảng kế hoạch

| Nhiệm vụ | Yêu cầu | Hiện trạng | Phần cần làm | Trạng thái lúc lập |
|---|---|---|---|---|
| TH1 | CRUD + duyệt/từ chối | PARTIAL | `GET /bookings/{id}`, `GET /bookings` (theo vai trò), `PUT /bookings/{id}` (sửa giờ hoặc sân khi `cho_duyet`), `PATCH /bookings/{id}/cancel` + `DELETE /bookings/{id}` (hủy mềm → `da_huy`, giữ lịch sử). Chặn chuyển trạng thái sai | PARTIAL |
| TH2 | WebClient + xử lý lỗi | PARTIAL | `PitchGateway` (interface) với 2 bản cài đặt: `WebClientPitchGateway` (mới) và `FeignPitchGateway` (bọc `PitchClient` cũ). Timeout connect và response. 404 → 404, 5xx/timeout/mất kết nối → 503. Dữ liệu sân sai → 503 | PARTIAL |
| TH3 | Phân trang, lọc, validation | PARTIAL | `GET /bookings?status&pitchId&from&to&page&size&sort` (giới hạn `size` ≤ 100, chỉ cho sắp xếp theo cột trong danh sách trắng). `my-bookings` nhận thêm `page/size` mà vẫn trả kiểu cũ khi không truyền. Validation: giờ bắt đầu ở tương lai, thời lượng 30 phút đến 8 giờ. Lỗi validation trả đủ danh sách trường | PARTIAL |
| TH4 | JPA/MySQL, transaction, trùng lịch | PARTIAL | `@Transactional` ở service. Index `(pitch_id, start_time, end_time)`, `(user_id, created_at)`, `status`. Cột `updated_at`, `price_per_hour`, `idempotency_key`. Bảng khóa `pitch_locks` (`SELECT … FOR UPDATE`) thay khóa bộ nhớ. Script nâng cấp `db/` riêng của Booking | PARTIAL |
| TH5 | JWT + phân quyền + sở hữu | PARTIAL | Ma trận quyền cho endpoint mới. User chỉ xem, sửa, hủy đơn của mình; xem đơn người khác → 403. Chủ sân: dùng `ownerId` nếu Pitch trả về; nếu chưa có thì làm theo cấu hình `booking.security.chusan-without-owner-info` (mặc định cho phép, ghi cảnh báo). 401/403 dạng JSON, phân biệt token hết hạn và token sai | PARTIAL |
| TH6 | Consumer/Provider, retry, idempotency | MISSING | Header `Idempotency-Key`. UNIQUE `(user_id, idempotency_key)`. Gửi lại cùng key → trả đơn cũ (200 + `Idempotent-Replayed: true`). Cùng key nhưng nội dung khác → 409. Retry tối đa 2 lần, backoff, chỉ cho GET và chỉ khi lỗi tạm thời. `GET /bookings/quote` (báo giá, không tạo đơn) | MISSING |
| TH7 | Eureka, discovery, LB | MISSING | Thêm `spring-cloud-starter-netflix-eureka-client` (bật bằng `EUREKA_ENABLED=true`). `@LoadBalanced WebClient.Builder`. URL `lb://pitchos-pitch-service` khi bật discovery. Test LoadBalancer với 2 instance Pitch. Kiểm thử thực tế với Eureka Server tạm (ngoài workspace) và 2 Pitch giả lập | MISSING |

## Thứ tự làm
TH4 (nền dữ liệu) → TH1 → TH3 → TH2 → TH5 → TH6 → TH7. Mỗi bước đều chạy `mvn test`.

## Kiểm thử dự kiến
- **Test tự động** (`mvn test`, H2 chế độ MySQL, server HTTP giả lập Pitch bằng `com.sun.net.httpserver`):
  - CRUD, phân trang, lọc, validation.
  - Lỗi Pitch: 404, 500, 503, timeout, mất kết nối, dữ liệu sai.
  - Retry.
  - JWT: không token, token sai, token hết hạn, sai role, đơn của người khác.
  - Idempotency.
  - 10 request đồng thời.
  - LoadBalancer với 2 instance.
- **Kiểm thử thực tế:**
  - Chạy file jar Booking.
  - Gọi Pitch Service thật (profile `h2`).
  - Tắt Pitch để kiểm tra 503.
  - Eureka Server tạm + 2 Pitch giả lập đăng ký qua REST API của Eureka.
- **Không làm được:** MySQL thật, vì máy không có MySQL hay Docker. Sẽ ghi rõ là BLOCKED.
