package com.cyclinglab.platform.common;

import com.cyclinglab.platform.auth.AuthException;
import com.cyclinglab.platform.library.exception.StructureValidationException;
import com.cyclinglab.platform.library.exception.TemplateNameConflictException;
import com.cyclinglab.platform.library.exception.TemplateNotFoundException;
import com.cyclinglab.platform.plan.exception.DailyPlanNotFoundException;
import com.cyclinglab.platform.workout.exception.WorkoutFileNotFoundException;
import com.cyclinglab.platform.plan.exception.WeeklyPlanConflictException;
import com.cyclinglab.platform.plan.exception.WeeklyPlanNotFoundException;
import com.cyclinglab.platform.training.exception.TrainingFileNotFoundException;
import com.cyclinglab.platform.review.exception.ReviewConflictException;
import com.cyclinglab.platform.review.exception.ReviewNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(String code, String message, String path, Instant timestamp, Object details) {}

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "AUTH_FAILED", ex.getMessage(), req, null);
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTemplateNotFound(TemplateNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
    }

    @ExceptionHandler(WeeklyPlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWeeklyPlanNotFound(WeeklyPlanNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
    }

    @ExceptionHandler(DailyPlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDailyPlanNotFound(DailyPlanNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
    }

    @ExceptionHandler(WorkoutFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkoutFileNotFound(WorkoutFileNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
    }

    @ExceptionHandler(TrainingFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrainingFileNotFound(TrainingFileNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReviewNotFound(ReviewNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
    }

    @ExceptionHandler(ReviewConflictException.class)
    public ResponseEntity<ErrorResponse> handleReviewConflict(ReviewConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "REVIEW_EXISTS", ex.getMessage(), req, null);
    }

    @ExceptionHandler(StructureValidationException.class)
    public ResponseEntity<ErrorResponse> handleStructure(StructureValidationException ex, HttpServletRequest req) {
        List<Map<String, String>> details = ex.getDetails().stream()
            .map(d -> Map.of("pointer", d.pointer(), "message", d.message()))
            .toList();
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_STRUCTURE", ex.getMessage(), req, details);
    }

    @ExceptionHandler(TemplateNameConflictException.class)
    public ResponseEntity<ErrorResponse> handleNameConflict(TemplateNameConflictException ex, HttpServletRequest req) {
        List<Map<String, Object>> details = List.of(
            Map.of("field", "name", "message", ex.getMessage(), "conflictWith", ex.getConflict().conflictWith().toString())
        );
        return build(HttpStatus.CONFLICT, "DUPLICATE_NAME", ex.getMessage(), req, details);
    }

    @ExceptionHandler(WeeklyPlanConflictException.class)
    public ResponseEntity<ErrorResponse> handleWeeklyPlanConflict(WeeklyPlanConflictException ex, HttpServletRequest req) {
        List<Map<String, Object>> details = List.of(
            Map.of("field", "isoYear+isoWeek", "message", ex.getMessage(),
                   "conflictWith", ex.getConflict().conflictWith().toString())
        );
        return build(HttpStatus.CONFLICT, "WEEK_PLAN_EXISTS", ex.getMessage(), req, details);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldDetail)
            .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", req, details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", req, null);
    }

    private Map<String, String> toFieldDetail(FieldError fe) {
        return Map.of("field", fe.getField(), "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest req, Object details) {
        return ResponseEntity.status(status).body(new ErrorResponse(
            code, message, req.getRequestURI(), Instant.now(), details
        ));
    }
}
