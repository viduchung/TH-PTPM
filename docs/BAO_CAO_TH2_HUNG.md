# BÁO CÁO THỰC HÀNH BUỔI 2 (TH2)
## HỌC PHẦN: PHÁT TRIỂN PHẦN MỀM HƯỚNG DỊCH VỤ (SOA) / KIẾN TRÚC PHẦN MỀM

---

> **HỌ VÀ TÊN SINH VIÊN:** VŨ ĐỨC HÙNG  
> **MÃ SINH VIÊN / TÀI KHOẢN GIT:** `viduchung`  
> **VAI TRÒ TRONG DỰ ÁN:** Database Administrator & Backend Data Engineer  
> **MẢNG PHỤ TRÁCH:** Tầng Dữ liệu & CSDL Phân tán (Data Layer Xuyên Suốt Hệ Thống)  
> **NHÁNH GIT:** `feat/hung-data`  
> **ĐỀ TÀI:** Hệ thống Quản lý Đặt lịch Sân bóng đá (Football Booking System)  
> **CHỦ ĐỀ TH2:** Giao tiếp giữa các service (Service-to-Service Communication)  
> **NHIỆM VỤ TH2:** **Chuẩn hóa DTO trao đổi giữa service, tránh trùng model (Entity Leaking)**  

---

## MỤC LỤC BÁO CÁO
1. [Nhiệm Vụ Được Phân Công Trong Buổi TH2](#1-nhiệm-vụ-được-phân-công-trong-buổi-th2)
2. [Cơ Sở Lý Thuyết: Anti-Pattern "Dùng Chung Model" Và Giải Pháp DTO](#2-cơ-sở-lý-thuyết-anti-pattern-dùng-chung-model-và-giải-pháp-dto)
3. [Ma Trận Hợp Đồng Trao Đổi Dữ Liệu Liên Dịch Vụ](#3-ma-trận-hợp-đồng-trao-đổi-dữ-liệu-liên-dịch-vụ)
4. [Thiết Kế Chi Tiết Các Lớp DTO Chuẩn Hóa](#4-thiết-kế-chi-tiết-các-lớp-dto-chuẩn-hóa)
5. [Tầng Chuyển Đổi An Toàn (Mapper Layer)](#5-tầng-chuyển-đổi-an-toàn-mapper-layer)
6. [Xử Lý Lỗi Giao Tiếp Mạng Và Nguyên Lý Tolerant Reader](#6-xử-lý-lỗi-giao-tiếp-mạng-và-nguyên-lý-tolerant-reader)
7. [Kết Quả Kiểm Thử Tự Động (JUnit 5 & Serialization Tests)](#7-kết-quả-kiểm-thử-tự-động-junit-5--serialization-tests)
8. [Kết Luận Và Hướng Phát Triển Cho Buổi TH3](#8-kết-luận-và-hướng-phát-triển-cho-buổi-th3)

---

## 1. NHIỆM VỤ ĐƯỢC PHÂN CÔNG TRONG BUỔI TH2

Theo bảng phân công nhiệm vụ nhóm ([BANG_PHAN_CONG_HANG_TUAN.xlsx](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/BANG_PHAN_CONG_HANG_TUAN.xlsx)):
* **Chủ đề TH2:** Giao tiếp giữa các service (Service-to-Service Communication).
* **Hoàng (auth-service - 8080):** Xây dựng cơ chế xác thực liên service, truyền token JWT giữa các service (`POST /api/v1/auth/validate`).
* **Linh (pitchs-service - 8082):** Cung cấp API thông tin sân bóng và báo giá cho `booking-service` gọi sang (`GET /api/pitches/{id}/price`).
* **Nhật (booking-service - 8081/8083):** Gọi sang `pitchs-service` để kiểm tra sân trống và lấy đơn giá (dùng Spring `WebClient` / `RestClient`).
* **HÙNG (Data xuyên suốt - nhánh `feat/hung-data`):**
  1. Phân tích nguyên lý phân tách giữa **Mô hình Lưu trữ Nội bộ (Internal Persistence Entity)** và **Mô hình Dữ liệu Trao đổi (Data Transfer Object - DTO)**.
  2. Thiết kế và ban hành bộ **Hợp đồng DTO chuẩn hóa (Standard Inter-Service Contracts)** cho toàn bộ các giao tiếp mạng trong hệ sinh thái.
  3. Xây dựng **Tầng Mapper chuyển đổi** (`UserMapper`, `PitchMapper`, `BookingMapper`) đảm bảo an toàn dữ liệu, chống rò rỉ mã băm mật khẩu (`passwordHash`) và triệt tiêu lỗi `LazyInitializationException`.
  4. Triển khai cấu trúc phản hồi chuẩn hóa **`ApiResponse<T>`**.
  5. Viết bộ kiểm thử kiểm chứng **Tolerant Reader Pattern** và khả năng tuần tự hóa / giải tuần tự hóa JSON với Jackson (27/27 test cases passed).

---

## 2. CƠ SỞ LÝ THUYẾT: ANTI-PATTERN "DÙNG CHUNG MODEL" VÀ GIẢI PHÁP DTO

### 2.1. Phân Tích Rủi Ro Khi Dùng Chung Entity Model (Shared Entity Anti-Pattern)
Trong các dự án phân tán non trẻ, các lập trình viên thường mắc lỗi sao chép nguyên vẹn hoặc đóng gói các lớp `@Entity` (như `User`, `Pitch`, `Booking`) vào một thư viện chung để các dịch vụ khác import vào. Điều này gây ra 5 hệ lụy kỹ thuật nghiêm trọng:

```
[Anti-Pattern: Lộ Entity JPA]
+----------------------+           Shared JPA Entity           +------------------------+
|    Pitch Service     | ===================================> |    Booking Service     |
| (Entity: Pitch, GHP) | (Kèm @Table, @OneToMany, Hibernate)   | (Dính chặt schema DB)  |
+----------------------+                                       +------------------------+
        |                                                                  |
        +-----> Thay đổi cột trong CSDL ---> Phá vỡ Code Booking Service <--+
```

1. **Ghép nối lược đồ CSDL (Schema Leakage & Tight Coupling):**
   * Các annotation JPA (`@Table`, `@Column`, `@Id`, `@OneToMany`) phản ánh cấu trúc vật lý của CSDL. Khi chia sẻ Entity, chi tiết cài đặt riêng tư (Private Implementation Details) bị lộ ra ngoài biên giới dịch vụ (Service Boundary).
2. **Lỗi `LazyInitializationException` và Vòng lặp tuần hoàn JSON (Circular Dependency):**
   * Các quan hệ 1-N như `Pitch` có `List<GoldenHourPrice>` nếu dùng `FetchType.LAZY`, khi Jackson serialize sang JSON ngoài transaction sẽ kích hoạt lỗi `LazyInitializationException`. Nếu cả hai Entity tham chiếu ngược nhau (`@ManyToOne` $\leftrightarrow$ `@OneToMany`), Jackson sẽ bị tràn bộ nhớ do lặp vô tận (Infinite Recursion).
3. **Rò rỉ dữ liệu nhạy cảm (Security & Privacy Breach):**
   * Thực thể `User` chứa trường `password_hash` (mật khẩu đã băm) và các cờ nội bộ. Nếu trả về trực tiếp `User` qua API, mã băm mật khẩu sẽ truyền qua mạng tới client và các dịch vụ khác, tạo lỗ hổng bảo mật nghiêm trọng.
4. **Vi phạm nguyên tắc bao đóng Domain-Driven Design (DDD):**
   * Trong ngữ cảnh của `Booking Service`, nó chỉ quan tâm: *"Sân số 2 có tên là gì, giá mỗi giờ bao nhiêu, và có đang AVAILABLE không?"*. Nó hoàn toàn không cần biết sân đó có bao nhiêu khung giá giờ vàng, mặt cỏ bảo dưỡng ngày nào, hay chi tiết địa chỉ chi tiết ra sao.

### 2.2. Giải Pháp Chuẩn Hóa DTO Của Hùng
Hùng áp dụng mô hình **Contract-First DTO Architecture**:
* **DTO (Data Transfer Object):** Chỉ là các Java POJO thuần túy (Plain Old Java Objects) không chứa bất kỳ annotation JPA nào.
* **Tách biệt 100%:** DTO chỉ chứa đúng những trường dữ liệu mà bên tiêu thụ (Consumer) thực sự cần.
* **Bảo mật:** Loại bỏ hoàn toàn các trường nhạy cảm (`passwordHash`).
* **Tính toán trước (Pre-calculated Fields):** Tự động tính toán các trường phái sinh (ví dụ: `durationHours` được Mapper tính toán sẵn từ `startTime` và `endTime`).

---

## 3. MA TRẬN HỢP ĐỒNG TRAO ĐỔI DỮ LIỆU LIÊN DỊCH VỤ

Hùng đã quy chuẩn ma trận trao đổi dữ liệu giữa 4 thành viên (Hoàng, Linh, Hùng, Nhật) như sau:

| Chiều giao tiếp | Giao thức / Endpoint | DTO Yêu cầu (Request) | DTO Phản hồi (Response) | Mục đích nghiệp vụ |
|---|---|---|---|---|
| **Booking $\rightarrow$ Auth** | `POST /api/v1/auth/validate` | `TokenValidationRequestDTO` | `TokenValidationResponseDTO` | Xác thực JWT token, lấy `userId`, `username`, `role` |
| **Booking $\rightarrow$ Pitch** | `GET /api/pitches/{id}/price` | (Path Variable `id`) | `PriceResponseDTO` | Lấy nhanh đơn giá sân và kiểm tra trạng thái sân trống |
| **Client $\rightarrow$ Pitch** | `GET /api/pitches/{id}` | (Path Variable `id`) | `PitchResponseDTO` | Xem chi tiết thông tin sân và bảng giá giờ vàng |
| **Client $\rightarrow$ Booking** | `POST /api/bookings` | `BookingRequestDTO` | `ApiResponse<BookingResponseDTO>` | Tạo mới lượt đặt sân bóng |
| **Booking $\rightarrow$ Payment** | `POST /api/payments` | (Booking DTO snapshot) | `ApiResponse<PaymentDTO>` | Khởi tạo giao dịch thanh toán tiền sân |
| **Tất cả service** | Toàn bộ API trả về | — | `ApiResponse<T>` | Chuẩn hóa định dạng JSON phản hồi chung |

---

## 4. THIẾT KẾ CHI TIẾT CÁC LỚP DTO CHUẨN HÓA

Toàn bộ các lớp DTO được đóng gói trong package `com.football.data.dto.*`:

### 4.1. DTO Xác thực Liên Dịch Vụ: `TokenValidationResponseDTO`
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TokenValidationResponseDTO {
    private boolean valid;
    private Long userId;
    private String username;
    private String role;
    private String fullName;
    private String message;
}
```

### 4.2. DTO Báo Giá Sân Bóng Tinh Gọn: `PriceResponseDTO`
Được thiết kế riêng cho `Booking Service` gọi sang để tính tiền đơn đặt sân:
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceResponseDTO {
    private Long pitchId;
    private String pitchName;
    private String pitchType;
    private BigDecimal pricePerHour;
    @Builder.Default
    private String currency = "VND";
    private String status;
    private boolean available;
}
```
* **Ưu điểm:** Kích thước gói tin JSON nhỏ hơn 80% so với việc serialize toàn bộ Entity `Pitch`.

### 4.3. DTO Đơn Đặt Sân: `BookingResponseDTO`
Bổ sung trường phái sinh `durationHours` (số giờ thuê) để hỗ trợ tính tiền và hiển thị giao diện:
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingResponseDTO {
    private Long id;
    private Long userId;
    private Long pitchId;
    private String pitchName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double durationHours;      // Tính tự động: 18:00 -> 19:30 = 1.5 giờ
    private BigDecimal pricePerHour;
    private BigDecimal totalPrice;
    private String status;
    private String statusDescription; // "Chờ duyệt", "Đã duyệt", "Từ chối"
    private String note;
    private LocalDateTime createdAt;
}
```

### 4.4. Universal Response Envelope: `ApiResponse<T>`
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private LocalDateTime timestamp;
}
```

---

## 5. TẦNG CHUYỂN ĐỔI AN TOÀN (MAPPER LAYER)

Để chuyển đổi qua lại giữa Entity và DTO, Hùng cài đặt 3 Mapper độc lập trong package `com.football.data.mapper.*`:

### 5.1. `UserMapper`: Chống Rò Rỉ Mật Khẩu
```java
public static UserResponseDTO toDTO(User user) {
    if (user == null) return null;
    return UserResponseDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .role(user.getRole() != null ? user.getRole().name() : null)
            .roleDisplayName(user.getRole() != null ? user.getRole().getDisplayName() : null)
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .active(user.isActive())
            // TUYỆT ĐỐI KHÔNG MAP passwordHash
            .build();
}
```

### 5.2. `PitchMapper`: Xử Lý An Toàn Quan Hệ 1-N & Báo Giá Nhanh
```java
public static PriceResponseDTO toPriceResponseDTO(Pitch pitch) {
    if (pitch == null) return null;
    return PriceResponseDTO.builder()
            .pitchId(pitch.getId())
            .pitchName(pitch.getName())
            .pitchType(pitch.getType())
            .pricePerHour(pitch.getPricePerHour())
            .currency("VND")
            .status(pitch.getStatus())
            .available("AVAILABLE".equalsIgnoreCase(pitch.getStatus()))
            .build();
}
```

### 5.3. `BookingMapper`: Tự Động Tính Thời Lượng Thuê Sân
```java
public static BookingResponseDTO toDTO(Booking booking, String pitchName) {
    if (booking == null) return null;

    double durationHours = 0.0;
    if (booking.getStartTime() != null && booking.getEndTime() != null) {
        Duration duration = Duration.between(booking.getStartTime(), booking.getEndTime());
        durationHours = (double) duration.toMinutes() / 60.0;
    }

    return BookingResponseDTO.builder()
            .id(booking.getId())
            .userId(booking.getUserId())
            .pitchId(booking.getPitchId())
            .pitchName(pitchName)
            .startTime(booking.getStartTime())
            .endTime(booking.getEndTime())
            .durationHours(durationHours)
            .pricePerHour(booking.getPricePerHour())
            .totalPrice(booking.getTotalPrice())
            .status(booking.getStatus() != null ? booking.getStatus().name() : null)
            .statusDescription(booking.getStatus() != null ? booking.getStatus().getDescription() : null)
            .note(booking.getNote())
            .createdAt(booking.getCreatedAt())
            .build();
}
```

---

## 6. XỬ LÝ LỖI GIAO TIẾP MẠNG VÀ NGUYÊN LÝ TOLERANT READER

Trong môi trường phân tán, các sự cố mạng và không đồng bộ phiên bản là điều tất yếu. Thiết kế DTO của Hùng đáp ứng 2 nguyên lý quan trọng:

### 6.1. Nguyên Lý Tolerant Reader (Đầu Đọc Khoan Dung)
* Khi `pitchs-service` nâng cấp và bổ sung thêm các trường mới trong JSON (ví dụ: `futureFieldA`, `serverZone`), `booking-service` khi giải mã JSON **không được phép bị ném ngoại lệ `UnrecognizedPropertyException`**.
* Cấu hình giải pháp:
  ```java
  objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  ```
* Bài test `testTolerantReaderPattern` trong [InterServiceSerializationContractTest.java](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/data-layer/src/test/java/com/football/data/dto/InterServiceSerializationContractTest.java) chứng minh hệ thống vẫn đọc dữ liệu ổn định khi payload JSON có thêm các trường lạ.

### 6.2. Ứng Phó Với Các Mã Trạng Thái HTTP Giữa Các Dịch Vụ
* **200 OK / 201 Created:** Dịch vụ phản hồi hợp lệ với dữ liệu dạng DTO.
* **404 Not Found:** Khi `booking-service` gọi `GET /api/pitches/{id}/price` nhưng sân không tồn tại $\rightarrow$ Trả về mã lỗi thân thiện cho khách đặt sân.
* **503 Service Unavailable:** Khi `pitchs-service` bị tắt hoặc mất mạng $\rightarrow$ `booking-service` kích hoạt Circuit Breaker hoặc Fallback, thông báo *"Dịch vụ quản lý sân hiện không khả dụng, vui lòng thử lại sau"*.

---

## 7. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (JUNIT 5 & SERIALIZATION TESTS)

Hùng đã xây dựng thêm 4 bộ test kiểm chứng hợp đồng DTO và Mapper:
1. [UserMapperTest.java](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/data-layer/src/test/java/com/football/data/mapper/UserMapperTest.java): Kiểm thử bảo mật (mật khẩu bị loại bỏ), null-safety.
2. [PitchMapperTest.java](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/data-layer/src/test/java/com/football/data/mapper/PitchMapperTest.java): Kiểm thử chuyển đổi sang `PitchResponseDTO` và `PriceResponseDTO`.
3. [BookingMapperTest.java](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/data-layer/src/test/java/com/football/data/mapper/BookingMapperTest.java): Kiểm thử tính toán `durationHours` (18:00 - 19:30 = 1.5 giờ) và ánh xạ trạng thái.
4. [InterServiceSerializationContractTest.java](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/data-layer/src/test/java/com/football/data/dto/InterServiceSerializationContractTest.java): Kiểm thử tuần tự hóa JSON với Jackson, kiểm chứng hợp đồng `TokenValidationResponseDTO`, `PriceResponseDTO`, `ApiResponse<T>` và Tolerant Reader Pattern.

### Kết Quả Thực Thi Thực Tế (`mvn test`):
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.football.data.auth.UserRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.football.data.booking.BookingRepositoryTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.football.data.dto.InterServiceSerializationContractTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.football.data.mapper.BookingMapperTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.football.data.mapper.PitchMapperTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.football.data.mapper.UserMapperTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.football.data.pitch.PitchRepositoryTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  10.077 s
```
$\implies$ **Toàn bộ 27/27 Test Cases ĐẠT 100% (BUILD SUCCESS)**.

---

## 8. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN CHO BUỔI TH3

1. **Đánh giá kết quả TH2:**
   * Sinh viên đã giải quyết dứt điểm bài toán **"Chuẩn DTO trao đổi giữa service, tránh trùng model"**.
   * Bộ DTO và Mapper hoạt động độc lập, an toàn, có khả năng tương thích cao với tất cả các HTTP Client (Spring `RestClient`, `WebClient`, Spring Cloud `OpenFeign`, Axios/Node.js).
2. **Kế hoạch cho buổi TH3:**
   * Tiếp tục chuẩn hóa định dạng phản hồi dữ liệu và cơ chế bắt lỗi tập trung `@ControllerAdvice`.
   * Hỗ trợ Nhật và Linh xây dựng chuẩn Response lỗi chung (HTTP 400 Validation, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict).
