# Auth Service — Football Booking System

Dịch vụ **Xác thực & Phân quyền** cho hệ thống quản lý đặt lịch sân bóng đá.

- **Phụ trách:** Hoàng (Nhóm trưởng)
- **Port:** `8080`
- **Nhánh Git:** `feat/hoang-auth-service`
- **Công nghệ:** Java 17, Spring Boot 3.2.5, Spring Security 6, JJWT 0.12.5, Spring Data JPA, MySQL 8 (CSDL `auth_db`), Eureka Client, SpringDoc OpenAPI 2.5.0

## Nhiệm vụ theo từng buổi (TH1–TH7)

| Buổi | Nội dung của Hoàng | Đã hiện thực ở |
|------|--------------------|----------------|
| TH1 | CRUD tài khoản + đăng ký/đăng nhập cơ bản | `AuthController`, `UserController`, `AuthService`, `UserService` |
| TH2 | Xác thực liên service, truyền token | `POST /api/v1/auth/validate`, header nội bộ |
| TH3 | Validation đăng ký, chuẩn lỗi 401/403 | `RegisterRequest` (Bean Validation), `GlobalExceptionHandler`, 2 handler 401/403 |
| TH4 | `auth_db` — bảng user/role | `User` entity, `UserRepository`, `database/auth_db_init.sql` |
| TH5 | Phát hành & xác thực JWT, RBAC | `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`, enum `Role` |
| TH6 | Bảo mật liên service, token propagation | Header `X-Internal-Service-Key` → `ROLE_INTERNAL_SERVICE`, endpoint `/validate` |
| TH7 | Đăng ký vào Eureka | cấu hình `eureka.*` + module `../eureka-server` |

## Chạy nhanh (không cần cài MySQL — dùng H2 in-memory)

```bash
cd auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

## Chạy với MySQL thật (TH4)

1. Mở MySQL, chạy `database/auth_db_init.sql` (hoặc để Spring tự tạo nhờ `createDatabaseIfNotExist=true`).
2. Chỉnh user/password trong `application.properties` nếu khác `root/root`.
3. `mvn spring-boot:run`

## Tài khoản mẫu (mật khẩu: `password123`)

| username | vai trò |
|----------|---------|
| customer01, customer02 | ROLE_CUSTOMER |
| owner01 | ROLE_FIELD_OWNER |
| admin01 | ROLE_ADMIN |

## API chính

| Method | Endpoint | Quyền | Mô tả |
|--------|----------|-------|-------|
| POST | `/api/v1/auth/register` | Public | Đăng ký (mặc định ROLE_CUSTOMER) |
| POST | `/api/v1/auth/login` | Public | Đăng nhập → JWT |
| POST | `/api/v1/auth/validate` | Đăng nhập / internal key | Xác thực token cho service khác |
| GET | `/api/v1/auth/me` | Đăng nhập | Thông tin phiên hiện tại |
| GET | `/api/v1/auth/health` | Public | Trạng thái service |
| GET | `/api/v1/users` | ADMIN | Danh sách người dùng (phân trang) |
| GET | `/api/v1/users/{id}` | ADMIN | Chi tiết người dùng |
| PATCH | `/api/v1/users/{id}/role` | ADMIN | Đổi vai trò |
| PATCH | `/api/v1/users/{id}/active` | ADMIN | Khóa/mở khóa |
| DELETE | `/api/v1/users/{id}` | ADMIN | Xóa người dùng |

Swagger UI: **http://localhost:8080/swagger-ui.html**

## Thử nhanh bằng curl

```bash
# Đăng nhập lấy token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin01","password":"password123"}'

# Gọi API cần quyền ADMIN (thay <TOKEN> bằng accessToken nhận được)
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <TOKEN>"
```

## Bật Eureka (TH7)

1. Khởi động `../eureka-server` (port 8761): `cd ../eureka-server && mvn spring-boot:run`
2. Trong `application.properties` của auth-service, đổi `eureka.client.enabled=true`
3. Khởi động lại auth-service → vào http://localhost:8761 thấy `AUTH-SERVICE` đã đăng ký.
