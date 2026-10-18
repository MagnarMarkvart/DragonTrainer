package ee.dragon.dragontrainer.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(RestClientResponseException.class)
	public ResponseEntity<Map<String, String>> upstream(RestClientResponseException exception) {
		int status = exception.getStatusCode().value();
		if (status == 404 || status == 410) {
			return ResponseEntity.status(HttpStatus.GONE)
					.body(Map.of("message", "This game is no longer available"));
		}
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(Map.of("message", "The game server did not answer"));
	}
}
