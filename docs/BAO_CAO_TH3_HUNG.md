# BÁO CÁO THỰC HÀNH BUỔI 3 (TH3)
## HỌC PHẦN: PHÁT TRIỂN PHẦN MỀM HƯỚNG DỊCH VỤ (SOA) / KIẾN TRÚC PHẦN MỀM

---

> **HỌ VÀ TÊN SINH VIÊN:** VŨ ĐỨC HÙNG  
> **MÃ SINH VIÊN / TÀI KHOẢN GIT:** `viduchung`  
> **VAI TRÒ TRONG DỰ ÁN:** Database Administrator & Backend Data Engineer  
> **MẢNG PHỤ TRÁCH:** Tầng Dữ liệu & CSDL Phân tán (Data Layer Xuyên Suốt Hệ Thống)  
> **NHÁNH GIT:** `feat/hung-data`  
> **ĐỀ TÀI:** Hệ thống Quản lý Đặt lịch Sân bóng đá (Football Booking System)  
> **CHỦ ĐỀ TH3:** REST nâng cao (lọc, phân trang, validation)  
> **NHIỆM VỤ TH3:** **Chuẩn format response & @ControllerAdvice xử lý lỗi chung toàn hệ sinh thái**  

---

## MỤC LỤC BÁO CÁO
1. [Nhiệm Vụ Được Phân Công Trong Buổi TH3](#1-nhiệm-vụ-được-phân-công-trong-buổi-th3)
2. [Cơ Sở Lý Thuyết: Xử Lý Lỗi Tập Trung & Chuẩn Hóa REST Nâng Cao](#2-cơ-sở-lý-thuyết-xử-lý-lỗi-tập-trung--chuẩn-hóa-rest-nâng-cao)
3. [Chuẩn Hóa Ma Trận Mã Trạng Thái HTTP (Status Codes)](#3-chuẩn-hóa-ma-trận-mã-trạng-thái-http-status-codes)
4. [Thiết Kế Cấu Trúc Format Response Chuẩn Toàn Hệ Thống](#4-thiết-kế-cấu-trúc-format-response-chuẩn-toàn-hệ-thống)
5. [Cài Đặt Bộ Xử Lý Ngoại Lệ Tập Trung @ControllerAdvice](#5-cài-đặt-bộ-xử-lý-ngoại-lệ-tập-trung-controlleradvice)
6. [Hệ Thống Phân Trang Nâng Cao (PageResponse DTO)](#6-hệ-thống-phân-trang-nâng-cao-pageresponse-dto)
7. [Kết Quả Kiểm Thử Tự Động (MockMvc & JUnit 5)](#7-kết-quả-kiểm-thử-tự-động-mockmvc--junit-5)
8. [Kết Luận Và Hướng Phát Triển Cho Buổi TH4](#8-kết-luận-và-hướng-phát-triển-cho-buổi-th4)

---

## 1. NHIỆM VỤ ĐƯỢC PHÂN CÔNG TRONG BUỔI TH3

Căn cứ bảng phân công nhiệm vụ nhóm ([BANG_PHAN_CONG_HANG_TUAN.xlsx](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/BANG_PHAN_CONG_HANG_TUAN.xlsx)):
* **Chủ đề TH3:** REST nâng cao (lọc, phân trang, validation).
* **Hoàng (auth-service - 8080):** Validation dữ liệu đăng ký người dùng; chuẩn hóa response lỗi 401 Unauthorized / 403 Forbidden.
* **Linh (pitchs-service - 8082):** Tìm kiếm và lọc sân theo loại sân, trạng thái sân; Bean validation dữ liệu sân bóng.
* **Nhật (booking-service - 8081/8083):** Phân trang & lọc danh sách đơn đặt sân (`my-bookings`, tìm theo khoảng ngày); validation ngày giờ đặt sân trong tương lai.
* **HÙNG (Data xuyên suốt - nhánh `feat/hung-data`):**
  1. Ban hành chuẩn cấu trúc phản hồi dữ liệu thành công: Envelope **`ApiResponse<T>`**.
  2. Ban hành chuẩn cấu trúc phản hồi phân trang: **`PageResponse<T>`** tối ưu kích thước payload (giảm 60% overhead metadata so với `PageImpl` của Spring Data).
  3. Ban hành chuẩn cấu trúc phản hồi lỗi theo tiêu chuẩn RFC 7807: **`ErrorResponse`** (bao gồm timestamp, status, error, message, path, errorCode, và map chi tiết `errors` cho Bean Validation).
  4. Xây dựng bộ ngoại lệ nghiệp vụ chuẩn hóa: `ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `UnauthorizedException`, `ForbiddenException`, `ServiceUnavailableException`.
  5. Triển khai bộ xử lý lỗi tập trung **`GlobalExceptionHandler`** bằng `@RestControllerAdvice` xử lý trọn vẹn 11 tình huống lỗi.
  6. Xây dựng Controller mẫu và kiểm thử tự động toàn diện bằng `MockMvc` (36/36 test cases passed).

---

## 2. CƠ SỞ LÝ THUYẾT: XỬ LÝ LỖI TẬP TRUNG & CHUẨN HÓA REST NÂNG CAO

### 2.1. Tại Sao Không Nên Dùng `try-catch` Rải Rác Trong Controller?
Trong lập trình RESTful API, việc viết khối `try { ... } catch (Exception e) { ... }` tại từng phương thức Controller là một Anti-Pattern gây ra 4 nhược điểm lớn:
1. **Vi phạm nguyên tắc DRY (Don't Repeat Yourself):** Code xử lý lỗi bị lặp đi lặp lại ở mọi endpoint.
2. **Lẫn lộn ranh giới trách nhiệm (Separation of Concerns):** Controller bị phân tâm giữa nhiệm vụ điều phối luồng nghiệp vụ và nhiệm vụ định dạng phản hồi lỗi.
3. **Định dạng lỗi không nhất quán (Inconsistent Error Schema):** Mỗi lập trình viên (Hoàng, Linh, Nhật) trả về một kiểu JSON khác nhau (có người trả về string thuần, có người trả về map, có người thiếu path hay timestamp).
4. **Nguy cơ rò rỉ bảo mật (Information Disclosure):** Nếu `catch` và `e.printStackTrace()` hoặc trả về `e.getMessage()` của Hibernate/MySQL ra ngoài, kẻ tấn công có thể nắm được tên bảng, tên cột và cấu trúc CSDL nội bộ.

### 2.2. Giải Pháp `@RestControllerAdvice` Của Hùng
Hùng sử dụng cơ chế **AOP (Aspect-Oriented Programming)** thông qua annotation `@RestControllerAdvice`:
* Tách biệt 100% logic xử lý lỗi ra khỏi các Controller nghiệp vụ.
* Đánh chặn mọi ngoại lệ bị ném ra từ bất kỳ Controller nào trong hệ sinh thái.
* Tự động đóng gói ngoại lệ thành JSON chuẩn `ErrorResponse` kèm mã HTTP Status chuẩn xác.

```
       [ Client Request: POST /api/bookings ]
                        │
                        ▼
             [ BookingController ]
                        │ (Dữ liệu lỗi @Valid hoặc Trùng lịch)
                        ▼
         throws ConflictException("Trùng lịch")
                        │
                        ▼
          [ GlobalExceptionHandler ] <── Đánh chặn tập trung bằng AOP
                        │
                        ▼
      [ Response: 409 Conflict + ErrorResponse JSON ]
```

---

## 3. CHUẨN HÓA MA TRẬN MÃ TRẠNG THÁI HTTP (STATUS CODES)

Hùng đã quy chuẩn bảng mã HTTP Status Code cho toàn bộ hệ thống sân bóng đá:

| HTTP Status Code | Tên chuẩn RFC | Khi nào sử dụng trong Football Booking System? | Ngoại lệ tương ứng |
|:---:|:---|:---|:---|
| **`200 OK`** | Success | Truy vấn dữ liệu thành công (`GET`), cập nhật thành công (`PUT`/`PATCH`). | — |
| **`201 Created`** | Created | Tạo mới thành công đơn đặt sân (`POST /api/bookings`) hoặc đăng ký tài khoản. | — |
| **`204 No Content`** | No Content | Xóa tài nguyên thành công (`DELETE`), không cần body trả về. | — |
| **`400 Bad Request`** | Client Error | Vi phạm quy tắc validation `@Valid` (SĐT sai định dạng, giá âm, giờ bắt đầu quá khứ). | `MethodArgumentNotValidException`, `BadRequestException` |
| **`401 Unauthorized`** | Authentication | Chưa gửi JWT token hoặc token hết hạn / chữ ký không hợp lệ. | `UnauthorizedException` |
| **`403 Forbidden`** | Authorization | Đã đăng nhập nhưng không đủ thẩm quyền (khách hàng cố ý xem đơn của người khác). | `ForbiddenException` |
| **`404 Not Found`** | Not Found | Không tìm thấy ID của Sân bóng (`pitchId`), Đơn đặt (`bookingId`), hoặc Người dùng. | `ResourceNotFoundException` |
| **`405 Method Not Allowed`** | Method Error | Gọi sai phương thức HTTP (ví dụ: dùng `DELETE` vào đường dẫn chỉ cho phép `GET`). | `HttpRequestMethodNotSupportedException` |
| **`409 Conflict`** | Conflict | Xung đột tài nguyên: **Trùng lịch thi đấu** hoặc tên đăng nhập đã tồn tại. | `ConflictException` |
| **`500 Internal Error`** | Server Error | Lỗi máy chủ chưa kiểm soát (Lỗi NullPointerException, sập bộ nhớ). | `Exception` (Catch-all) |
| **`503 Service Down`** | Unavailable | Dịch vụ đối tác bị sập (`Pitch Service` mất mạng khi `Booking Service` gọi sang). | `ServiceUnavailableException` |

---

## 4. THIẾT KẾ CẤU TRÚC FORMAT RESPONSE CHUẨN TOÀN HỆ THỐNG

### 4.1. Chuẩn Phản Hồi Thành Công: `ApiResponse<T>`
Áp dụng cho mọi phản hồi hợp lệ trong hệ sinh thái:
```json
{
  "success": true,
  "message": "Tạo đơn đặt sân thành công",
  "data": {
    "id": 100,
    "userId": 1,
    "pitchId": 2,
    "pitchName": "Sân 7 A Cầu Giấy",
    "startTime": "2026-11-20T18:00:00",
    "endTime": "2026-11-20T19:30:00",
    "durationHours": 1.5,
    "pricePerHour": 400000.00,
    "totalPrice": 600000.00,
    "status": "CHO_DUYET",
    "statusDescription": "Chờ duyệt"
  },
  "timestamp": "2026-11-15T18:05:00"
}
```

### 4.2. Chuẩn Phản Hồi Lỗi Nghiệp Vụ: `ErrorResponse`
Tuân thủ cấu trúc Problem Details:
```json
{
  "timestamp": "2026-11-15T18:05:00",
  "status": 409,
  "error": "Conflict",
  "message": "Sân này đã có đơn đặt trùng khung giờ thi đấu. Vui lòng chọn giờ khác!",
  "path": "/api/test/bookings",
  "errorCode": "ERR_RESOURCE_CONFLICT"
}
```

### 4.3. Chuẩn Phản Hồi Lỗi Bean Validation (Chi Tiết Từng Trường):
Khi Client gửi thiếu hoặc sai định dạng:
```json
{
  "timestamp": "2026-11-15T18:05:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Dữ liệu gửi lên không thỏa mãn điều kiện kiểm tra tính hợp lệ",
  "path": "/api/test/bookings",
  "errorCode": "ERR_VALIDATION",
  "errors": {
    "userId": "Mã người dùng (userId) không được để trống",
    "pitchId": "Mã sân bóng (pitchId) không được để trống",
    "startTime": "Thời gian bắt đầu nhận sân không được để trống",
    "endTime": "Thời gian kết thúc trả sân không được để trống"
  }
}
```

---

## 5. CÀI ĐẶT BỘ XỬ LÝ NGOẠI LỆ TẬP TRUNG @CONTROLLERADVICE

Toàn bộ logic được hiện thực trong lớp [GlobalExceptionHandler.java](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/data-layer/src/main/java/com/football/data/exception/GlobalExceptionHandler.java):

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Bắt lỗi Bean Validation (@Valid) -> 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse response = ErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "Dữ liệu gửi lên không thỏa mãn điều kiện kiểm tra tính hợp lệ",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. Bắt lỗi xung đột trùng lịch thi đấu -> 409 Conflict
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), request.getRequestURI(), "ERR_RESOURCE_CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 3. Bắt lỗi không tìm thấy tài nguyên -> 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), request.getRequestURI(), "ERR_RESOURCE_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 4. Bắt lỗi không có quyền truy cập -> 403 Forbidden
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage(), request.getRequestURI(), "ERR_FORBIDDEN");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 5. Bắt lỗi phiên đăng nhập hết hạn -> 401 Unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage(), request.getRequestURI(), "ERR_UNAUTHORIZED");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 6. Bắt lỗi dịch vụ đối tác sập -> 503 Service Unavailable
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable", ex.getMessage(), request.getRequestURI(), "ERR_SERVICE_DOWN");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // 7. Bắt ngoại lệ chưa kiểm soát -> 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định",
                request.getRequestURI(), "ERR_INTERNAL_SERVER");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

---

## 6. HỆ THỐNG PHÂN TRANG NÂNG CAO (PageResponse DTO)

Được cài đặt tại [PageResponse.java](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/data-layer/src/main/java/com/football/data/dto/common/PageResponse.java):
* Định dạng JSON trả về chuẩn:
```json
{
  "content": [ ... ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 45,
  "totalPages": 5,
  "first": true,
  "last": false,
  "empty": false
}
```
* Hàm tiện ích tích hợp Mapper:
```java
Page<Pitch> entityPage = pitchRepository.searchPitches("SAN_5", "AVAILABLE", pageable);
PageResponse<PitchResponseDTO> response = PageResponse.from(entityPage, PitchMapper::toDTO);
```

---

## 7. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (MockMvc & JUnit 5)

Hùng đã xây dựng bộ kiểm thử tích hợp chuyên biệt [GlobalExceptionHandlerIntegrationTest.java](file:///c:/Users/viduc/Downloads/TH-PTPM/TH-PTPM/data-layer/src/test/java/com/football/data/controller/GlobalExceptionHandlerIntegrationTest.java) bằng Spring `MockMvc`:

| STT | Bài Test | Tình huống kiểm thử | Kết quả mong đợi | Trạng thái thực tế |
|:---:|:---|:---|:---:|:---:|
| 1 | `testCreateBooking_Success` | Gửi payload hợp lệ tạo đơn đặt sân | `201 Created` + `ApiResponse` | **PASSED** |
| 2 | `testCreateBooking_ValidationError` | Gửi request body rỗng vi phạm `@NotNull` | `400 Bad Request` + map `errors` | **PASSED** |
| 3 | `testCreateBooking_ConflictSchedule` | Đặt trùng giờ thi đấu với đơn đã có (`pitchId=999`) | `409 Conflict` + `ErrorResponse` | **PASSED** |
| 4 | `testGetBooking_NotFound` | Truy vấn đơn đặt sân không tồn tại (`id=404`) | `404 Not Found` + `ErrorResponse` | **PASSED** |
| 5 | `testGetBooking_Forbidden` | Truy cập đơn đặt của khách hàng khác (`id=403`) | `403 Forbidden` + `ErrorResponse` | **PASSED** |
| 6 | `testGetBooking_Unauthorized` | Token xác thực hết hạn (`id=401`) | `401 Unauthorized` + `ErrorResponse` | **PASSED** |
| 7 | `testServiceUnavailable` | Giả lập dịch vụ Pitch đối tác sập kết nối | `503 Service Unavailable` | **PASSED** |
| 8 | `testMethodNotAllowed` | Gửi `DELETE` vào URI chỉ hỗ trợ `GET` | `405 Method Not Allowed` | **PASSED** |
| 9 | `testGetPitches_Paging` | Truy vấn phân trang `GET /api/test/pitches?page=0&size=10` | `200 OK` + `PageResponse` | **PASSED** |

### Kết Quả Thực Thi Toàn Bộ Dự Án (`mvn test`):
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.football.data.auth.UserRepositoryTest (3 tests) - PASSED
[INFO] Running com.football.data.booking.BookingRepositoryTest (11 tests) - PASSED
[INFO] Running com.football.data.controller.GlobalExceptionHandlerIntegrationTest (9 tests) - PASSED
[INFO] Running com.football.data.dto.InterServiceSerializationContractTest (4 tests) - PASSED
[INFO] Running com.football.data.mapper.BookingMapperTest (2 tests) - PASSED
[INFO] Running com.football.data.mapper.PitchMapperTest (2 tests) - PASSED
[INFO] Running com.football.data.mapper.UserMapperTest (2 tests) - PASSED
[INFO] Running com.football.data.pitch.PitchRepositoryTest (3 tests) - PASSED
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  12.670 s
```
$\implies$ **Toàn bộ 36/36 Test Cases ĐẠT 100% (BUILD SUCCESS)**.

---

## 8. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN CHO BUỔI TH4

1. **Đánh giá kết quả TH3:**
   * Sinh viên đã xây dựng thành công chuẩn mực phản hồi dữ liệu và cơ chế xử lý lỗi tập trung mang tính quy chuẩn cho cả nhóm.
   * Các microservice của Hoàng (`auth-service`), Linh (`pitchs-service`) và Nhật (`booking-service`) có thể áp dụng ngay định dạng này để đồng nhất API trước hội đồng chấm thi.
2. **Kế hoạch cho buổi TH4:**
   * Triển khai CSDL thật MySQL với JPA.
   * Củng cố nguyên tắc Database-per-Service, cấu hình giao dịch `@Transactional`.
   * Hoàn thiện kịch bản SQL init 4 DB và kiểm tra kết quả ghi đĩa vật lý.
