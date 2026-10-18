package com.hrcoresystem.hrcore_api.common.exception;

import com.hrcoresystem.hrcore_api.common.response.ApiErrorResponse;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.application.exception.FaceRecognitionUnavailableException;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.exception.AuthenticationFailedException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiErrorResponse.of("Recurso no encontrado", safeMessage(exception.getMessage())));
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse.of("Solicitud inválida", safeMessage(exception.getMessage())));
  }

  @ExceptionHandler(BusinessConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleBusinessConflict(BusinessConflictException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiErrorResponse.of("La operación presenta un conflicto", safeMessage(exception.getMessage())));
  }

  @ExceptionHandler(InvalidBusinessStateException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidBusinessState(InvalidBusinessStateException exception) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(ApiErrorResponse.of("La operación no está disponible en el estado actual", safeMessage(exception.getMessage())));
  }

  @ExceptionHandler(AuthenticationFailedException.class)
  public ResponseEntity<ApiErrorResponse> handleAuthenticationFailed(AuthenticationFailedException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiErrorResponse.of("No se pudo autenticar al usuario", safeMessage(exception.getMessage())));
  }

  @ExceptionHandler(FaceRecognitionUnavailableException.class)
  public ResponseEntity<ApiErrorResponse> handleFaceRecognitionUnavailable(
      FaceRecognitionUnavailableException exception) {
    String reason = exception.getMessage();
    if (reason == null || reason.isBlank()) {
      reason = "El servicio de reconocimiento facial no está disponible";
    }

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiErrorResponse.of(
            "El servicio de reconocimiento facial no está disponible",
            List.of()));
  }

  @ExceptionHandler(TenantResolutionException.class)
  public ResponseEntity<ApiErrorResponse> handleTenantResolutionException(TenantResolutionException exception) {
    String message = exception.getMessage();
    String userMessage = "La configuración del tenant no es válida";
    String guidance = "Verifique el identificador del tenant e inténtelo nuevamente.";

    if (message != null && !message.isBlank()) {
      if (message.contains("missing")) {
        userMessage = "Falta el identificador del tenant";
      } else if (message.contains("was not found")) {
        userMessage = "El tenant no existe";
      } else if (message.contains("not ready")) {
        userMessage = "El esquema del tenant aún no está disponible";
      } else if (message.contains("does not match any registered tenant")) {
        userMessage = "El tenant no existe";
      } else if (message.contains("invalid")) {
        userMessage = "El valor del tenant no es válido";
      }
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse.of(
            userMessage,
            List.of(guidance,
                "Verifique el identificador del tenant y que su esquema esté disponible.")));
  }

  @ExceptionHandler({ AccessDeniedException.class, AuthorizationDeniedException.class })
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(RuntimeException exception) {
    String reason = exception.getMessage();
    if (reason == null || reason.isBlank()) {
      reason = "No tiene permisos para realizar esta operación";
    }

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiErrorResponse.of(
            "No tiene permisos para realizar esta operación",
            List.of()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
    List<String> errors = exception.getBindingResult()
        .getAllErrors()
        .stream()
        .map(error -> {
          if (error instanceof FieldError fieldError) {
            return fieldError.getField() + ": " + fieldError.getDefaultMessage();
          }
          return error.getDefaultMessage();
        })
        .toList();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse.of("Los datos enviados no son válidos", errors));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
    List<String> errors = exception.getConstraintViolations()
        .stream()
        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
        .toList();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse.of("Los datos enviados no son válidos", errors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
    String reason = "El cuerpo de la solicitud tiene un formato inválido";

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse.of("El contenido de la solicitud no es válido", List.of(reason)));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
    String reason = "El archivo supera el tamaño permitido";

    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(ApiErrorResponse.of(
            "El archivo supera el tamaño permitido",
            List.of(reason)));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
    String field = exception.getName() != null ? exception.getName() : "parameter";
    String expected = exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "tipo válido";

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse.of(
            "El parámetro enviado no es válido",
            List.of("El parámetro '" + field + "' debe tener el tipo " + expected)));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
    String reason = extractDataIntegrityReason(exception);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiErrorResponse.of("No se pudo completar la operación por un conflicto de datos", List.of(reason)));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGenericException(Exception exception) {
    logger.error("Unexpected internal error", exception);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiErrorResponse.of("Ocurrió un error interno del servidor", List.of()));
  }

  private String extractDataIntegrityReason(DataIntegrityViolationException exception) {
    Throwable current = exception;
    while (current != null) {
      String message = current.getMessage();
      if (message != null && !message.isBlank()) {
        String normalized = message.trim();
        if (normalized.contains("duplicate key value")) {
          return "El valor ya existe";
        }
        if (normalized.contains("violates foreign key constraint")) {
          return "La entidad relacionada no existe";
        }
        if (normalized.contains("violates not-null constraint")) {
          return "Falta un campo obligatorio";
        }
        if (normalized.contains("violates check constraint")) {
          return "Un valor no cumple una regla de negocio";
        }
        return "Los datos enviados entran en conflicto con información existente";
      }
      current = current.getCause();
    }

    return "Database constraint violation";
  }

  private List<String> safeMessage(String message) {
    return message == null || message.isBlank() ? List.of() : List.of(message);
  }
}
