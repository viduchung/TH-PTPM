# Pitchs Service — Football Booking System

Dịch vụ **Quản lý Sân bóng & Báo giá** cho hệ thống quản lý đặt lịch sân bóng đá.

- **Phụ trách:** Linh
- **Port:** `8082`
- **Nhánh Git:** `feat/linh-pitchs-service`
- **CSDL:** `pitch_db` (MySQL)
- **Công nghệ:** Java 17, Spring Boot 3.2.5, Spring Security 6, JJWT 0.12.5, Spring Data JPA, Hibernate, MySQL 8 / H2 in-memory, SpringDoc OpenAPI (Swagger UI) 2.5.0

---

## 1. Tiến độ bài thực hành theo từng buổi (TH1 - TH6)

| Buổi | Nội dung của Linh | Đã hiện thực ở |
|---|---|---|
| **TH1** | Khởi tạo service port 8082, CSDL `pitch_db`, CRUD cơ bản cho thực thể Sân bóng | `application.yml`, `Pitch`, `PitchRepository`, `PitchServiceImpl` |
| **TH2** | Chuẩn hóa DTO trao đổi, cung cấp API thông tin sân & báo giá cho `booking-service` | `PitchResponseDTO`, `PriceResponseDTO`, `PitchRequestDTO`, `GET /api/pitches/{id}/price` |
| **TH3** | REST nâng cao: Tìm kiếm lọc sân theo loại/trạng thái kết hợp phân trang (`Pageable`); validation dữ liệu đầu vào | `GET /api/pitches/search`, `@Pattern`, `@DecimalMin`, `@NotBlank`, `GlobalExceptionHandler` |
| **TH4** | CSDL thật JPA: Quan hệ 1-N giữa Sân bóng (`Pitch`) và Giá giờ vàng (`GoldenHourPrice`) | `GoldenHourPrice`, `@OneToMany`, `@ManyToOne`, `GoldenHourPriceRepository`, `pitch_db_init.sql` |
| **TH5** | Bảo mật JWT & Spring Security RBAC: Phân quyền Thêm/Sửa/Xóa chỉ dành cho `OWNER` hoặc `ADMIN` | `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, `@PreAuthorize` |
| **TH6** | Provider API: Cung cấp API `/quote` tính toán chi tiết giá thuê sân theo khung giờ & giờ vàng cho các service khác | `GET /api/pitches/{id}/quote`, `QuoteResponseDTO`, `BadRequestException` |

---

## 2. Cách chạy ứng dụng

### Cách 1: Chạy với MySQL thật (Mặc định)
```bash
cd pitchs-service
mvn clean spring-boot:run
```

### Cách 2: Chạy nhanh không cần MySQL (dùng H2 in-memory)
```bash
cd pitchs-service
mvn clean spring-boot:run -Dspring-boot.run.profiles=h2
```

---

## 3. Danh sách RESTful API đầy đủ (TH1 - TH6)

| Method | Endpoint | Quyền truy cập | Mô tả |
|---|---|---|---|
| `GET` | `/api/pitches/search` | **Public** | [TH3] Tìm kiếm & lọc sân theo loại, trạng thái kết hợp phân trang |
| `GET` | `/api/pitches` | **Public** | Lấy danh sách tất cả sân bóng (DTO) |
| `GET` | `/api/pitches/{id}` | **Public** | Lấy chi tiết sân bóng kèm danh sách giờ vàng |
| `GET` | `/api/pitches/{id}/price` | **Public** | Lấy thông tin giá cơ bản của sân (cho `booking-service`) |
| `GET` | `/api/pitches/{id}/quote` | **Public** | **[TH6]** Tính toán báo giá thuê sân theo thời gian & giờ vàng |
| `POST` | `/api/pitches` | **OWNER / ADMIN** | **[TH5]** Thêm mới một sân bóng (Yêu cầu JWT Bearer Token) |
| `PUT` | `/api/pitches/{id}` | **OWNER / ADMIN** | **[TH5]** Cập nhật thông tin sân bóng theo ID |
| `DELETE` | `/api/pitches/{id}` | **OWNER / ADMIN** | **[TH5]** Xóa sân bóng theo ID (Cascade xóa giờ vàng) |
| `POST` | `/api/pitches/{id}/golden-hours` | **OWNER / ADMIN** | **[TH4/5]** Thêm khung giá giờ vàng cho sân |
| `DELETE` | `/api/pitches/{id}/golden-hours/{priceId}` | **OWNER / ADMIN** | **[TH4/5]** Xóa khung giá giờ vàng của sân |

Swagger UI tài liệu API: **http://localhost:8082/swagger-ui.html**

---

## 4. Hướng dẫn kiểm thử API `/quote` (TH6 Provider)

### Ví dụ 1: Tính tiền bao gồm giờ vàng
* **Request:**
  ```bash
  curl -X GET "http://localhost:8082/api/pitches/1/quote?date=2026-11-20&startTime=16:30:00&endTime=18:30:00"
  ```
* **Kết quả trả về:**
  * Tổng thời gian: 120 phút.
  * Giờ thường (16:30 -> 17:30): 60 phút x 250.000đ/h = 250.000đ.
  * Giờ vàng (17:30 -> 18:30): 60 phút x 320.000đ/h = 320.000đ.
  * **Tổng tiền:** 570.000 VNĐ.

### Ví dụ 2: Lỗi tham số sai (Giờ kết thúc trước giờ bắt đầu)
* **Request:**
  ```bash
  curl -X GET "http://localhost:8082/api/pitches/1/quote?date=2026-11-20&startTime=19:00:00&endTime=17:00:00"
  ```
* **Kết quả (HTTP 400 Bad Request):**
  ```json
  {
    "timestamp": "2026-11-13T00:31:35.834",
    "status": 400,
    "error": "Bad Request",
    "message": "Giờ kết thúc (17:00) phải diễn ra sau giờ bắt đầu (19:00)!",
    "path": "/api/pitches/1/quote"
  }
  ```
