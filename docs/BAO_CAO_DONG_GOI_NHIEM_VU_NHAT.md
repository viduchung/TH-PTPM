# BÁO CÁO ĐÓNG GÓI PHẦN ĐÓNG GÓP CỦA NHẬT — BOOKING SERVICE TH1–TH7

> Cập nhật: 15/11/2026 · Workspace: `C:\nhom13`
> Mục tiêu: `C:\nhom13` chỉ còn phần Booking TH1–TH7 của Nhật, để Nhật tự đưa lên **nhánh riêng** trong repository chung của nhóm.
> Không chạy lệnh Git nào: không `init`, `add`, `commit`, `push`, `pull`, `checkout`; không tạo nhánh; không kết nối GitHub.

> **Cập nhật sau giai đoạn khắc phục 1–2 (15/11/2026):** số liệu hiện hành là **87 test, 87 pass** (`mvn clean test`, chạy trên bản sao ngoài workspace), workspace **84 file** (thêm 4 lớp test: `Th5JwtClaimsAndRoleIntegrationTest`, `BookingTimeZoneUnitTest`, `Th5OwnerFailClosedIntegrationTest`, `Th5LegacyOwnerModeIntegrationTest`). Chính sách chủ sân đã chuyển sang **fail-closed** (mặc định `BOOKING_CHUSAN_WITHOUT_OWNER_INFO=false`); sửa lỗ hổng role lạ xem toàn bộ đơn; cố định múi giờ `Asia/Ho_Chi_Minh`. Các con số 57 test / 80 file bên dưới là số liệu **tại thời điểm** của từng vòng, giữ lại làm lịch sử. Chi tiết: `BAO_CAO_HOAN_THANH_NHAT_TH1_TH7.md` mục 7–9.

> **Cập nhật sau giai đoạn khắc phục 3 (TH4, 15/11/2026):** đã kiểm thử trên **MySQL 8.4.3** thử nghiệm (87/87 test, 2 instance cùng CSDL, `FOR UPDATE` thật, V2 chạy lại an toàn — `bang-chung-kiem-thu/th4-mysql-8.4.3.txt`); **chưa** kiểm thử MySQL 8.0 / MySQL của nhóm. Test index đã tương thích H2/MySQL. Chờ Hùng: tên CSDL, quy trình migration, `ddl-auto`, user/quyền, SSL. TH4 vẫn **PARTIAL**.

## Kết luận

`C:\nhom13` hiện **chỉ chứa phần Booking TH1–TH7 của Nhật**:
- 80 file, cộng `.vscode/settings.json` là cấu hình VS Code trên máy, đã bị `.gitignore` loại.
- Không có mã nguồn hay tài liệu riêng của thành viên khác.
- Không có `.env`, secret dùng chung, mật khẩu hay token còn hiệu lực.

`mvn clean package` chạy **57/57 test pass, BUILD SUCCESS**, sau đó `target/` đã được xóa bằng `mvn clean`.

Booking **không chạy độc lập hoàn toàn**: khi chạy thật cần MySQL, Pitch Service và `JWT_SECRET` trùng Auth Service. Chỉ bộ test tự động là chạy độc lập.

## 1. Cấu trúc workspace cuối cùng

```text
C:\nhom13
├── README.md                    hướng dẫn cài đặt, cấu hình, build, chạy, kiểm thử
├── .env.example                 biến môi trường mẫu (chỉ placeholder)
├── .gitignore                   loại .env, target/, file build, IDE, log, khóa/chứng chỉ
├── .gitattributes               chuẩn hóa xuống dòng (text=auto)
├── docs/
│   ├── README.md                mục lục tài liệu
│   ├── BAO_CAO_PHAN_TICH_PTPM_CU.md · BAO_CAO_HIEN_TRANG_NHIEM_VU_NHAT.md · KE_HOACH_TRIEN_KHAI_NHAT_TH1_TH7.md
│   ├── BAO_CAO_HOAN_THANH_NHAT_TH1_TH7.md · BAO_CAO_RA_SOAT_TRUOC_KHI_PUSH.md · BAO_CAO_DONG_GOI_NHIEM_VU_NHAT.md
│   ├── HOP_DONG_API_TICH_HOP.md hợp đồng API với Pitch, Auth, MySQL, Eureka
│   └── bang-chung-kiem-thu/     3 log kiểm thử thực tế
└── pitchos-booking-service/
    ├── pom.xml · README.md
    ├── src/main/                43 file Java + application.properties
    ├── src/test/                15 file Java + application-test.properties (57 test)
    ├── db/V2__booking_th1_th7_mysql.sql
    ├── demo/MockPitchService.java   Pitch giả lập để tập dượt và kiểm thử
    └── postman/PitchOS-Booking.postman_collection.json
```

Không có file Postman *environment* riêng (bản PTPM cũng không có). Mọi biến nằm trong collection, đã làm sạch (mục 4).

## 2. Kiểm tra dependency (trước khi loại file)

| Câu hỏi | Kết quả kiểm tra |
|---|---|
| Booking build được khi chỉ còn workspace này? | **Có.** `mvn clean package` thành công trong workspace đã dọn (57/57 test) |
| Có module Maven nội bộ hoặc file dùng chung? | **Không.** Parent `spring-boot-starter-parent:3.2.5` (`relativePath` rỗng), BOM `spring-cloud-dependencies:2023.0.1`, mọi thư viện lấy từ Maven Central. Không import `com.pitchos.auth`/`com.pitchos.pitch` |
| API cần từ Auth và Pitch đã mô tả đủ? | **Có.** `docs/HOP_DONG_API_TICH_HOP.md` mô tả `GET /api/v1/pitches/{id}` (các trường, cách xử lý lỗi), cấu trúc JWT (HS256, `userId`, `role`, `exp`), `POST /api/v1/auth/login` và `register` (dùng trong Postman), CSDL, tên dịch vụ Eureka |
| Cấu hình MySQL, `JWT_SECRET`, Pitch URL, Eureka rõ ràng? | **Có.** `.env.example` và README mục 5 và 8. `JWT_SECRET` bắt buộc, **người dùng tự đặt trùng secret của Auth**, repo không tạo hay chứa secret dùng chung |

**Dependency khi tích hợp:**

| Loại | Thành phần |
|---|---|
| Maven (tải độc lập) | Spring Boot 3.2.5 (web, data-jpa, validation, security, webflux), Spring Cloud 2023.0.1 (openfeign, netflix-eureka-client, loadbalancer), mysql-connector-j, jjwt 0.11.5, lombok |
| Bắt buộc khi chạy thật | **MySQL 8** (CSDL riêng của Booking); **Pitch Service** (`GET /api/v1/pitches/{id}`); **`JWT_SECRET` trùng Auth** (Booking không gọi HTTP tới Auth) |
| Tùy chọn | **Eureka Server** (TH7); **Auth Service** để lấy token khi kiểm thử bằng Postman |
| Chỉ khi test | spring-boot-starter-test, spring-security-test, h2 (scope `test`); Pitch giả lập trong test (JDK HttpServer) |

## 3. Giữ lại và loại bỏ

Workspace trước khi dọn có **158 file** (không tính `target/` và thư mục tạm `ban-ban-giao-nhat/`).
- **Giữ 74 file**: 65 file Booking, 8 tài liệu và bằng chứng của Nhật, `.vscode/settings.json` (cục bộ).
- **Loại 84 file**.
- **Thêm 7 file mới**: `README.md`, `.env.example`, `.gitignore`, `.gitattributes` (bản mới), `docs/README.md`, `docs/HOP_DONG_API_TICH_HOP.md`, báo cáo này.
- Kết quả: **80 file** (cộng `.vscode`).

**Không có file Booking hoặc tài liệu của Nhật bị mất:** trước khi loại, mình xác nhận từng nhóm hoặc giống hệt bản PTPM gốc, hoặc đã có bản thay thế đã làm sạch.

| Bị loại (số file) | Lý do | Bản gốc còn ở |
|---|---|---|
| `pitchos-auth-service/` (26) | Mã nguồn Auth của Hoàng | `Downloads\PTPM-main` (giống hệt) |
| `pitchos-pitch-service/` (28) | Mã nguồn Pitch của Linh | `Downloads\PTPM-main` (giống hệt) |
| `pitchos-database/` (4) | Script CSDL chung của Hùng | `Downloads\PTPM-main` (giống hệt) |
| `NHIEM_VU_HOANG/LINH/HUNG.md` (3) | Nhiệm vụ của thành viên khác | `Downloads\PTPM-main` |
| `NHIEM_VU_NHAT.md` (1) | Phân công bài kiểm tra số 1 cũ (8083/OpenFeign) có bảng điểm cả nhóm, không phải TH1–TH7 | `Downloads\PTPM-main` |
| `README.md` tổng, `CONG_NGHE_SU_DUNG.md`, `docker-compose.yml` (3) | Tài liệu và hạ tầng chung của nhóm (compose mount script SQL của Hùng) | `Downloads\PTPM-main` |
| `docs/BAO_CAO_CHUONG_1_LINH`, `_3_HUNG`, `_4_HOANG`, `_5_HOANG`, `DEMO_VA_KIEM_THU`, `MUC_LUC_BAO_CAO_WORD` (6) | Báo cáo của thành viên khác hoặc của nhóm | `Downloads\PTPM-main` |
| `docs/BAO_CAO_CHUONG_2_NHAT.md`, `docs/tham-khao-ptpm-cu/` (5) | Báo cáo Chương 2 của bài kiểm tra cũ, không phải TH1–TH7 | `Downloads\PTPM-main`, `C:\New folder (2)\PTPM\docs` |
| `.env` (1) | Chứa mật khẩu và secret | `Downloads\PTPM-main` |
| `.env.example`, `.gitignore`, `.gitattributes` cũ (3) | Bản cũ chứa giá trị mặc định thật hoặc thiếu quy tắc → thay bằng bản mới | `Downloads\PTPM-main` |
| `.github/modernize/` (3) | File do extension VS Code tạo | `Downloads\PTPM-main` |
| `pitchos-booking-service/application.properties` ở gốc service (1) | Bản cũ trùng lặp, chứa mật khẩu và secret mặc định, lại được Spring ưu tiên hơn `src/main/resources` | `Downloads\PTPM-main` |
| Bản Booking và `docs/` **chưa làm sạch** của workspace (thay bằng bản đã làm sạch, cùng tên) | Còn secret dùng chung, token Postman, mật khẩu | Sao lưu tạm (mục 8) |
| `*/target/` | Sản phẩm build | Tạo lại bằng `mvn package` |

## 4. File đã chỉnh sửa so với nguồn ban đầu (Booking trong PTPM gốc)

`diff -rq` với `Downloads\PTPM-main\pitchos-booking-service`: **16 sửa, 1 loại, 26 mục mới**.

**16 file sửa:**

| File | Nội dung sửa |
|---|---|
| `pom.xml` | Thêm webflux, eureka-client; test: spring-security-test, h2 (TH2, TH7) |
| `src/main/resources/application.properties` | Cấu hình WebClient, timeout, retry, Eureka, quyền chủ sân. Bỏ giá trị mặc định của `MYSQL_PASSWORD` và `JWT_SECRET` (bắt buộc). Đọc `.env` qua `spring.config.import` |
| `controller/BookingController` | API GET danh sách (phân trang, lọc), GET/PUT/DELETE `/{id}`, `/cancel`, `/quote`, Idempotency-Key (TH1, TH3, TH6) |
| `dto/BookingRequest`, `BookingResponse`, `ErrorResponse`, `PitchResponseDTO` | Validation, trường bổ sung (`pricePerHour`, `updatedAt`, `path`, `details`, `ownerId`); không bỏ trường cũ |
| `entity/Booking`, `BookingStatus` | Index, UNIQUE idempotency, `@Version`, `updated_at`, trạng thái `da_huy` (TH4, TH1) |
| `exception/GlobalExceptionHandler` | Lỗi thống nhất, 403/409/503, giữ mã 4xx của Spring (TH3) |
| `repository/BookingRepository` | Specification, truy vấn trùng bỏ qua `da_huy`, truy vấn loại trừ chính đơn đang sửa |
| `security/JwtAuthenticationFilter`, `SecurityConfig` | 401/403 JSON, phân biệt token hết hạn/sai, ma trận quyền mới (TH5) |
| `service/BookingService` | Điều phối: gọi Pitch ngoài transaction, idempotency, quyền sở hữu |
| `README.md` | Thêm mục 7 (TH1–TH7), bỏ link tới tài liệu không còn trong workspace, hướng dẫn `.env` và token |
| `postman/PitchOS-Booking.postman_collection.json` | Thêm thư mục 4 (TH1–TH7). Xóa token còn hiệu lực và mật khẩu demo. Thêm đăng nhập admin, đăng ký user khác. Token sai chữ ký sinh lúc chạy |

**Mục mới (26):**
- `client/` (6 file)
- `config/`
- `controller/BookingSort`
- `dto/PageResponse`, `QuoteResponse`
- `entity/PitchLock`
- `exception/` (3 file)
- `repository/PitchLockRepository`
- `security/CurrentUser`, `JsonSecurityHandlers`
- `service/` (7 file)
- `db/`
- `src/test/` (16 file: 15 Java + `application-test.properties`, với secret **riêng cho test**, sinh ngẫu nhiên)

**Không sửa:**
- `client/PitchClient`, `CustomErrorDecoder` (OpenFeign cũ, vẫn dùng được)
- `security/JwtUtil`
- `dto/UpdateStatusRequest`
- `exception/BadRequestException`, `ResourceNotFoundException`
- lớp `Application`
- `demo/MockPitchService.java`

## 5. Kết quả build và test (workspace sau khi dọn)

```text
cd C:\nhom13\pitchos-booking-service && mvn -B clean package
Tests run: 57, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS (22,3 s)
```

| Lớp test | Số test | Lớp test | Số test |
|---|---|---|---|
| Th1BookingCrud | 10 | Th5Security | 8 |
| Th2Th6Pitch | 10 | Th5StrictOwnerPolicy | 1 |
| Th2PitchServiceDown | 1 | Th6IdempotencyConcurrency | 6 |
| Th3PagingFilterValidation | 8 | Th7LoadBalancer | 1 |
| Th4BookingRepositoryJpa | 6 | FeignCompatibility / BookingRulesUnit | 1 / 5 |

Kết quả giống hệt các lần chạy trước: workspace đầy đủ 57/57, thư mục bàn giao 57/57. Sau khi test xong, `mvn clean` đã xóa `target/` (hiện không còn thư mục `target` nào).

Kiểm thử thực tế với tiến trình thật (log ở `bang-chung-kiem-thu/`) được thực hiện trước khi đóng gói. Riêng lần chạy thử jar đã làm sạch (thiếu `JWT_SECRET` → dừng khi khởi động; có `.env` → đặt sân được, token sai secret → 401) được ghi ở `BAO_CAO_RA_SOAT_TRUOC_KHI_PUSH.md` mục 3.

## 6. Kiểm tra bảo mật và file thừa (quét toàn bộ workspace)

| Kiểm tra | Kết quả |
|---|---|
| `.env`, `*.pem`, `*.key`, `*.jks`, `*.p12` | Không có |
| `.env.example` | Chỉ có placeholder (`<ten-dang-nhap-mysql>`, `<mat-khau-mysql>`, `<secret-base64-giong-auth-service>`) |
| Secret JWT dùng chung của nhóm | Không có (0 file) |
| Token JWT | 1 token: `tokenExpired` trong Postman, **hết hạn 01/01/2026**, giữ cho ca 401 "token hết hạn" |
| Mật khẩu demo / CSDL | Không có |
| Email | Chỉ có mẫu `@example.com` trong Postman |
| `jwt.secret` trong `src/test/resources/application-test.properties` | Secret **riêng cho test**, sinh ngẫu nhiên, không dùng ở môi trường nào |
| Mã nguồn hoặc nhiệm vụ của thành viên khác | Không có (không có `package com.pitchos.auth/pitch`, không có `NHIEM_VU_*`, không có báo cáo của Linh, Hùng, Hoàng) |
| `target/`, file build, file tạm | Không có. `.vscode/` (cục bộ) đã bị `.gitignore` loại |

## 7. Hạn chế còn tồn tại (PARTIAL)

| Buổi | Đã có bằng chứng | Còn thiếu |
|---|---|---|
| TH4 | đã kiểm thử trên **MySQL 8.4.3** thử nghiệm (87/87 test, 2 instance cùng CSDL, `FOR UPDATE` thật, V2 chạy lại an toàn — `bang-chung-kiem-thu/th4-mysql-8.4.3.txt`); **chưa** kiểm thử MySQL 8.0 / MySQL của nhóm | Chạy V2 + Postman trên MySQL của nhóm sau khi Hùng xác nhận tên CSDL và quy trình migration |
| TH5 | JWT thật từ Auth, 401/403, quyền sở hữu đơn của user, admin | Pitch **chưa trả `ownerId`**: ở mặc định fail-closed chủ sân bị chặn (chỉ admin duyệt); chưa xác minh quyền sở hữu với Pitch thật |
| TH7 | Eureka Server tạm + 2 Pitch thật: chia tải 5/5, failover 6/6, 2 Booking đăng ký | Nhóm chưa có Eureka Server; Pitch chưa có Eureka client; chế độ OpenFeign chỉ gọi URL cố định |

## 8. Việc cần làm trước khi đưa lên nhánh của Nhật

1. Chờ trưởng nhóm tạo repository chung và nhánh của Nhật, rồi tự thực hiện Git trong `C:\nhom13`.
2. Sau `git add`, chạy `git status` và kiểm tra **không có** `.env`, `target/`, `.vscode/`.
3. Thống nhất với nhóm vị trí phần này trong repository chung: đặt ở gốc nhánh hay trong một thư mục con như `booking/`. README dùng đường dẫn tương đối nên chạy được ở cả hai cách.
4. Khi chạy tích hợp: tạo `.env` từ `.env.example`. `JWT_SECRET` lấy từ người phụ trách Auth (phải trùng). Đặt `demoPassword` trong Postman lúc chạy và không lưu vào collection.
5. Thống nhất port (8083/8081) và tên CSDL (`pitchos_booking_db`/`booking_db`), đổi bằng `.env`.
6. Để chuyển TH4, TH5, TH7 sang DONE: kiểm thử với MySQL 8 thật; Pitch bổ sung `ownerId` và Eureka client; nhóm có Eureka Server.
7. **Sao lưu tạm** trạng thái trước khi dọn (237 file, có cả `.env` với giá trị dev): `C:\Users\admin\AppData\Local\Temp\claude\dbf8402525c742d8\b13520bb-f252-47f4-ac9d-e14ed90bcca9\scratchpad\sao-luu\nhom13-truoc-khi-don-dep.tar`. Thư mục tạm có thể bị hệ thống dọn; bản gốc PTPM vẫn còn ở `Downloads\PTPM-main` và `C:\New folder (2)\PTPM`. Có thể xóa file sao lưu này khi không cần nữa.
