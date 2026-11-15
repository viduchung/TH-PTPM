# BÁO CÁO RÀ SOÁT TRƯỚC KHI PUSH — NHIỆM VỤ NHẬT TH1–TH7

> Cập nhật: 15/11/2026 · Workspace: `C:\nhom13`, **chỉ chứa phần Booking TH1–TH7 của Nhật**. Chưa có `.git`, không chạy lệnh Git nào.
> Báo cáo gộp 3 vòng rà soát:
> - (1) Rà soát workspace đầy đủ;
> - (2) Đóng gói bản sạch;
> - (3) Thu gọn `C:\nhom13` về phần của Nhật.
>
> Chi tiết giữ, loại và sửa: [BAO_CAO_DONG_GOI_NHIEM_VU_NHAT.md](BAO_CAO_DONG_GOI_NHIEM_VU_NHAT.md).

> **Cập nhật sau giai đoạn khắc phục 1–2 (15/11/2026):** số liệu hiện hành là **87 test, 87 pass** (`mvn clean test`, chạy trên bản sao ngoài workspace), workspace **84 file** (thêm 4 lớp test: `Th5JwtClaimsAndRoleIntegrationTest`, `BookingTimeZoneUnitTest`, `Th5OwnerFailClosedIntegrationTest`, `Th5LegacyOwnerModeIntegrationTest`). Chính sách chủ sân đã chuyển sang **fail-closed** (mặc định `BOOKING_CHUSAN_WITHOUT_OWNER_INFO=false`); sửa lỗ hổng role lạ xem toàn bộ đơn; cố định múi giờ `Asia/Ho_Chi_Minh`. Các con số 57 test / 80 file bên dưới là số liệu **tại thời điểm** của từng vòng, giữ lại làm lịch sử. Chi tiết: `BAO_CAO_HOAN_THANH_NHAT_TH1_TH7.md` mục 7–9.

> **Cập nhật sau giai đoạn khắc phục 3 (TH4, 15/11/2026):** đã kiểm thử trên **MySQL 8.4.3** thử nghiệm (87/87 test, 2 instance cùng CSDL, `FOR UPDATE` thật, V2 chạy lại an toàn — `bang-chung-kiem-thu/th4-mysql-8.4.3.txt`); **chưa** kiểm thử MySQL 8.0 / MySQL của nhóm. Test index đã tương thích H2/MySQL. Chờ Hùng: tên CSDL, quy trình migration, `ddl-auto`, user/quyền, SSL. TH4 vẫn **PARTIAL**.

## Kết luận

**CÓ THỂ ĐƯA LÊN NHÁNH CỦA NHẬT, NHƯNG CÒN HẠN CHẾ CẦN GHI NHẬN.**

- Không có lỗi nghiêm trọng. `mvn clean package`: **57/57 test pass**.
- Không có `.env`, secret dùng chung, mật khẩu, token còn hiệu lực, thông tin cá nhân hay mã nguồn của thành viên khác.
- TH1, TH2, TH3, TH6: **DONE**. TH4, TH5, TH7: **PARTIAL**, do phụ thuộc MySQL thật, Pitch Service và Eureka Server của nhóm (mục 5).

## 1. Trạng thái workspace

| Mục | Kết quả |
|---|---|
| Cấu trúc | `README.md`, `.env.example`, `.gitignore`, `.gitattributes`, `docs/` (11 file), `pitchos-booking-service/` (65 file) — tổng **80 file**, cộng `.vscode/` cục bộ (bị `.gitignore` loại) |
| Không chứa | `pitchos-auth-service/`, `pitchos-pitch-service/`, `pitchos-database/`, `NHIEM_VU_*`, báo cáo của Linh, Hùng, Hoàng, `docker-compose.yml`, `.env`, `target/` |
| Build độc lập | Không có module Maven nội bộ; dependency lấy từ Maven Central; build thành công khi chỉ còn workspace này |
| Hợp đồng tích hợp | `docs/HOP_DONG_API_TICH_HOP.md` (Pitch `GET /api/v1/pitches/{id}`, cấu trúc JWT của Auth, MySQL, Eureka) |

## 2. Build và test

| Lần chạy | Thư mục | Kết quả |
|---|---|---|
| Rà soát workspace đầy đủ | `C:\nhom13\pitchos-booking-service` (bản cũ) | 57/57, BUILD SUCCESS |
| Đóng gói bản sạch | `ban-ban-giao-nhat\pitchos-booking-service` | 57/57, BUILD SUCCESS |
| **Sau khi thu gọn (hiện tại)** | `C:\nhom13\pitchos-booking-service` | **57/57, BUILD SUCCESS (22,3 s)**, sau đó `mvn clean` xóa `target/` |

**Lỗi phát hiện và sửa khi rà soát (phần Booking):** `POST` với `Content-Type: text/plain` trả 500, đúng ra là 415, vì handler `Exception` chung nuốt mất ngoại lệ chuẩn của Spring.
- Đã viết test tái hiện `springClientErrors_keepTheirStatus`. Lần chạy đầu FAIL: 500 thay vì 415.
- Đã sửa `GlobalExceptionHandler`: giữ mã 4xx của `org.springframework.web.ErrorResponse`. Test pass.

## 3. Bảo mật

| Kiểm tra | Kết quả |
|---|---|
| `.gitignore` | Loại `target/`, `*.jar`, `*.class`, file IDE (`.idea/`, `.vscode/`, `.settings/`…), `*.log`, `logs/`, `.env`, `.env.*` (trừ `.env.example`), `*.pem/*.key/*.p12/*.jks/*.keystore`, `application-local/secret*.properties`, `credentials*.json` |
| `.env` | Không có trong workspace |
| `.env.example` | Chỉ có placeholder; `JWT_SECRET=<secret-base64-giong-auth-service>` |
| `application.properties` | `JWT_SECRET` **bắt buộc**, không có giá trị mặc định; `MYSQL_PASSWORD` mặc định rỗng. Người dùng tự đặt `JWT_SECRET` trùng Auth khi chạy tích hợp. Repo không tạo secret dùng chung |
| Test | `jwt.secret` riêng cho test, sinh ngẫu nhiên, không trùng secret của nhóm |
| Postman | Không có token còn hiệu lực (3 token mẫu ký bằng secret của nhóm, hạn 07/2027, đã bị xóa). `tokenExpired` hết hạn 01/01/2026 được giữ cho ca 401. Mật khẩu demo thành biến `{{demoPassword}}` để trống. Thư mục 0 tự lấy token qua Auth |
| Quét toàn bộ | 0 file chứa secret dùng chung; 0 mật khẩu; 0 email thật (chỉ mẫu `@example.com`); 0 `.env`, khóa, chứng chỉ |
| Log bằng chứng | Chỉ có độ dài token và payload demo, không có token đầy đủ |
| Chạy thử jar đã làm sạch (H2 nạp lúc chạy, Pitch giả lập) | Thiếu `JWT_SECRET` → dừng khi khởi động (`Could not resolve placeholder 'JWT_SECRET'`). Có `.env` → đọc đúng cổng và secret, `POST /api/v1/bookings` tạo đơn `cho_duyet`, token ký bằng secret khác → 401 |

## 4. README và hướng dẫn

- `README.md` (gốc):
  - phạm vi TH1–TH7 kèm trạng thái
  - công nghệ, cấu trúc, yêu cầu môi trường
  - cấu hình `.env`, build và test, chạy (MySQL, Pitch, Booking, PowerShell)
  - kết nối Auth, Pitch, MySQL, Eureka
  - Postman, giới hạn
  - Ghi rõ Booking **không chạy độc lập hoàn toàn**.
- `pitchos-booking-service/README.md`: đặc tả API, kịch bản demo, biến môi trường. Đã bỏ link tới tài liệu không còn trong workspace.
- `docs/README.md`: mục lục. Ghi chú rằng các báo cáo đầu được viết trong workspace đầy đủ, nên các đoạn nhắc tới Auth, Pitch, Database là bối cảnh lúc làm việc.

## 5. Hạn chế (không ghi DONE khi chưa có bằng chứng)

| Buổi | Trạng thái | Còn thiếu | Nguyên nhân |
|---|---|---|---|
| TH4 | PARTIAL | đã kiểm thử trên **MySQL 8.4.3** thử nghiệm (87/87 test, 2 instance cùng CSDL, `FOR UPDATE` thật, V2 chạy lại an toàn — `bang-chung-kiem-thu/th4-mysql-8.4.3.txt`); **chưa** kiểm thử MySQL 8.0 / MySQL của nhóm | Chờ Hùng xác nhận tên CSDL, phiên bản MySQL, quy trình migration; chạy trên MySQL của nhóm |
| TH5 | PARTIAL | Chủ sân chỉ duyệt đơn sân mình: đã cài theo nguyên tắc **fail-closed** (mặc định chặn khi không xác minh được) và test với `ownerId` giả lập, nhưng Pitch thật chưa trả `ownerId` nên chủ sân hiện bị chặn; chủ sân xem hoặc duyệt cần Pitch đang chạy | Phụ thuộc Pitch Service |
| TH7 | PARTIAL | Đã kiểm chứng với Eureka Server **tạm** (Pitch đăng ký thủ công). Nhóm chưa có Eureka Server; Pitch chưa có Eureka client; chế độ OpenFeign chỉ gọi URL cố định | Phụ thuộc hạ tầng chung và Pitch |

Các điểm chờ nhóm thống nhất (không chặn việc push):
- port 8083 hay 8081;
- tên CSDL `pitchos_booking_db` hay `booking_db`;
- bảng phân công tuần ghi Linh = booking, Nhật = pitch.

## 6. Việc cần làm trước khi đưa lên nhánh

1. Chờ trưởng nhóm tạo repository và nhánh. Tự thực hiện Git; kiểm tra `git status` **không có** `.env`, `target/`, `.vscode/`.
2. Thống nhất vị trí trong repository chung: gốc nhánh hay thư mục con.
3. Khi chạy tích hợp: tạo `.env` từ `.env.example`, đặt `JWT_SECRET` trùng Auth (hỏi người phụ trách Auth), đặt `demoPassword` trong Postman lúc chạy và không lưu.
4. Chuyển TH4, TH5, TH7 sang DONE khi có MySQL thật, Pitch có `ownerId` và Eureka client, nhóm có Eureka Server.
5. Xóa file sao lưu tạm khi không cần nữa (đường dẫn ở mục 8 báo cáo đóng gói).
