package mz.com.sgp.exception.hadler;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import mz.com.sgp.exception.ResourceNotFoundException;

@ControllerAdvice
@RestController
public class CustomEntityResponseHandler extends ResponseEntityExceptionHandler  {

	@ExceptionHandler(Exception.class)
    public final ResponseEntity<ExceptionResponse> handleAllExceptions(Exception ex, WebRequest request) {
        ExceptionResponse response = new ExceptionResponse(
                new Date(),
                "Erro interno ao processar o pedido",
                request.getDescription(false));
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
	
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ExceptionResponse> authentication(Exception ex, WebRequest request) {
        return response("Credenciais inválidas", HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> forbidden(Exception ex, WebRequest request) {
        return response("Acesso negado", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(mz.com.sgp.exception.InvalidQuantityException.class)
    public ResponseEntity<ExceptionResponse> invalidQuantity(mz.com.sgp.exception.InvalidQuantityException ex, WebRequest request) {
        return response(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> invalid(Exception ex, WebRequest request) {
        return response("Dados inválidos", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionResponse> conflict(Exception ex, WebRequest request) {
        return response("Operação em conflito com os dados existentes", HttpStatus.CONFLICT, request);
    }

    private ResponseEntity<ExceptionResponse> response(String message, HttpStatus status, WebRequest request) {
        return new ResponseEntity<>(new ExceptionResponse(new Date(), message,
                request.getDescription(false)), status);
    }

	@ExceptionHandler(ResourceNotFoundException.class)
    public final ResponseEntity<ExceptionResponse> handleNotFoundExceptions(Exception ex, WebRequest request) {
        ExceptionResponse response = new ExceptionResponse(
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
