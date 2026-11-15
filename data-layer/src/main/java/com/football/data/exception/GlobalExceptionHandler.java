package com.football.data.exception;

import com.football.data.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bộ Xử Lý Ngoại Lệ Tập Trung Chuẩn Hóa (@ControllerAdvice / @RestControllerAdvice)
 * 
 * =========================================================================
 * VAI TRÒ CỦA HÙNG TRONG BUỔI TH3:
 * 1. Chuẩn hóa format phản hồi lỗi (Error Contract) cho toàn bộ hệ thống.
 * 2. Bắt và phân loại các ngoại lệ theo mã HTTP Status tương ứng:
 *    - 400: Lỗi cú pháp, sai tham số, vi phạm validation Bean.
 *    - 401: Chưa xác thực danh tính hoặc token không hợp lệ.
 *    - 403: Không có quyền truy cập tài nguyên.
 *    - 404: Không tìm thấy tài nguyên (User, Pitch, Booking).
 *    - 409: Xung đột tài nguyên (Trùng lịch thi đấu, trùng username).
 *    - 405: Sai phương thức HTTP Method.
 *    - 503: Dịch vụ đối tác mất kết nối (Service-to-Service fault).
 *    - 500: Lỗi hệ thống ngoài tầm kiểm soát.
 * =========================================================================
 * 
 * Phụ trách: HÙNG (feat/hung-data) - Tuần 3 (TH3)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. Xử lý lỗi Bean Validation (@Valid trên Request Body) -> 400 Bad Request
     */
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

    /**
     * 2. Xử lý lỗi ConstraintViolationException (@Validated trên PathVariable / RequestParam) -> 400
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String field = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            fieldErrors.put(field, violation.getMessage());
        });

        ErrorResponse response = ErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "Tham số đường dẫn hoặc query parameter không hợp lệ",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 3. Xử lý thiếu tham số bắt buộc trong request -> 400 Bad Request
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request - Missing Parameter",
                String.format("Thiếu tham số bắt buộc: '%s' (kiểu %s)", ex.getParameterName(), ex.getParameterType()),
                request.getRequestURI(),
                "ERR_MISSING_PARAM"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 4. Xử lý lỗi nghiệp vụ BadRequestException & IllegalArgumentException -> 400 Bad Request
     */
    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI(),
                "ERR_BAD_REQUEST"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 5. Xử lý lỗi không tìm thấy tài nguyên -> 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                "ERR_RESOURCE_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 6. Xử lý lỗi xung đột dữ liệu (Trùng lịch thi đấu, trùng username) -> 409 Conflict
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI(),
                "ERR_RESOURCE_CONFLICT"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 7. Xử lý lỗi chưa xác thực hoặc token hết hạn -> 401 Unauthorized
     */
    @ExceptionHandler({UnauthorizedException.class, org.springframework.security.core.AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage() != null ? ex.getMessage() : "Chưa xác thực hoặc token không hợp lệ",
                request.getRequestURI(),
                "ERR_UNAUTHORIZED"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 8. Xử lý lỗi từ chối truy cập / không đủ thẩm quyền -> 403 Forbidden
     */
    @ExceptionHandler({ForbiddenException.class, org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage() != null ? ex.getMessage() : "Bạn không có quyền truy cập tài nguyên này",
                request.getRequestURI(),
                "ERR_FORBIDDEN"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 9. Xử lý lỗi dịch vụ đối tác mất kết nối -> 503 Service Unavailable
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ex.getMessage(),
                request.getRequestURI(),
                "ERR_SERVICE_DOWN"
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 10. Xử lý sai phương thức HTTP Method (VD: POST nhầm sang GET) -> 405 Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed",
                String.format("Phương thức HTTP '%s' không được hỗ trợ cho đường dẫn này", ex.getMethod()),
                request.getRequestURI(),
                "ERR_METHOD_NOT_ALLOWED"
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * 11. Bắt ngoại lệ chưa kiểm soát (Catch-all) -> 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi không xác định trên máy chủ",
                request.getRequestURI(),
                "ERR_INTERNAL_SERVER"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

