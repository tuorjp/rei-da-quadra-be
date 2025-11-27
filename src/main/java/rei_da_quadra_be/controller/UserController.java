package rei_da_quadra_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.service.AuthorizationService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/usuario")
public class UserController {
  private final AuthorizationService authorizationService;

  @GetMapping("/find-by-email")
  public ResponseEntity<User> findByEmail(@RequestBody String email) {
    User user = (User) authorizationService.loadUserByUsername(email);

    return ResponseEntity.ok().body(user);
  }
}
