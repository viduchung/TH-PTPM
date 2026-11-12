# Pitchs Service — Football Booking System

Dịch vụ **Quản lý Sân bóng & Báo giá** cho hệ thống quản lý đặt lịch sân bóng đá.

- **Phụ trách:** Linh
- **Port:** `8082`
- **Nhánh Git:** `feat/linh-pitchs-service`
- **CSDL:** `pitch_db` (MySQL)
- **Công nghệ:** Java 17, Spring Boot 3.2.5, Spring Data JPA, Hibernate, MySQL 8 / H2 in-memory, SpringDoc OpenAPI (Swagger UI) 2.5.0

---

## 1. Nhiệm vụ theo từng buổi (TH1 - TH4)

| Buổi | Nội dung của Linh | Đã hiện thực ở |
|---|---|---|
| **TH1** | Khởi tạo service port 8082, CSDL `pitch_db`, CRUD cơ bản cho thực thể Sân bóng | `application.yml`, `Pitch`, `PitchRepository`, `PitchServiceImpl` |
| **TH2** | Chuẩn hóa DTO trao đổi, cung cấp API thông tin sân & báo giá cho `booking-service` | `PitchResponseDTO`, `PriceResponseDTO`, `PitchRequestDTO`, `GET /api/pitches/{id}/price` |
| **TH3** | REST nâng cao: Tìm kiếm lọc sân theo loại/trạng thái kết hợp phân trang (`Pageable`); validation dữ liệu đầu vào | `GET /api/pitches/search`, `@Pattern`, `@DecimalMin`, `@NotBlank`, `GlobalExceptionHandler` |
| **TH4** | CSDL thật JPA: Quan hệ 1-N giữa Sân bóng (`Pitch`) và Giá giờ vàng (`GoldenHourPrice`) | `GoldenHourPrice`, `@OneToMany`, `@ManyToOne`, `GoldenHourPriceRepository`, `pitch_db_init.sql` |

---

## 2. Cách chạy ứng dụng

### Cách 1: Chạy với MySQL thật (Mặc định)
1. Đảm bảo MySQL đang chạy ở port 3306.
2. Kiểm tra tài khoản `username` và `password` trong `src/main/resources/application.yml` (mặc định: `root/root`).
3. Chạy lệnh:
```bash
cd pitchs-service
mvn clean spring-boot:run
```
*(Cơ sở dữ liệu `pitch_db` và các bảng `pitches`, `golden_hour_prices` sẽ tự động được tạo và seed sẵn dữ liệu mẫu nếu bảng trống).*

### Cách 2: Chạy nhanh không cần MySQL (dùng H2 in-memory)
Nếu máy bạn chưa bật MySQL hoặc muốn test nhanh, chạy bằng profile `h2`:
```bash
cd pitchs-service
mvn clean spring-boot:run -Dspring-boot.run.profiles=h2
```

---

## 3. Danh sách RESTful API đầy đủ (TH1 - TH4)

| Method | Endpoint | Đối tượng gọi | Mô tả |
|---|---|---|---|
| `GET` | `/api/pitches/search` | Frontend / Admin | **[TH3]** Tìm kiếm & lọc sân theo loại, trạng thái kết hợp phân trang |
| `GET` | `/api/pitches` | Frontend / Admin | Lấy danh sách tất cả sân bóng (DTO) |
| `GET` | `/api/pitches/{id}` | booking-service / User | Lấy thông tin chi tiết một sân bóng kèm danh sách giờ vàng |
| `GET` | `/api/pitches/{id}/price` | booking-service | Lấy thông tin báo giá và trạng thái khả dụng của sân |
| `POST` | `/api/pitches` | Admin / Chủ sân | **[TH3]** Thêm mới một sân bóng (Validation chặt chẽ) |
| `PUT` | `/api/pitches/{id}` | Admin / Chủ sân | **[TH3]** Cập nhật thông tin sân bóng theo ID (Validation) |
| `DELETE` | `/api/pitches/{id}` | Admin / Chủ sân | Xóa sân bóng theo ID (Cascade xóa luôn giờ vàng) |
| `POST` | `/api/pitches/{id}/golden-hours` | Admin / Chủ sân | **[TH4]** Thêm khung giá giờ vàng cho một sân |
| `DELETE` | `/api/pitches/{id}/golden-hours/{priceId}` | Admin / Chủ sân | **[TH4]** Xóa một khung giá giờ vàng |

Swagger UI tài liệu API: **http://localhost:8082/swagger-ui.html**
