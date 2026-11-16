# booking-service — Nghiệp vụ đặt sân & điều phối giao dịch (Nhật)

| | |
|---|---|
| Port | `8081` (instance 2: `--server.port=8083`) |
| CSDL | `booking_db` (bảng `bookings`, `idempotency_keys`, `pitch_locks`) |
| Tên trên Eureka | `booking-service` |
| Gọi sang | `pitchs-service` (kiểm tra sân, báo giá), `auth-service` (`/api/v1/auth/me`) |

## Chạy

```bash
cd booking-service
mvn spring-boot:run                                    # MySQL booking_db + Eureka 8761
mvn spring-boot:run -Dspring-boot.run.profiles=h2      # không cần MySQL
java -jar target/booking-service-1.0.0.jar --server.port=8083   # instance thứ 2 (TH7)
```

Swagger UI: http://localhost:8081/swagger-ui.html (bấm **Authorize**, dán `accessToken` lấy từ `POST :8080/api/v1/auth/login`).

Mặc định booking gọi pitchs-service theo tên `http://pitchs-service` qua Eureka + Spring Cloud LoadBalancer, còn auth-service theo `http://localhost:8080` (auth-service đang tắt Eureka). Đổi bằng biến môi trường:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `PITCH_SERVICE_URL` / `PITCH_LOAD_BALANCED` | `http://pitchs-service` / `true` | Không chạy Eureka: `http://localhost:8082` / `false` |
| `AUTH_SERVICE_URL` / `AUTH_LOAD_BALANCED` | `http://localhost:8080` / `false` | |
| `EUREKA_ENABLED`, `EUREKA_SERVER_URL` | `true`, `http://localhost:8761/eureka/` | |
| `JWT_SECRET`, `INTERNAL_SERVICE_KEY` | khóa dùng chung của nhóm | Phải trùng auth-service / pitchs-service |

## API (`/api/v1/bookings`)

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| POST | `/` | CUSTOMER | Đặt sân → `201` + `Location`. Header `Idempotency-Key` tùy chọn: gửi lại cùng key → `200` + `Idempotent-Replayed: true` |
| GET | `/my` | CUSTOMER | Đơn của tôi (phân trang, lọc `status`) |
| GET | `/` | mọi vai trò | Phân trang + lọc `status, pitchId, userId, date, from, to`, `sort=startTime,desc`, `size ≤ 100`. Khách hàng luôn chỉ thấy đơn của mình |
| GET | `/{id}` | mọi vai trò | Khách hàng chỉ xem đơn của mình |
| PUT | `/{id}` | CUSTOMER | Đổi sân / giờ của đơn `CHO_DUYET`, tính lại giá |
| PATCH | `/{id}/cancel` | CUSTOMER (đơn của mình, trước giờ đá), ADMIN | Hủy mềm → `DA_HUY` |
| DELETE | `/{id}` | như trên | Hủy mềm, trả `204` |
| PATCH | `/{id}/status` | OWNER, ADMIN | Body `{"status":"DA_DUYET"}` hoặc `TU_CHOI` |
| GET | `/quote?pitchId&startTime&endTime` | công khai | Giá từ pitchs-service + khung giờ còn trống không |
| GET | `/pitches/{pitchId}/schedule?date=` | công khai | Các khung giờ đã kín (không lộ người đặt) |
| GET | `/instance-info` | công khai | Port của instance đang phục vụ (TH7) |

Phản hồi thành công bọc trong `ApiResponse {success, message, data, timestamp}`, lỗi theo `ErrorResponse {status, error, message, path, errorCode, errors}`, giống chuẩn data-layer của Hùng.

Trạng thái đơn theo `BookingStateTransitionValidator` của data-layer: `CHO_DUYET → DA_DUYET | TU_CHOI | DA_HUY`, `DA_DUYET → DA_HUY`; `TU_CHOI`, `DA_HUY` là trạng thái cuối và nhả khung giờ.

## Đối chiếu nhiệm vụ TH1 – TH7

| Buổi | Đã làm | Test |
|---|---|---|
| TH1 | CRUD đặt / xem / sửa / hủy, xem lịch sân theo ngày, chống trùng lịch (`start < end' AND end > start'`) | `Th1BookingCrudIntegrationTest` |
| TH2 | `PitchClient` dùng WebClient gọi `GET /api/v1/pitches/{id}` và `/quote`, kèm `X-Internal-Service-Key` | `Th2Th6PitchIntegrationTest` |
| TH3 | Phân trang, lọc, sắp xếp theo danh sách trường cho phép; validation: kết thúc sau bắt đầu, cùng ngày, ở tương lai, 30 phút–4 giờ, đặt trước tối đa 60 ngày | `Th3PagingValidationIntegrationTest` |
| TH4 | `booking_db`, `user_id` / `pitch_id` là Logical ID (không Foreign Key xuyên DB), transaction ngắn ở `BookingWriter` | toàn bộ test tích hợp (H2 chế độ MySQL) |
| TH5 | Xác thực JWT HS256 bằng khóa chung, đọc được cả token của auth-service (`role`) lẫn dạng data-layer (`userId`, `roles`); CUSTOMER đặt sân, OWNER duyệt đơn | `Th5SecurityIntegrationTest` |
| TH6 | Consumer `/quote`: 404/400 chuyển tiếp, lỗi tạm thời retry có backoff, provider chết → `503` và không tạo đơn; `Idempotency-Key` (bảng `idempotency_keys`); khóa theo sân `SELECT … FOR UPDATE` chống double-booking giữa nhiều instance | `Th2Th6PitchIntegrationTest`, `Th6IdempotencyConcurrencyIntegrationTest` |
| TH7 | Đăng ký Eureka (heartbeat 10s, hết hạn 30s), gọi `http://pitchs-service` qua Spring Cloud LoadBalancer round-robin | `Th7LoadBalancerIntegrationTest` |

```bash
mvn test     # 76 test: 53 tích hợp (H2 + pitchs/auth giả lập bằng HTTP server thật) + 23 unit
```

## Quyết định thiết kế cần biết

- **userId**: token của auth-service chỉ có `sub` (username) và `role`, không có `userId`, trong khi `bookings.user_id` là BIGINT. Booking chuyển tiếp chính token đó sang `GET /api/v1/auth/me` (token propagation) để lấy `id`, rồi nhớ tạm 10 phút theo username. Nếu token có claim `userId` (dạng data-layer) thì dùng luôn, không gọi auth. Auth-service không phản hồi → `503`, không đoán người dùng.
- **Giá**: pitchs-service là nguồn giá duy nhất (`totalAmount` của `/quote`, đã tính giờ vàng). `price_per_hour` lưu đơn giá cơ bản tại thời điểm đặt.
- **Lời gọi mạng nằm ngoài transaction**; transaction chỉ bao "khóa sân → kiểm tra trùng → lưu".
- `pitch_locks` là bảng mới do Hibernate tự tạo (`ddl-auto=update`); script `database/03_booking_db_init.sql` chưa có bảng này.

## Hạn chế còn lại

- `pitch_db` chưa lưu chủ của từng sân, nên **mọi OWNER duyệt được mọi đơn**. Khi pitchs-service trả `ownerId`, cần thêm kiểm tra quyền sở hữu sân.
- `pitchName` chỉ có trong phản hồi tạo / sửa đơn (booking_db không lưu tên sân).
- Cache `userId` theo username giữ tối đa 10 phút: tài khoản bị khóa trong khoảng đó vẫn đặt được nếu đã có trong cache.
- Đã chạy E2E với auth-service, 2 instance pitchs-service, Eureka thật trên **H2**; chưa chạy trên MySQL thật.
