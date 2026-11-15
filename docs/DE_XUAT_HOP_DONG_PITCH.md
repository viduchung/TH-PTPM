# ĐỀ XUẤT HỢP ĐỒNG PITCH SERVICE CHO BOOKING (ownerId VÀ DANH SÁCH SÂN THEO CHỦ)

> Người đề xuất: Nhật (Booking) · Gửi: người phụ trách Pitch Service · Ngày: 16/11/2026 · Trạng thái: **ĐỀ XUẤT, chờ xác nhận**
> Booking **không sửa Pitch**. Hai thay đổi dưới đây đều **chỉ thêm, không phá vỡ** API hiện có.

## 1. Bối cảnh

Booking áp dụng chính sách chủ sân **fail-closed**: chủ sân (`chusan`) chỉ được xem, duyệt hoặc từ chối đơn khi Pitch xác nhận họ là chủ sân. Pitch hiện **chưa trả `ownerId`**, nên trong tích hợp thật:
- chủ sân bị 403 ở mọi thao tác, chỉ admin duyệt được;
- `GET /api/v1/bookings` của chủ sân luôn 403, vì Booking không biết chủ sân sở hữu những sân nào.

## 2. Thay đổi 1: thêm `ownerId` vào chi tiết sân

`GET /api/v1/pitches/{id}`: giữ nguyên mọi trường hiện có, **thêm** `ownerId`.

```json
{ "id": 1, "name": "…", "pitchType": "san_5", "pricePerHour": 300000.00, "address": "…",
  "status": "hoat_dong", "createdAt": "2026-10-01T08:00:00", "ownerId": 2 }
```

| Trường | Yêu cầu |
|---|---|
| `ownerId` | Số nguyên, **không null** với mọi sân, bằng `userId` (trong JWT của Auth) của tài khoản `chusan` sở hữu sân |
| `id`, `pricePerHour` (> 0), `status` (`hoat_dong` / `bao_tri`) | Giữ như hiện tại (Booking kiểm tra) |
| Quyền | Giữ công khai (Booking gọi không kèm token) |
| Lỗi | Sân không tồn tại → 404 (Booking trả 404); lỗi khác → Booking trả 503 và **không** cấp quyền |

Booking **đã sẵn sàng**: `PitchResponseDTO.ownerId` đã có, đã có test cho cả ownerId trùng (200) và khác (403). Không cần sửa code Booking cho thay đổi này.

## 3. Thay đổi 2: danh sách sân theo chủ sở hữu

Đề xuất **mở rộng API danh sách hiện có**, vốn đã có bộ lọc `pitchType` và `status`:

```
GET /api/v1/pitches?ownerId={ownerId}
→ 200 [ { "id": 1, "ownerId": 2, … }, { "id": 5, "ownerId": 2, … } ]   (rỗng → [])
```

| Mục | Đề xuất |
|---|---|
| Tham số | `ownerId` (số nguyên), tùy chọn; kết hợp được với `pitchType`, `status` |
| Kết quả | Mọi sân của chủ đó, kể cả sân `bao_tri` (để chủ sân vẫn xem được đơn cũ) |
| Quyền | Giữ như API danh sách hiện tại (công khai), hoặc yêu cầu token nội bộ (cần thống nhất) |
| Phân trang | Không bắt buộc (số sân của một chủ thường nhỏ); nếu có thì cho Booking lấy hết |

Phương án thay thế: `GET /api/v1/owners/{ownerId}/pitches`. Phương án trên gọn hơn vì dùng lại API có sẵn.

**Việc phía Booking sau khi có API:** sửa `PitchGateway` / `WebClientPitchGateway`, `BookingAccessPolicy`, `BookingService` để `GET /api/v1/bookings` của chủ sân **chỉ trả đơn thuộc các sân của họ** (thay vì 403 như hiện nay), kèm test.

## 4. Câu hỏi cần xác nhận

1. Tên service khi đăng ký Eureka (`spring.application.name`): `pitchos-pitch-service` hay tên khác ("pitchs-service" theo bảng phân công)? Booking sẽ đặt `PITCH_SERVICE_ID` theo tên này.
2. Cổng Pitch trong hệ thống mới (8082?).
3. Pitch có thêm Eureka client không, và vào khi nào?
4. Giá trị `status` có giữ `hoat_dong` / `bao_tri` không? (Booking chỉ cho đặt sân khi `status = hoat_dong`.)
5. API `/quote` giá giờ vàng (TH6 phía Pitch) có thay đổi cách Booking tính tiền không? Hiện Booking tính `pricePerHour × phút / 60`.
