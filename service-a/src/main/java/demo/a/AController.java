package demo.a;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
public class AController {
  private final RestTemplate rest = new RestTemplate();

  @GetMapping("/call")
  public ResponseEntity<String> call(
      @RequestHeader(value="traceparent", required=false) String tp) {

    // A NO genera traceparent: si no viene, falla
    if (tp == null || tp.isBlank()) {
      return ResponseEntity.badRequest().body("A: Missing required header traceparent");
    }

    String url = System.getenv().getOrDefault("SERVICE_B_URL", "http://service-b:8080/receive");

    HttpHeaders headers = new HttpHeaders();
    headers.set("traceparent", tp.trim());

    ResponseEntity<String> resp =
        rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

    // A responde con el status + body de B
    return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
  }

  @GetMapping("/health")
  public String health(){ return "ok"; }
}
