# Pitchs Service — Football Booking System

Dịch vụ **Quản lý Sân bóng & Báo giá** cho hệ thống quản lý đặt lịch sân bóng đá.

- **Phụ trách:** Linh
- **Port:** `8082`
- **Nhánh Git:** `feat/linh-pitchs-service`
- **CSDL:** `pitch_db` (MySQL)
- **Công nghệ:** Java 17, Spring Boot 3.2.5, Spring Data JPA, MySQL 8 / H2 in-memory, SpringDoc OpenAPI (Swagger UI) 2.5.0

---

## 1. Nhiệm vụ Buổi TH1

| Câu | Yêu cầu | Đã hiện thực ở |
|---|---|---|
| Câu 1 ý 1 | Khởi tạo service trên port 8082, kết nối CSDL `pitch_db` | `application.yml`, `PitchServiceApplication.java` |
| Câu 2 ý 1 | Xây dựng RESTful API CRUD cho thực thể Sân bóng (Pitch): Thêm, Sửa, Xóa, Lấy danh sách | `PitchController`, `PitchService`, `PitchServiceImpl`, `PitchRepository`, `Pitch` |

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

## 3. Danh sách RESTful API

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/pitches` | Lấy danh sách tất cả sân bóng |
| `GET` | `/api/pitches/{id}` | Lấy chi tiết sân bóng theo ID |
| `POST` | `/api/pitches` | Thêm mới một sân bóng |
| `PUT` | `/api/pitches/{id}` | Cập nhật thông tin sân bóng theo ID |
| `DELETE` | `/api/pitches/{id}` | Xóa một sân bóng theo ID |

Swagger UI tài liệu API: **http://localhost:8082/swagger-ui.html**

---

## 4. Test API với cURL

### 4.1. Thêm sân mới (POST)
```bash
curl -X POST http://localhost:8082/api/pitches \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sân 7 Hoàng Gia",
    "type": "SAN_7",
    "pricePerHour": 380000,
    "address": "Số 10 Phạm Hùng, Nam Từ Liêm, Hà Nội",
    "status": "AVAILABLE",
    "description": "Mặt sân mới, có khu vực khán đài nhỏ"
  }'
```

### 4.2. Lấy danh sách sân (GET)
```bash
curl -X GET http://localhost:8082/api/pitches
```

### 4.3. Xem chi tiết sân (GET)
```bash
curl -X GET http://localhost:8082/api/pitches/1
```

### 4.4. Cập nhật thông tin sân (PUT)
```bash
curl -X PUT http://localhost:8082/api/pitches/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sân 5 số 1 - Cỏ nhân tạo (Đã bảo trì xong)",
    "type": "SAN_5",
    "pricePerHour": 280000,
    "address": "Khu thể thao Bách Khoa, Hai Bà Trưng, Hà Nội",
    "status": "AVAILABLE",
    "description": "Sân 5 người, cỏ nhân tạo vừa bảo trì"
  }'
```

### 4.5. Xóa sân (DELETE)
```bash
curl -X DELETE http://localhost:8082/api/pitches/1
```

