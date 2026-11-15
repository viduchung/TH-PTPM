# BÁO CÁO HOÀN THÀNH NHIỆM VỤ NHẬT TH1–TH7 — BOOKING SERVICE

> Người thực hiện: Nhật · Ngày: 15/11/2026 · Workspace: `C:\nhom13`
> Cơ sở: PTPM cũ, bản `Downloads\PTPM-main` (lý do chọn: [BAO_CAO_PHAN_TICH_PTPM_CU.md](BAO_CAO_PHAN_TICH_PTPM_CU.md)).
> Hiện trạng trước khi làm: [BAO_CAO_HIEN_TRANG_NHIEM_VU_NHAT.md](BAO_CAO_HIEN_TRANG_NHIEM_VU_NHAT.md) · Kế hoạch: [KE_HOACH_TRIEN_KHAI_NHAT_TH1_TH7.md](KE_HOACH_TRIEN_KHAI_NHAT_TH1_TH7.md)

## 0. Bảng nghiệm thu

| Buổi | Trạng thái | Kiểm thử (bằng chứng) | File chính thay đổi |
|---|---|---|---|
| TH1 – CRUD, RESTful | **DONE** | 10 test tích hợp + E2E 3 service thật (mục 7.2) | `BookingController`, `BookingService`, `BookingWriter`, `BookingStatus` |
| TH2 – WebClient | **DONE** | 11 test (Pitch tắt thật, 404, 500, 503, timeout, dữ liệu sai) + E2E tắt Pitch thật → 503 | `client/WebClientPitchGateway`, `PitchClientConfig`, `PitchGateway`, `FeignPitchGateway` |
| TH3 – REST nâng cao | **DONE** | 8 test (phân trang, lọc, sắp xếp, validation, lỗi thống nhất, 415/405) + E2E | `BookingController`, `BookingSort`, `BookingSearchCriteria`, `BookingTimeRules`, `GlobalExceptionHandler`, `ErrorResponse` |
| TH4 – MySQL / JPA | **PARTIAL** | Trên **MySQL 8.4.3** thử nghiệm: 87/87 test; 2 instance Booking cùng CSDL (20 request đồng thời → 1 đơn, `FOR UPDATE` thật, rollback vi phạm UNIQUE, khóa lạc quan); V2 chạy lại an toàn (3 lần liên tiếp, dữ liệu giữ nguyên) và báo lỗi rõ khi không tương thích. Trên H2: 87/87. **Chưa** kiểm thử MySQL 8.0 / MySQL của nhóm; chờ Hùng xác nhận tên CSDL và quy trình migration | `Booking`, `PitchLock`, `BookingRepository`, `PitchLockRepository`, `BookingWriter`, `db/V2__booking_th1_th7_mysql.sql`, `Th4BookingRepositoryJpaTest` |
| TH5 – JWT / Security | **PARTIAL** | 29 test TH5 (token thiếu `userId`/`role`, hết hạn, sai chữ ký trên 9 endpoint, role lạ, chủ sân **fail-closed** ở mặc định, chế độ tương thích) + E2E với **token thật của Auth Service** (PTPM cũ). Quyền chủ sân **chưa xác minh với Pitch thật** vì Pitch chưa trả `ownerId` → chủ sân hiện bị chặn | `security/*`, `BookingAccessPolicy`, `BookingService` |
| TH6 – Consumer/Provider, idempotency | **DONE** (phía Booking) | 6 test idempotency/đồng thời + 10 test retry/lỗi Provider + E2E. Đã kiểm chứng test bắt được race condition (mục 7.3) | `IdempotencyKeys`, `BookingWriter`, `PitchLockService`, `WebClientPitchGateway`, `QuoteResponse` |
| TH7 – Eureka, LoadBalancer | **PARTIAL** | 1 test LoadBalancer (2 instance thật) + E2E với **Eureka Server tạm**: 2 Booking đăng ký, 10 lời gọi chia 5/5, failover 6/6. Pitch Service chưa có Eureka client nên phải đăng ký thủ công qua REST API của Eureka; repo chưa có Eureka Server của nhóm | `pom.xml`, `PitchClientConfig`, `application.properties` |

**Tổng test tự động:** `mvn clean test` → **87 test, 0 lỗi, 0 thất bại, BUILD SUCCESS** (sau giai đoạn khắc phục 2, 15/11/2026; chạy trên bản sao ngoài workspace). Báo cáo này **không** tuyên bố hoàn thành 100%: TH4, TH5, TH7 còn phần bị chặn bởi môi trường hoặc service khác (mục 9).

## 1. Tổng quan và kiến trúc Booking Service

```text
Client ──JWT──► BookingController ──► BookingService (điều phối, KHÔNG giữ transaction khi gọi mạng)
                                        ├─► PitchGateway ──► WebClientPitchGateway (mặc định) ──HTTP──► Pitch Service
                                        │                 └► FeignPitchGateway (OpenFeign cũ, pitch.client.type=feign)
                                        │      (timeout, retry giới hạn, LoadBalancer khi bật discovery)
                                        ├─► BookingAccessPolicy (quyền sở hữu)
                                        ├─► BookingTimeRules (validation giờ, tính tiền)
                                        ├─► PitchLockService (tạo dòng khóa sân)
                                        └─► BookingWriter (@Transactional READ_COMMITTED: khóa sân FOR UPDATE → kiểm tra → lưu)
                                                 └─► BookingRepository / PitchLockRepository ──► pitchos_booking_db
```

- Database-per-Service: Booking chỉ truy cập `pitchos_booking_db`. `user_id` và `pitch_id` là logical ID, **không có Foreign Key** sang DB khác (test `schemaHasIndexesAndNoForeignKeys` kiểm tra 0 FK). Thông tin sân luôn lấy qua API của Pitch Service.
- Booking ↔ Pitch: Booking là **Consumer**, Pitch là **Provider** của `GET /api/v1/pitches/{id}` (hợp đồng có sẵn, không đổi).

## 2. File đã tạo và chỉnh sửa

Chỉ `pitchos-booking-service/`, `docs/` và phần **bổ sung** cuối `.gitignore` (quy tắc loại trừ `.env`/bí mật, thêm khi rà soát trước push theo yêu cầu) thay đổi. `diff -rq` so với bản gốc xác nhận Auth, Pitch, `pitchos-database/`, README tổng, `.env*` và `docker-compose.yml` **giữ nguyên**. Các bản PTPM gốc trên máy không bị sửa.

**Sửa (14 file code + README + Postman):**
- `pom.xml`: thêm `spring-boot-starter-webflux`, `spring-cloud-starter-netflix-eureka-client`; test: `spring-security-test`, `h2`.
- `src/main/resources/application.properties`
- `controller/BookingController.java`
- `dto/BookingRequest.java`, `BookingResponse.java`, `ErrorResponse.java`, `PitchResponseDTO.java`
- `entity/Booking.java`, `BookingStatus.java`
- `exception/GlobalExceptionHandler.java`
- `repository/BookingRepository.java`
- `security/JwtAuthenticationFilter.java`, `SecurityConfig.java`
- `service/BookingService.java`
- `README.md` (thêm mục 7)
- `postman/PitchOS-Booking.postman_collection.json` (thêm thư mục "4. Bổ sung TH1–TH7", 14 request; 4 thư mục cũ giữ nguyên)

**Tạo mới (main, 25):**
- `client/`: `PitchGateway`, `WebClientPitchGateway`, `FeignPitchGateway`, `PitchClientConfig`, `PitchClientProperties`, `PitchResponseValidator`
- `config/TimeConfig`
- `controller/BookingSort`
- `dto/PageResponse`, `QuoteResponse`
- `entity/PitchLock`
- `exception/ForbiddenException`, `ConflictException`, `PitchServiceUnavailableException`
- `repository/PitchLockRepository`
- `security/CurrentUser`, `JsonSecurityHandlers`
- `service/BookingAccessPolicy`, `BookingCommands`, `BookingSearchCriteria`, `BookingTimeRules`, `BookingWriter`, `IdempotencyKeys`, `PitchLockService`
- `db/V2__booking_th1_th7_mysql.sql`

**Tạo mới (test, 20 — cập nhật sau giai đoạn khắc phục 2):**
- `support/FakePitchServer`, `TestTokens`, `IntegrationTestBase`
- `client/WebClientPitchGatewayTestAccess`
- 15 lớp test (mục 7.1), trong đó 4 lớp thêm ở giai đoạn khắc phục 1–2: `Th5JwtClaimsAndRoleIntegrationTest`, `BookingTimeZoneUnitTest`, `Th5OwnerFailClosedIntegrationTest`, `Th5LegacyOwnerModeIntegrationTest`
- `src/test/resources/application-test.properties`

**Tài liệu:**
- `docs/BAO_CAO_PHAN_TICH_PTPM_CU.md`, `BAO_CAO_HIEN_TRANG_NHIEM_VU_NHAT.md`, `KE_HOACH_TRIEN_KHAI_NHAT_TH1_TH7.md`, file này
- `docs/bang-chung-kiem-thu/*.txt`
- `docs/tham-khao-ptpm-cu/` (báo cáo Word Chương 2 và ảnh sơ đồ của Nhật, chép nguyên từ bản PTPM có commit chưa push)

**Tái sử dụng từ PTPM cũ:**
- Entity/Repository và truy vấn trùng lịch của Hùng (mở rộng thêm `da_huy`).
- `PitchClient` + `CustomErrorDecoder` (OpenFeign, **không xóa**).
- Định dạng `ErrorResponse`.
- `JwtUtil` (không đổi) và hợp đồng JWT của Auth.
- 3 API cũ: giữ nguyên URL, quyền, mã trạng thái và body.
- Postman collection cũ.

## 3. TH1 – CRUD và REST API

| Method | Endpoint | Quyền | Kết quả |
|---|---|---|---|
| POST | `/api/v1/bookings` | user | 201 + `Location` (cũ, bổ sung `Location`) |
| GET | `/api/v1/bookings/my-bookings` | user | 200 danh sách (cũ, không đổi) |
| GET | `/api/v1/bookings` | đã đăng nhập | 200 phân trang (mới, TH3) |
| GET | `/api/v1/bookings/{id}` | chủ đơn / chusan / admin | 200, 403, 404 |
| PUT | `/api/v1/bookings/{id}` | user, chủ đơn | 200; chỉ đơn `cho_duyet`; tính lại tiền |
| PATCH | `/api/v1/bookings/{id}/cancel` | user, chủ đơn / admin | 200 → `da_huy` (hủy mềm) |
| DELETE | `/api/v1/bookings/{id}` | user, chủ đơn / admin | 204, hủy mềm, **không xóa dòng** (giữ lịch sử) |
| PATCH | `/api/v1/bookings/{id}/status` | chusan / admin | 200 `da_duyet`/`tu_choi` (cũ) |

Trạng thái:
- `cho_duyet` → `da_duyet` | `tu_choi` (chủ sân/admin)
- `cho_duyet` | `da_duyet` → `da_huy` (khách trước giờ đá, hoặc admin)
- `tu_choi` và `da_huy` là trạng thái cuối, đổi tiếp → 400
- Gửi `cho_duyet` hoặc `da_huy` vào `/status` → 400

## 4. TH2 + TH6 – Giao tiếp Booking (Consumer) → Pitch (Provider)

- **WebClient** (mặc định): timeout kết nối 2s, timeout phản hồi 3s (cấu hình được).
- **Retry có giới hạn:** tối đa 2 lần, backoff 200ms tăng dần. **Chỉ** áp dụng cho GET (không đổi dữ liệu) và **chỉ** khi lỗi tạm thời (mất kết nối, timeout, 502/503/504). Không retry 404 và 500. Có trần thời gian chờ tổng.
- **Phân loại lỗi (thống nhất giữa WebClient và Feign):**

| Tình huống | Mã trả về |
|---|---|
| Pitch trả 404 | 404 "Không tìm thấy sân bóng…" |
| Pitch tắt, timeout, 5xx, mã lạ, JSON hỏng, thiếu giá, giá ≤ 0, id lệch | **503** |
| Sân `bao_tri` | 400 |

  Trong mọi trường hợp trên, **không tạo đơn**.
- Lời gọi Pitch nằm **ngoài transaction**, nên Pitch chậm không giữ kết nối CSDL.
- `GET /api/v1/bookings/quote`: báo giá (giá × phút / 60) kèm `available`, không tạo đơn.
- **OpenFeign giữ nguyên:** `PITCH_CLIENT_TYPE=feign` (test `FeignCompatibilityIntegrationTest`).
- **Idempotency:**
  - Header tùy chọn `Idempotency-Key` (1–64 ký tự `[A-Za-z0-9._:-]`).
  - Ràng buộc **UNIQUE (`user_id`, `idempotency_key`)** trong CSDL. Trạng thái xử lý chính là đơn đã lưu: không lưu đơn thì không lưu key, nên lỗi 503 cho phép gửi lại.
  - Gửi lại cùng key và cùng nội dung → **200 + `Idempotent-Replayed: true`**, trả đúng đơn cũ, **không gọi lại Pitch** (vẫn chạy khi Pitch đang lỗi).
  - Cùng key nhưng khác nội dung → **409**.
- **Chống race condition (không chỉ SELECT rồi INSERT):**
  - Mỗi sân có một dòng trong `pitch_locks`. Transaction đặt hoặc sửa đơn chạy `SELECT … FOR UPDATE` trên dòng đó, rồi mới kiểm tra trùng lịch và lưu, ở mức READ_COMMITTED.
  - Cơ chế này đúng cả khi chạy **nhiều instance** Booking. Khóa `synchronized` trong bộ nhớ của bản cũ chỉ đúng với 1 instance, nên đã được thay thế; chức năng chống trùng vẫn giữ nguyên.
  - Request đồng thời cùng key nhưng khác sân sẽ bị ràng buộc UNIQUE chặn, sau đó trả đơn cũ hoặc 409.
- Giới hạn đã biết: nếu đơn đã bị đổi giờ bằng `PUT`, gửi lại request gốc với cùng key → 409, vì nội dung không còn khớp.

## 5. TH3, TH4

- **TH3:**
  - `page` ≥ 0, `size` 1–100.
  - `sort` chỉ theo danh sách trắng (`id`, `createdAt`, `startTime`, `endTime`, `totalPrice`, `status`, `pitchId`), kèm `id` để thứ tự ổn định.
  - Lọc theo `status`, `pitchId`, `userId` (user luôn bị ép về chính mình), `date` hoặc `from`/`to` (theo giờ bắt đầu).
  - Validation: `startTime` ở tương lai, `endTime > startTime`, thời lượng 30 phút – 8 giờ.
  - Lỗi: định dạng chung `{timestamp, status, error, message, path, details}`. Có `details` liệt kê đủ lỗi từng trường. 401/403 cũng trả JSON. Lỗi lạ → 500 với thông điệp chung, chi tiết chỉ ghi log.
- **TH4:**
  - Thêm `price_per_hour` (bản chụp giá lúc đặt), `idempotency_key`, `version` (khóa lạc quan: cập nhật đồng thời → 409), `updated_at`.
  - Thêm 3 index: `(pitch_id, start_time, end_time)`, `(user_id, created_at)`, `status`.
  - Bảng `pitch_locks`.
  - `@Transactional` ở tầng service. `spring.jpa.open-in-view=false`.
  - Script nâng cấp MySQL `db/V2__booking_th1_th7_mysql.sql`: không cố định tên CSDL, **chạy lại an toàn** (kiểm tra `information_schema` + SQL động), kiểm tra cấu trúc trước khi sửa, dừng với mã `V2_LOI_…` nếu không tương thích, không `DROP`/`DELETE`. Đã kiểm thử trên **MySQL 8.4.3** (giai đoạn khắc phục 3, xem mục 7.4). Lý do cần script: `ddl-auto=update` không sửa danh sách ENUM của cột `status` trong bảng tạo từ script của Hùng.

## 6. TH5, TH7

- **TH5:**
  - Token theo đúng hợp đồng Auth: HS256, secret chung, `userId`, `role` ∈ {`user`, `chusan`, `admin`}.
  - `userId` **chỉ lấy từ JWT**. `userId` gửi trong body hoặc query đều bị bỏ qua (có test).
  - 401 phân biệt ba lý do: thiếu token, token sai, token hết hạn.
  - 403 khi sai vai trò hoặc thao tác trên đơn của người khác.
  - Token thiếu `role` → 401; thiếu `userId` → 401 nếu `sub` không phải số (nếu `sub` là số thì dùng làm `userId` — cơ chế dự phòng có sẵn, chờ Hoàng xác nhận).
  - Role không thuộc `user`/`chusan`/`admin` (ví dụ `CUSTOMER`, `guest`) → 403, **không** còn được xem toàn bộ đơn như admin (lỗ hổng phát hiện và sửa ở giai đoạn khắc phục 1).
  - Chủ sân — **fail-closed** (mặc định `BOOKING_CHUSAN_WITHOUT_OWNER_INFO=false`, từ giai đoạn khắc phục 2):
    - Pitch trả `ownerId` trùng `userId` trong JWT → xem chi tiết, duyệt, từ chối được; khác → 403.
    - Pitch không trả `ownerId`, lỗi 5xx, 404, timeout, JSON hỏng, dữ liệu sai → **không** cấp quyền (403 / 404 / 503), đơn không đổi.
    - `GET /api/v1/bookings` của chủ sân → 403 (chưa lọc được theo các sân mà chủ sân sở hữu; chờ Pitch có API này).
    - `true` = **chế độ tương thích KHÔNG AN TOÀN**, chỉ để demo cục bộ với Pitch cũ / giả lập, không dùng trong tích hợp thật.
  - Admin: xem và lọc mọi đơn, duyệt hoặc hủy (không cần gọi Pitch).
- **TH7:**
  - Thêm Eureka client (mặc định tắt: `EUREKA_ENABLED=false`, nên Booking vẫn chạy độc lập).
  - Tên dịch vụ: `pitchos-booking-service`. instance-id: `tên:port`.
  - Khi `PITCH_DISCOVERY_ENABLED=true`, WebClient gọi `http://pitchos-pitch-service`, được phân giải qua **Spring Cloud LoadBalancer** (`ReactorLoadBalancerExchangeFilterFunction`, round-robin). Retry kết hợp với LoadBalancer cho phép chuyển sang instance khác khi một instance chết.
  - Không tạo Eureka Server trong dự án, vì đây là phần hạ tầng chung.

## 7. Kết quả build và kiểm thử

### 7.1. Test tự động: `cd pitchos-booking-service && mvn test`

| Lớp test | Số test | Nội dung |
|---|---|---|
| `Th1BookingCrudIntegrationTest` | 10 | tạo, xem, sửa, hủy, DELETE mềm, duyệt, từ chối, trùng lịch, khung giờ liền kề |
| `Th2Th6PitchIntegrationTest` | 10 | Pitch OK, 404, 500, 503 tạm thời → retry thành công, 503 liên tục (đúng 3 lần gọi), timeout < 4s, dữ liệu sai, bảo trì, quote, phục hồi |
| `Th2PitchServiceDownIntegrationTest` | 1 | không tiến trình nào ở cổng Pitch → 503, Booking vẫn trả 200 cho API khác |
| `Th3PagingFilterValidationIntegrationTest` | 8 | phân trang, lọc trạng thái/sân/ngày/khoảng giờ, tham số sai, validation body và giờ, lỗi thống nhất + lỗi Spring 415/405 giữ đúng mã |
| `Th4BookingRepositoryJpaTest` | 6 | mapping, truy vấn trùng (tu_choi/da_huy/liền kề), UNIQUE idempotency, version; schema đọc qua JDBC `DatabaseMetaData` (chạy được trên H2 và MySQL): 3 index đúng cột theo thứ tự, UNIQUE `(user_id, idempotency_key)`, PK `pitch_locks`, không khóa ngoại |
| `Th5SecurityIntegrationTest` | 8 | thiếu/sai/hết hạn token, sai role, đơn người khác, userId giả mạo, ownerId chủ sân, chủ sân bị chặn ở mặc định khi Pitch chưa có ownerId, admin |
| `Th5StrictOwnerPolicyIntegrationTest` | 4 | chế độ chặt đặt tường minh: duyệt / xem danh sách / xem chi tiết bị chặn khi chưa xác minh; Pitch lỗi → 503 không cấp quyền; admin không bị ảnh hưởng |
| `Th5JwtClaimsAndRoleIntegrationTest` | 7 | token thiếu `userId` (sub username → 401; sub số → dùng làm userId), thiếu / rỗng `role` → 401, token hết hạn và sai chữ ký trên 9 endpoint → 401, role lạ → 403, chủ sân không liên quan bị chặn ở mặc định |
| `Th5OwnerFailClosedIntegrationTest` | 6 | mặc định fail-closed: không có `ownerId` → 403; Pitch 500/503/404/JSON hỏng/thiếu giá/thiếu ownerId/timeout → không cấp quyền; `ownerId` khác → 403; trùng → 200; admin vẫn thao tác khi Pitch lỗi; danh sách của chủ sân 403 |
| `Th5LegacyOwnerModeIntegrationTest` | 4 | chế độ tương thích (=true): ghi nhận hành vi và rủi ro cũ; Pitch lỗi vẫn không cấp quyền |
| `BookingTimeZoneUnitTest` | 10 | đồng hồ luôn theo `Asia/Ho_Chi_Minh` khi JVM ở UTC/New York/Tokyo/VN; quy tắc giờ bắt đầu cho cùng kết quả; đối chứng lỗi cũ; `main` đặt múi giờ mặc định |
| `Th6IdempotencyConcurrencyIntegrationTest` | 6 | gửi lại cùng key, gửi lại khi Pitch lỗi, 409, **10 request đồng thời cùng key → 1 đơn**, **10 khách đồng thời cùng giờ → 1 đơn 201 + 9 lỗi 400**, nhiều sân đồng thời |
| `Th7LoadBalancerIntegrationTest` | 1 | 2 instance HTTP thật: 6 lời gọi chia 3/3; tắt 1 instance → 6/6 vẫn 200 |
| `FeignCompatibilityIntegrationTest` | 1 | OpenFeign cũ: 201, 404, 503 |
| `BookingRulesUnitTest` | 5 | biên thời gian (đồng hồ cố định), công thức tiền, định dạng key, phân loại lỗi tạm thời |
| **Tổng** | **87** | **87 pass, 0 fail** (`mvn clean test`, giai đoạn khắc phục 2). Lịch sử: 57 khi đóng gói → 77 sau khắc phục 1 → 87 sau khắc phục 2 |

Môi trường test:
- H2 trong bộ nhớ chế độ MySQL, mỗi Spring context một DB riêng (riêng `Th4BookingRepositoryJpaTest` dùng `@DataJpaTest` nên chạy H2 thường). Toàn bộ 87 test cũng đạt trên MySQL 8.4.3 thử nghiệm (mục 7.4).
- Pitch Service giả lập là **HTTP server thật** (`com.sun.net.httpserver`), nên lỗi mạng, mã HTTP và độ trễ đều là thật.
- Token sinh đúng định dạng của Auth.

### 7.2. Kiểm thử thực tế với tiến trình thật (log ở `docs/bang-chung-kiem-thu/`)

Chạy file jar **không sửa code** của Auth (Hoàng), Pitch (Linh) và Booking. Máy không có MySQL, nên H2 được nạp lúc chạy bằng `PropertiesLauncher -Dloader.path=h2.jar` cho Auth và Booking; Pitch dùng sẵn profile `h2` của nó.

- **3 service thật** (`e2e-3-service-that.txt`): token lấy từ `POST /api/v1/auth/login` thật (payload `{"sub":"user","userId":3,"role":"user",...}`). 23 ca đều đúng mong đợi:
  - 401 ×2
  - đặt sân 201 (450.000đ)
  - GET 200, my-bookings 200
  - PUT sang sân 2 200 (500.000đ)
  - trùng giờ 400, sân 999 404, bảo trì 400, giờ quá khứ 400
  - quote 200
  - idempotency 201 → 200 (cùng id) → 409
  - lọc + phân trang 200
  - user duyệt 403, chusan đặt 403, chusan duyệt 200 (*chạy khi mặc định còn là chế độ tương thích; với mặc định fail-closed hiện tại và Pitch cũ không có `ownerId`, ca này trả 403*), admin từ chối 200
  - đổi đơn `tu_choi` 400, hủy 200, DELETE đơn đã hủy 400
  - admin xem tất cả 200
- **Tắt Pitch Service thật** (`e2e-th2-pitch-tat-log-booking.txt`):
  - POST → **503 sau 920 ms**, log cho thấy 2 lần thử lại.
  - `my-bookings` vẫn 200.
  - Bật lại Pitch → 201.
- **TH7 với Eureka Server tạm** (`e2e-th7-eureka-loadbalancer.txt`):
  - Môi trường: Eureka Server dựng tạm ngoài workspace (Spring Cloud 2023.0.1, cổng 8761), 2 Pitch Service thật (8082, 8092), 2 Booking (8083, 8093) bật `EUREKA_ENABLED` và `PITCH_DISCOVERY_ENABLED`.
  - Registry: `PITCHOS-BOOKING-SERVICE` ×2 và `PITCHOS-PITCH-SERVICE` ×2 đều `UP`. 2 instance Pitch được đăng ký **thủ công qua REST API** của Eureka, vì Pitch chưa có Eureka client.
  - 10 lời gọi qua tên dịch vụ: **Pitch-A nhận 5, Pitch-B nhận 5**.
  - Booking:8093 đặt sân qua Eureka: 201.
  - Tắt tiến trình Pitch-B (vẫn còn trong registry): **6/6 lời gọi vẫn 200**, Pitch-A nhận cả 6, Booking ghi 6 lần thử lại.
  - Lần chạy đầu tiên bị 401 do đăng nhập quá sớm khi Auth vừa khởi động. Đã chạy lại, kết quả trên là của lần chạy lại.

### 7.3. Kiểm chứng test có giá trị (mutation check)

Mình tạm vô hiệu hóa khóa sân (`lockPitch` không làm gì) rồi chạy lại `concurrentSameSlot_onlyOneWins` **3 lần**: cả 3 lần **FAIL** với `201=10` (10 đơn trùng một khung giờ). Sau đó khôi phục file (đã `diff` xác nhận) và test pass trở lại. Như vậy test thực sự phát hiện race condition.

> Lưu ý trung thực: test được viết **sau** khi viết code, không theo đúng chu trình TDD RED → GREEN. Bước mutation check ở trên bù một phần cho điều đó. Chưa đo độ phủ code (dự án không cấu hình JaCoCo).

### 7.4. Kiểm thử trên MySQL 8.4.3 (TH4, giai đoạn khắc phục 3)

Môi trường: `mysqld 8.4.3` có sẵn trên máy (bản của Laragon, chữ ký số hợp lệ), chạy với `--no-defaults`, **thư mục dữ liệu mới trong `%TEMP%`**, cổng 3399, chỉ lắng nghe `127.0.0.1`. Không dùng dữ liệu Laragon hay CSDL nào đang có. Script chung của nhóm chỉ được đọc. Log đầy đủ: `bang-chung-kiem-thu/th4-mysql-8.4.3.txt`.

| Hạng mục | Kết quả |
|---|---|
| V2 trên CSDL của script chung: lần 1, lần 2, lần 3 | Thoát mã 0 cả 3 lần; ENUM có `da_huy`; đủ 4 cột, 3 index, UNIQUE, `pitch_locks`; lần 2–3 cấu trúc và `CHECKSUM TABLE` không đổi; dữ liệu gốc giữ nguyên (md5), dòng cũ `version = 0` |
| V2 khi bảng đã có cột / index nhưng **thiếu `da_huy`** (ứng dụng khởi động trước) | Thêm đúng `da_huy`, chạy lại không đổi, dữ liệu giữ nguyên |
| V2 trên CSDL do Hibernate tạo | Không thay đổi gì (2 lần) |
| Không chỉ định CSDL | Dừng: `ERROR 1046 No database selected` |
| Cấu trúc không tương thích (6 ca: không có bảng, ENUM lạ, cột sai kiểu, index trùng tên khác cột, dữ liệu trùng key, `pitch_locks` khác cấu trúc) | Dừng với đúng mã `V2_LOI_…`, cấu trúc và dữ liệu **không đổi** |
| Chức năng với 2 instance Booking cùng CSDL (11 ca F1–F10) | Đạt: ghi/đọc đúng cột; 20 request đồng thời → 1 đơn; 10 request cùng key → 1 dòng; `SELECT … FOR UPDATE` làm request chờ 3,0 giây; vi phạm UNIQUE trong transaction được rollback; 2 cập nhật đồng thời → 200 + 409 |
| Bộ test 87 test trỏ sang MySQL | **87/87** (trước khắc phục: 86/87, lỗi ở câu SQL chỉ có trên H2 của test index) |
| Test index mới (JDBC `DatabaseMetaData`) | Đạt trên H2 và MySQL; bắt được 3 đột biến (bỏ index, đảo cột, thêm khóa ngoại) |

**Chưa kiểm chứng:** MySQL 8.0 (docker-compose của PTPM dùng `mysql:8.0`), MySQL của nhóm, user CSDL có quyền tối thiểu, kết nối SSL.

## 8. Lỗi đã phát hiện và xử lý

1. Khóa `synchronized` theo sân chỉ đúng với 1 instance, sai khi chạy nhiều instance (TH7) → thay bằng khóa dòng trong CSDL.
2. Truy vấn trùng lịch cần bỏ qua đơn `da_huy` mới → đã cập nhật cả 3 truy vấn.
3. 401 trả body rỗng, 403 không đúng định dạng chung → nay đều trả JSON thống nhất.
4. Lỗi validation chỉ báo trường đầu tiên → thêm `details` (vẫn giữ `message` cũ).
5. File `pitchos-booking-service/application.properties` ở thư mục gốc (bản cũ) được Spring ưu tiên hơn cấu hình profile trong classpath → test dùng `@TestPropertySource`. File cũ **không bị sửa**; ghi chú trong README.
6. Postman: ca 08 của bản cũ dùng sân 4 làm sân bảo trì (đúng với mock), còn Pitch Service thật bảo trì sân 3 → ghi chú trong thư mục Postman mới, không sửa ca cũ.
7. (Phát hiện khi rà soát trước push) `GlobalExceptionHandler` bắt `Exception` chung làm lỗi phía client của Spring MVC (sai `Content-Type` → 415) bị trả **500**. Đã viết test tái hiện (RED: 500), sửa bằng cách giữ mã HTTP của ngoại lệ chuẩn Spring (`org.springframework.web.ErrorResponse`), test GREEN.

8. (Giai đoạn khắc phục 1) `searchBookings` coi mọi role khác `user`/`chusan` như admin → role lạ (`guest`, `CUSTOMER`…) xem được toàn bộ đơn. Test tái hiện (RED: 200 thay vì 403), sửa: chỉ admin xem tất cả, role khác → 403.
9. (Giai đoạn khắc phục 1) Đồng hồ dùng múi giờ của máy → trong container UTC, quy tắc "giờ bắt đầu ở tương lai" lệch 7 giờ. Sửa: `TimeConfig` cố định `Asia/Ho_Chi_Minh`, `main` đặt múi giờ mặc định của JVM (RED: 3/4 múi giờ sai).
10. (Giai đoạn khắc phục 2) Mặc định chính sách chủ sân là `true` → mọi chủ sân duyệt / xem mọi đơn khi Pitch chưa có `ownerId`. Đổi mặc định sang `false` (fail-closed) ở `application.properties`, `BookingAccessPolicy`, `.env.example`; 5 test phụ thuộc mặc định cũ được điều chỉnh (Th1: Pitch giả lập xác nhận chủ sân) hoặc chuyển sang `Th5LegacyOwnerModeIntegrationTest` (không xóa).
11. (Giai đoạn khắc phục 3) V2 bản cũ không chạy lại được (lỗi 1060/1061) và cố định `USE pitchos_booking_db`. Viết lại: không `USE`, kiểm tra `information_schema` + SQL động, kiểm tra cấu trúc trước khi sửa, mã lỗi `V2_LOI_…`. Kiểm chứng trên MySQL 8.4.3 (mục 7.4).
12. (Giai đoạn khắc phục 3) Test index dùng `information_schema.indexes` (chỉ có trên H2) → lỗi trên MySQL. Viết lại bằng JDBC `DatabaseMetaData`, kiểm tra chặt hơn (cột theo thứ tự, UNIQUE, PK, khóa ngoại hai chiều); đạt trên H2 và MySQL.

## 9. Phần còn thiếu và bị chặn

| Vấn đề | Trạng thái | Cần làm |
|---|---|---|
| Kiểm thử trên MySQL của nhóm / MySQL 8.0 | CHỜ TÍCH HỢP — đã đạt trên MySQL 8.4.3 thử nghiệm (mục 7.4) | Hùng xác nhận tên CSDL, phiên bản MySQL, quy trình migration (script chung hay V2, Flyway/Liquibase), `ddl-auto`, user/quyền, SSL; sau đó chạy V2 + Postman trên MySQL của nhóm |
| Chủ sân chỉ thao tác đơn của sân mình | BLOCKED (Pitch Service chưa có `ownerId`) — Booking đã **fail-closed**: chủ sân hiện bị chặn, chỉ admin duyệt được | Phía Pitch thêm `ownerId` vào `PitchResponse`. Booking tự áp dụng, không cần sửa code; xác minh lại bằng Postman ca 14 |
| Chủ sân xem danh sách `GET /api/v1/bookings` | BLOCKED (cần API sân theo chủ sở hữu ở Pitch) | Hiện trả 403 ở mặc định |
| Pitch Service đăng ký Eureka | BLOCKED (thuộc Pitch Service) | Thêm `spring-cloud-starter-netflix-eureka-client` cho Pitch |
| Eureka Server dùng chung (8761) | BLOCKED (hạ tầng chung) | Phần của Hoàng theo bảng tuần. Booking chỉ cần `EUREKA_ENABLED=true` |
| API `/quote` giờ vàng phía Provider | Ngoài phạm vi Booking | Booking hiện báo giá từ `pricePerHour`. Khi Provider có `/quote`, đổi `PitchGateway` sang gọi API đó |
| Thống nhất **port 8081/8083** và tên DB `booking_db`/`pitchos_booking_db` | CHỜ NHÓM QUYẾT | Giữ mặc định cũ, đổi qua `BOOKING_SERVICE_PORT`, `BOOKING_DB_NAME`. Đổi Booking sang 8081 sẽ đụng cổng Auth (đang 8081) |
| Phân công lệch: bảng tuần ghi Linh = booking, Nhật = pitchs | CHỜ NHÓM XÁC NHẬN | Workspace làm theo yêu cầu: Nhật = booking-service |
| `.env` có trong repo cũ (giá trị trùng `.env.example`) | Đã xử lý | Đã thêm `.env`, `.env.*` (trừ `.env.example`), khóa/chứng chỉ vào `.gitignore`; file `.env` của bạn giữ nguyên |

## 10. Hướng dẫn chạy

```bash
# 1. Test tự động (không cần MySQL)
cd pitchos-booking-service
mvn test

# 2. Chạy thật với MySQL (sau khi có MySQL 8)
# CSDL mới: CREATE DATABASE <ten_csdl_booking> ... rồi để Hibernate tạo bảng
# CSDL tạo từ script chung của nhóm (Hùng): nâng cấp TRƯỚC khi chạy (chạy lại an toàn, tự chỉ định tên CSDL, không --force)
mysql -u <user> -p --default-character-set=utf8mb4 <ten_csdl_booking> < db/V2__booking_th1_th7_mysql.sql
mvn spring-boot:run                                          # cổng 8083, gọi Pitch ở 8082

# 3. Bật Eureka + LoadBalancer (cần Eureka Server 8761 và Pitch đã đăng ký)
EUREKA_ENABLED=true PITCH_DISCOVERY_ENABLED=true mvn spring-boot:run
# Nhiều instance Booking: thêm BOOKING_SERVICE_PORT=8093 cho instance thứ hai

# 4. Dùng lại OpenFeign cũ thay cho WebClient
PITCH_CLIENT_TYPE=feign mvn spring-boot:run
```

**Kiểm thử thủ công bằng Postman:** import `postman/PitchOS-Booking.postman_collection.json`.
- Thư mục 1–3 là kịch bản cũ.
- Thư mục **4** là TH1–TH7, chạy lần lượt từ trên xuống. Request "User khác xem đơn này (403)" cần điền biến `tokenUserOther` bằng token của một user khác.
- Kịch bản demo 503: tắt Pitch Service rồi gửi lại request đặt sân (thư mục 3).

## 11. Xác nhận về Git và phạm vi

- **Không thực hiện bất kỳ thao tác Git nào** trong `C:\nhom13` (thư mục này không có `.git`) và không thao tác với repository nào. Ở bước khảo sát ban đầu có chạy các lệnh chỉ đọc (`git status`, `git log`, `git ls-remote`) trên các bản PTPM cũ.
- Mọi thay đổi nằm trong `C:\nhom13`. Thư mục tạm cho Eureka Server và log E2E nằm trong scratchpad của phiên làm việc, không thuộc dự án.
- Không sửa Auth, Pitch, script SQL của Hùng. File chung duy nhất được đụng tới là `.gitignore` (chỉ thêm quy tắc, theo yêu cầu rà soát trước push).
