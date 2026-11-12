# Pitchs Service — Football Booking System

Dịch vụ **Quản lý Sân bóng & Báo giá** cho hệ thống quản lý đặt lịch sân bóng đá.

- **Phụ trách:** Linh
- **Port mặc định:** `8082` (Hỗ trợ cấu hình port linh hoạt `--server.port=...` hoặc `port: 0`)
- **Nhánh Git:** `feat/linh-pitchs-service`
- **CSDL:** `pitch_db` (MySQL)
- **Công nghệ:** Java 17, Spring Boot 3.2.5, Spring Cloud Netflix Eureka Client (2023.0.1), Spring Security 6, JJWT 0.12.5, Spring Data JPA, Hibernate, MySQL 8 / H2 in-memory, SpringDoc OpenAPI (Swagger UI) 2.5.0

---

## 1. Tiến độ bài thực hành theo từng buổi (TH1 - TH7)

| Buổi | Nội dung của Linh | Đã hiện thực ở |
|---|---|---|
| **TH1** | Khởi tạo service port 8082, CSDL `pitch_db`, CRUD cơ bản cho thực thể Sân bóng | `application.yml`, `Pitch`, `PitchRepository`, `PitchServiceImpl` |
| **TH2** | Chuẩn hóa DTO trao đổi, cung cấp API thông tin sân & báo giá cho `booking-service` | `PitchResponseDTO`, `PriceResponseDTO`, `PitchRequestDTO`, `GET /api/pitches/{id}/price` |
| **TH3** | REST nâng cao: Tìm kiếm lọc sân theo loại/trạng thái kết hợp phân trang (`Pageable`); validation dữ liệu đầu vào | `GET /api/pitches/search`, `@Pattern`, `@DecimalMin`, `@NotBlank`, `GlobalExceptionHandler` |
| **TH4** | CSDL thật JPA: Quan hệ 1-N giữa Sân bóng (`Pitch`) và Giá giờ vàng (`GoldenHourPrice`) | `GoldenHourPrice`, `@OneToMany`, `@ManyToOne`, `GoldenHourPriceRepository`, `pitch_db_init.sql` |
| **TH5** | Bảo mật JWT & Spring Security RBAC: Phân quyền Thêm/Sửa/Xóa chỉ dành cho `OWNER` hoặc `ADMIN` | `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, `@PreAuthorize` |
| **TH6** | Provider API: Cung cấp API `/quote` tính toán chi tiết giá thuê sân theo khung giờ & giờ vàng cho các service khác | `GET /api/pitches/{id}/quote`, `QuoteResponseDTO`, `BadRequestException` |
| **TH7** | Service Registry (Eureka Client): Đăng ký `pitchs-service` vào Eureka Server (port 8761), cấu hình multi-instance cân bằng tải | `@EnableDiscoveryClient`, `instance-id: ${spring.application.name}:${random.value}:${server.port}`, `GET /api/pitches/instance-info` |

---

## 2. Cách chạy ứng dụng với Eureka Server (TH7)

### Bước 1: Khởi động Eureka Server (Port 8761)
```bash
cd eureka-server
mvn clean spring-boot:run
```
*(Truy cập dashboard Eureka tại: **http://localhost:8761**)*

### Bước 2: Chạy nhiều Instance của `pitchs-service` cùng lúc

* **Instance 1 (Port 8082):**
  ```bash
  cd pitchs-service
  java -jar target/pitchs-service-1.0.0.jar --server.port=8082
  ```

* **Instance 2 (Port 8083):**
  ```bash
  cd pitchs-service
  java -jar target/pitchs-service-1.0.0.jar --server.port=8083
  ```

* **Instance 3 (Port ngẫu nhiên tự động):**
  ```bash
  cd pitchs-service
  java -jar target/pitchs-service-1.0.0.jar --server.port=0
  ```

### Bước 3: Kiểm tra cân bằng tải (Load Balancing)
* Gọi API kiểm tra instance đang phục vụ:
  * `http://localhost:8082/api/pitches/instance-info`
  * `http://localhost:8083/api/pitches/instance-info`
* Vào **http://localhost:8761**, bạn sẽ thấy cả 2-3 instance hiển thị đồng thời dưới tên ứng dụng **`PITCHS-SERVICE`**.
