package com.pitchos.booking.exception;

import com.pitchos.booking.dto.ErrorResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Xử lý lỗi tập trung, mọi lỗi trả cùng định dạng ErrorResponse {timestamp, status, error, message, path, details}.
 * Thông điệp cho client chung chung; chi tiết kỹ thuật chỉ ghi log.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String INTERNAL_ERROR_MESSAGE = "Đã có lỗi hệ thống, vui lòng thử lại sau";

    /**
     * Pitch Service bị tắt tiến trình / mất mạng / quá thời gian chờ / lỗi 5xx / dữ liệu sai:
     * chuyển thành 503 thay vì 500, Booking Service vẫn hoạt động (ngăn sụp đổ dây chuyền - Cascading Failure).
     */
    @ExceptionHandler(PitchServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePitchUnavailable(PitchServiceUnavailableException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    /** Giữ từ bản cũ: FeignException lọt ra ngoài (nếu có) cũng trả 503. RetryableException là lớp con. */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeign(FeignException ex, HttpServletRequest request) {
        log.warn("Không gọi được Pitch Service: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, PitchServiceUnavailableException.DEFAULT_MESSAGE, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Hai thao tác cùng sửa một đơn (ví dụ chủ sân duyệt đúng lúc khách hủy): thao tác sau nhận 409. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Đơn vừa được cập nhật bởi thao tác khác, vui lòng tải lại", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex,
                                                         HttpServletRequest request) {
        log.warn("Vi phạm ràng buộc dữ liệu: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return build(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc xung đột, vui lòng thử lại", request);
    }

    /** Lỗi @Valid trên body: message = lỗi đầu tiên (tương thích bản cũ), details = toàn bộ lỗi từng trường. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Dữ liệu gửi lên không hợp lệ"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return build(HttpStatus.BAD_REQUEST, message, request, details);
    }

    /** Lỗi ràng buộc trên tham số query (page, size, pitchId...). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(HandlerMethodValidationException ex,
                                                               HttpServletRequest request) {
        List<String> details = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Tham số không hợp lệ", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Tham số không hợp lệ", request, details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Giá trị của tham số '" + ex.getName() + "' không hợp lệ", request);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ErrorResponse> handleMissingParameter(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không đúng định dạng", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Không tìm thấy đường dẫn", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex,
                                                      HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức " + ex.getMethod() + " không được hỗ trợ", request);
    }

    /** Lỗi chưa lường trước: 500 với thông điệp chung, chi tiết chỉ ghi log server. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Lỗi không mong đợi khi xử lý {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE, request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        return build(status, message, request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                List<String> details) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, request.getRequestURI(), details));
    }
}
