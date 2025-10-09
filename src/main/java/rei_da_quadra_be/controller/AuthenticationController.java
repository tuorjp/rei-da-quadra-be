package rei_da_quadra_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rei_da_quadra_be.dto.AuthenticationDTO;
import rei_da_quadra_be.dto.LoginResponseDTO;
import rei_da_quadra_be.dto.RegisterDTO;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.repository.UserRepository;
import rei_da_quadra_be.security.TokenService;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthenticationController {
  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final PasswordEncoder passwordEncoder;

  @PostMapping("/login")
  public ResponseEntity login(@RequestBody AuthenticationDTO data) {
    var usernamePassword = new UsernamePasswordAuthenticationToken(data.login, data.password);

    var auth = this.authenticationManager.authenticate(usernamePassword);

    var token = tokenService.generateToken((User) auth.getPrincipal());

    LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
    loginResponseDTO.setToken(token);

    return ResponseEntity.ok(loginResponseDTO);
  }

  @PostMapping("register")
  public ResponseEntity register(@RequestBody RegisterDTO data) {
    if(this.userRepository.findByLogin(data.login) != null) {
      return ResponseEntity.badRequest().build();
    }

    String encryptedPassword = passwordEncoder.encode(data.password);
    User newUser = new User(data.getLogin(), encryptedPassword, data.getRole());

    userRepository.save(newUser);
    return ResponseEntity.ok().build();
  }
}

