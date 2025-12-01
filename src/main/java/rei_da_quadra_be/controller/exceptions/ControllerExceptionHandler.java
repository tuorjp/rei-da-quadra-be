package rei_da_quadra_be.controller.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rei_da_quadra_be.service.exception.EventoNaoEncontradoException;
import rei_da_quadra_be.service.exception.NumeroInsuficienteInscritosException;
import rei_da_quadra_be.service.exception.TimeDeEsperaNaoConfiguradoException;
import rei_da_quadra_be.service.exception.UserAlreadyExistsException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ControllerExceptionHandler {

  @ExceptionHandler(EventoNaoEncontradoException.class)
  public ResponseEntity<Map<String, Object>> handleEventoNaoEncontradoException(EventoNaoEncontradoException e) {
    var code = HttpStatus.NOT_FOUND.value();
    var message = e.getMessage();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(montarBody(code, message));
  }

  @ExceptionHandler(NumeroInsuficienteInscritosException.class)
  public ResponseEntity<Map<String, Object>> handleNumeroInsuficienteInscritosException(NumeroInsuficienteInscritosException e) {
    var code = HttpStatus.BAD_REQUEST.value();
    var message = e.getMessage();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(montarBody(code, message));
  }

  @ExceptionHandler(TimeDeEsperaNaoConfiguradoException.class)
  public ResponseEntity<Map<String, Object>> handleTimeDeEsperaNaoConfiguradoException(TimeDeEsperaNaoConfiguradoException e) {
    var code = HttpStatus.NOT_FOUND.value();
    var message = e.getMessage();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(montarBody(code, message));
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
    var code = HttpStatus.CONFLICT.value();
    var message = e.getMessage();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(montarBody(code, message));
  }

  private Map<String, Object> montarBody(Object code, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", true);
    body.put("status", code);
    body.put("message", message);

    return body;
  }
}
