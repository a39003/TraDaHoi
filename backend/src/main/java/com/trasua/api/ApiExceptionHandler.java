package com.trasua.api;

import com.trasua.support.BusinessRuleException;
import com.trasua.support.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException error) {
        return error(HttpStatus.NOT_FOUND, error.getMessage(), Map.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> conflict(BusinessRuleException error) {
        return error(HttpStatus.CONFLICT, error.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException error) {
        Map<String, String> fields = error.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(field -> field.getField(), field -> field.getDefaultMessage(), (first, ignored) -> first));
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên chưa hợp lệ", fields);
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<ApiError> arithmetic(ArithmeticException error) {
        return error(HttpStatus.BAD_REQUEST, "Số tiền không hợp lệ", Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> uploadTooLarge(MaxUploadSizeExceededException error) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh tải lên vượt quá dung lượng cho phép", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException error) {
        return error(HttpStatus.BAD_REQUEST, "Nội dung gửi lên sai định dạng hoặc thiếu dữ liệu", Map.of());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ApiError> missingRequest(Exception error) {
        return error(HttpStatus.BAD_REQUEST, "Yêu cầu đang thiếu thông tin bắt buộc", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> wrongType(MethodArgumentTypeMismatchException error) {
        return error(HttpStatus.BAD_REQUEST, "Giá trị '" + error.getName() + "' không đúng định dạng", Map.of());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> invalidUpload(MultipartException error) {
        return error(HttpStatus.BAD_REQUEST, "Không thể đọc ảnh tải lên. Hãy chọn lại tệp ảnh hợp lệ", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> dataConflict(DataIntegrityViolationException error) {
        return error(HttpStatus.CONFLICT, "Không thể xóa hoặc thay đổi vì dữ liệu này đang được sử dụng", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception error) {
        log.error("Unhandled API error", error);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Máy chủ gặp lỗi khi xử lý thao tác. Vui lòng thử lại", Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), message, fieldErrors));
    }

    public record ApiError(Instant timestamp, int status, String message, Map<String, String> fieldErrors) {
    }
}
