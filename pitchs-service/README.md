# Pitchs Service — Football Booking System

Dịch vụ **Quản lý Sân bóng & Báo giá** cho hệ thống quản lý đặt lịch sân bóng đá.

- **Phụ trách:** Linh
- **Port:** `8082`
- **Nhánh Git:** `feat/linh-pitchs-service`
- **CSDL:** `pitch_db` (MySQL)
- **Công nghệ:** Java 17, Spring Boot 3.2.5, Spring Data JPA, MySQL 8 / H2 in-memory, SpringDoc OpenAPI (Swagger UI) 2.5.0

---

## 1. Nhiệm vụ theo từng buổi (TH1 - TH2)

| Buổi | Nội dung của Linh | Đã hiện thực ở |
|---|---|---|
| **TH1** | Khởi tạo service port 8082, CSDL `pitch_db`, CRUD cơ bản cho thực thể Sân bóng | `application.yml`, `Pitch`, `PitchRepository`, `PitchServiceImpl` |
| **TH2** | Chuẩn hóa DTO trao đổi, cung cấp API thông tin sân & báo giá cho `booking-service` gọi | `PitchResponseDTO`, `PriceResponseDTO`, `PitchRequestDTO`, `GET /api/pitches/{id}/price` |

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
*(Cơ sở dữ liệu `pitch_db` và bảng `pitches` sẽ tự động được tạo và seed sẵn 4 sân mẫu nếu bảng trống).*

### Cách 2: Chạy nhanh không cần MySQL (dùng H2 in-memory)
Nếu máy bạn chưa bật MySQL hoặc muốn test nhanh, chạy bằng profile `h2`:
```bash
cd pitchs-service
mvn clean spring-boot:run -Dspring-boot.run.profiles=h2
```

---

## 3. Danh sách RESTful API (Đã chuẩn hóa DTO cho TH2)

| Method | Endpoint | Đối tượng gọi | Mô tả |
|---|---|---|---|
| `GET` | `/api/pitches` | Frontend / Admin | Lấy danh sách tất cả sân bóng (DTO) |
| `GET` | `/api/pitches/{id}` | **booking-service** | Lấy thông tin chi tiết một sân bóng theo ID |
| `GET` | `/api/pitches/{id}/price` | **booking-service** | Lấy thông tin báo giá và trạng thái khả dụng của sân |
| `POST` | `/api/pitches` | Admin / Chủ sân | Thêm mới một sân bóng (Validate dữ liệu đầu vào) |
| `PUT` | `/api/pitches/{id}` | Admin / Chủ sân | Cập nhật thông tin sân bóng theo ID |
| `DELETE` | `/api/pitches/{id}` | Admin / Chủ sân | Xóa sân bóng theo ID |

Swagger UI tài liệu API: **http://localhost:8082/swagger-ui.html**

---

## 4. Hướng dẫn cho `booking-service` (Bạn Nhật) gọi sang

### 4.1. Cấu hình WebClient trong `booking-service`
```java
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient pitchWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8082")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
```

### 4.2. Gọi API lấy giá sân bằng WebClient
```java
public PriceResponseDTO getPitchPrice(Long pitchId) {
    return pitchWebClient.get()
            .uri("/api/pitches/{id}/price", pitchId)
            .retrieve()
            .bodyToMono(PriceResponseDTO.class)
            .block();
}
```
