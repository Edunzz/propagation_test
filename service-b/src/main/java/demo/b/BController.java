package demo.b;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class BController {

  @GetMapping("/receive")
  public ResponseEntity<String> receive(
      @RequestHeader(value="traceparent", required=false) String tp) {

    if (tp == null || tp.isBlank()) {
      return ResponseEntity.badRequest().body("B: Missing required header traceparent");
    }
    return ResponseEntity.ok("B: OK - received traceparent=" + tp);
  }

  @GetMapping("/health")
  public String health(){ return "ok"; }
}
