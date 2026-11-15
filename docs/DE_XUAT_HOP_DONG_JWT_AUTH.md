# ĐỀ XUẤT HỢP ĐỒNG JWT GIỮA AUTH SERVICE VÀ BOOKING SERVICE

> Người đề xuất: Nhật (Booking) · Gửi: Hoàng (Auth) · Ngày: 16/11/2026 · Trạng thái: **ĐỀ XUẤT, chờ xác nhận**
> Booking **không sửa Auth**. Tài liệu ghi những gì Booking cần và hành vi hiện tại của Booking (có test chứng minh) để hai bên chốt.

## 1. Đề xuất

| Mục | Đề xuất | Booking hiện tại |
|---|---|---|
| Thuật toán | **HS256**, cố định | HS256 (jjwt 0.11.5) |
| Khóa | `JWT_SECRET` là chuỗi **Base64**, giải mã ra ≥ 32 byte (256 bit). Hai bên dùng **cùng một giá trị**, trao đổi ngoài repository | `Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET))`. Thiếu `JWT_SECRET` → Booking dừng khi khởi động |
| Gửi token | Header `Authorization: Bearer <token>` | Như vậy |
| `userId` | **Bắt buộc**, số nguyên dương (id người dùng trong CSDL của Auth) | Đọc `userId`; nếu thiếu thì tạm dùng `sub` khi `sub` là số (cơ chế dự phòng, xem mục 3) |
| `role` | **Bắt buộc**, đúng **một** giá trị chữ thường trong `user` / `chusan` / `admin` | So khớp chính xác, phân biệt hoa thường; role khác → 403 |
| `sub` | Username (chuỗi) | Không dùng để phân quyền |
| `exp` | **Bắt buộc** | Hết hạn → 401 "Token đã hết hạn"; không có độ lệch đồng hồ cho phép |
| `iat` | Nên có | Không dùng |
| `fullName` | Tùy chọn | Không dùng |
| `iss` / `aud` (tùy chọn) | Đề xuất `iss = pitchos-auth`, `aud` có `pitchos-booking` | **Chưa** kiểm tra; chỉ thêm khi hai bên đồng ý |

Ví dụ payload:

```json
{ "sub": "user", "userId": 3, "role": "user", "fullName": "…", "iat": 1794738786, "exp": 1794825186 }
```

## 2. Hành vi của Booking với từng loại token (đã có test)

| Token | Kết quả | Test |
|---|---|---|
| Không có header hoặc không có tiền tố `Bearer ` | 401 "Chưa đăng nhập hoặc thiếu token" | `Th5SecurityIntegrationTest` |
| Sai chữ ký, sai định dạng | 401 "Token không hợp lệ" | `Th5SecurityIntegrationTest`, `Th5JwtClaimsAndRoleIntegrationTest` |
| Hết hạn | 401 "Token đã hết hạn" | như trên, trên 9 endpoint |
| Thiếu `role`, hoặc `role` rỗng | 401 | `tokenWithoutRole_returns401` |
| Thiếu `userId`, `sub` là username | 401 | `tokenWithoutUserId_andUsernameSubject_returns401` |
| Thiếu `userId`, `sub` là số | **Chấp nhận**, dùng `sub` làm `userId` | `tokenWithoutUserId_butNumericSubject_usesSubjectAsUserId` |
| `role` lạ (`guest`, `CUSTOMER`, `ADMIN` viết hoa…) | 403 (không bao giờ được coi như admin) | `unknownRole_cannotListBookings` |
| `userId` trong body hoặc query | Bị bỏ qua, luôn dùng `userId` trong token | `userIdFromBodyIsIgnored` |

## 3. Câu hỏi cần Hoàng xác nhận

1. **Tên role chính thức:** `user` / `chusan` / `admin` (như Auth PTPM cũ) hay `CUSTOMER` / `OWNER` / `ADMIN` (như bảng phân công)? Nếu dùng bộ thứ hai, Booking cần sửa `CurrentUser` và `SecurityConfig` (ánh xạ tên role). Nếu chưa sửa thì **mọi request đều bị từ chối**.
2. `userId` có **luôn** nằm trong token không? Nếu có, đề xuất **bỏ cơ chế dự phòng** dùng `sub` khi `sub` là số, để hợp đồng chặt hơn.
3. `JWT_SECRET` có mã hóa **Base64** không? Nếu Auth dùng chuỗi thô, chữ ký sẽ không khớp.
4. Thời hạn token: Auth PTPM cũ mặc định 24 giờ. Có cần cho phép lệch đồng hồ (ví dụ ≤ 60 giây) giữa các máy không?
5. Có dùng `iss` / `aud` không?
6. Cổng của Auth trong hệ thống mới (8080 theo bảng phân công) và tài khoản demo để kiểm thử tích hợp (mật khẩu không ghi vào repository).

## 4. Việc phía Booking sau khi chốt

- Sửa `security/JwtUtil.java`, `CurrentUser.java`, `SecurityConfig.java` theo hợp đồng cuối, và cập nhật test `Th5*`.
- Kiểm thử tích hợp với Auth thật: lấy token qua `POST /api/v1/auth/login` (Postman thư mục 0).
