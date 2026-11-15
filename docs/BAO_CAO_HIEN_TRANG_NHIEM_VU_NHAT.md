# BÁO CÁO HIỆN TRẠNG NHIỆM VỤ NHẬT (BOOKING SERVICE) TRƯỚC KHI TRIỂN KHAI

> Ngày lập: 15/11/2026 · Cơ sở: mã nguồn `pitchos-booking-service` vừa sao chép vào `C:\nhom13` (bản PTPM-main, xem [BAO_CAO_PHAN_TICH_PTPM_CU.md](BAO_CAO_PHAN_TICH_PTPM_CU.md))
> Nguồn yêu cầu TH1–TH7:
> - (1) yêu cầu trực tiếp của người dùng;
> - (2) `BANG_PHAN_CONG_HANG_TUAN.xlsx`, cột booking-service;
> - (3) `TH1.pdf`, phiếu TH1.
>
> Không có phiếu TH2–TH7 trên máy.

Bằng chứng ban đầu:
- `mvn -DskipTests package` của Booking gốc: **thành công**.
- Booking gốc có **0 test tự động**.
- Không có MySQL hay Docker trên máy, nên chưa chạy được Booking gốc với CSDL thật.

| Nhiệm vụ | Yêu cầu (theo tài liệu) | Hiện trạng (đã đọc code) | Phần cần làm | Trạng thái |
|---|---|---|---|---|
| TH1 | CRUD Booking: đặt, sửa, hủy, xem lịch đặt; duyệt và từ chối; DTO; mã trạng thái HTTP | Có `POST`, `GET /my-bookings`, `PATCH /{id}/status`. Có DTO request/response | Thiếu `GET /{id}`, `GET` danh sách chung, `PUT /{id}` (sửa), hủy đơn. Thiếu kiểm tra quyền sở hữu khi xem/sửa. Thiếu trạng thái "đã hủy". Không có test | PARTIAL |
| TH2 | Booking gọi Pitch kiểm tra sân bằng **WebClient**; timeout; 404/500/503; không tạo đơn khi không xác minh được sân | Có OpenFeign + `CustomErrorDecoder` (404) + chuyển `FeignException` thành 503 + timeout 2s/3s | **Chưa có WebClient.** Chưa phân biệt dữ liệu sân không hợp lệ (thiếu giá hoặc giá ≤ 0). Chưa có test | PARTIAL |
| TH3 | Phân trang, lọc danh sách booking; validation ngày giờ; xử lý lỗi tập trung; response lỗi thống nhất | Có `GlobalExceptionHandler`, `ErrorResponse`. Validation chỉ `@NotNull` và `end > start` | Chưa có phân trang, sắp xếp, lọc (trạng thái, ngày, sân). Chưa chặn đặt giờ quá khứ hay thời lượng bất thường. 401/403 chưa trả JSON. Lỗi validation chỉ báo trường đầu tiên | PARTIAL |
| TH4 | booking_db; quan hệ booking – field – user; `@Transactional`; chống trùng lịch; index | Entity `Booking` (logical ID `user_id`, `pitch_id`, không FK). Truy vấn trùng lịch của Hùng. Khóa `synchronized` trong bộ nhớ | Chưa có `@Transactional`, chưa có index. Khóa chỉ đúng với 1 instance. Thiếu `updated_at` và giá đơn vị lúc đặt. Chưa kiểm thử ghi/đọc CSDL | PARTIAL |
| TH5 | Bảo vệ endpoint theo vai trò; user chỉ thao tác đơn của mình; chủ sân chỉ duyệt sân mình; không tin userId từ client | JWT filter đúng hợp đồng Auth. `userId` lấy từ token. `POST`, `my-bookings` cho `user`; `PATCH status` cho `chusan`/`admin` | Thiếu kiểm tra sở hữu cho endpoint mới. Chủ sân hiện duyệt được **mọi** đơn, do Pitch chưa có `ownerId`. 401/403 chưa có body. Chưa có test token sai, hết hạn hay thiếu | PARTIAL / BLOCKED một phần |
| TH6 | Consumer gọi Pitch; xử lý lỗi; retry có giới hạn; **idempotency**; chống race condition | Booking là Consumer (Feign). Chưa có retry. Không có idempotency | Thêm Idempotency-Key + ràng buộc UNIQUE. Thêm khóa CSDL thay khóa bộ nhớ. Retry giới hạn cho GET. API báo giá phía Booking. Test đồng thời | MISSING (idempotency, retry) |
| TH7 | Đăng ký booking-service vào Eureka; gọi Pitch bằng service-name; load balancing | Không có Eureka client, không có LoadBalancer. Repo không có Eureka Server | Thêm Eureka client (bật/tắt bằng cấu hình), `@LoadBalanced` WebClient, gọi `lb://pitchos-pitch-service`. Kiểm thử 2 instance Pitch | MISSING |

## Phụ thuộc bên ngoài đã xác định

| Phụ thuộc | Ảnh hưởng | Người liên quan |
|---|---|---|
| Pitch Service chưa có `ownerId` | Chưa kiểm tra được đầy đủ "chủ sân chỉ duyệt sân mình" | Phụ trách Pitch Service |
| Pitch Service chưa có `/quote`, giá giờ vàng | Booking tự tính giá từ `pricePerHour` | Phụ trách Pitch Service |
| Pitch Service chưa có Eureka client | Ở môi trường thật, Booking chưa tìm được Pitch qua Eureka cho tới khi Pitch đăng ký | Phụ trách Pitch Service |
| Chưa có Eureka Server | Kiểm thử Eureka cần server tạm | Hoàng (theo bảng tuần) |
| Máy không có MySQL, Docker | Không kiểm thử được MySQL thật | Môi trường |
| Port 8083/8081 và DB `pitchos_booking_db`/`booking_db` chưa thống nhất | Giữ mặc định cũ, đổi được qua biến môi trường | Cả nhóm |
