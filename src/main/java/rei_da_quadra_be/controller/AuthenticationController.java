package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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
import rei_da_quadra_be.enums.UserRole;
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
  @Operation(summary = "Autentica um usuário e retorna um token JWT")
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Login bem-sucedido",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginResponseDTO.class)
          )
        }
      ),
      @ApiResponse(
        responseCode = "403",
        description = "Credenciais inválidas",
        content = @Content // Resposta de erro sem corpo
      )
    }
  )
  public ResponseEntity<LoginResponseDTO> login(@RequestBody AuthenticationDTO data) {
    var usernamePassword = new UsernamePasswordAuthenticationToken(data.login, data.password);

    var auth = this.authenticationManager.authenticate(usernamePassword);

    var token = tokenService.generateToken((User) auth.getPrincipal());

    LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
    loginResponseDTO.setToken(token);

    return ResponseEntity.ok(loginResponseDTO);
  }

  @PostMapping("register")
  public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data) {
    if (this.userRepository.findByLogin(data.login) != null) {
      return ResponseEntity.badRequest().build();
    }

    String encryptedPassword = passwordEncoder.encode(data.password);

    // Define role padrão como USER se não for informado
    UserRole userRole = data.getRole() != null ? data.getRole() : UserRole.USER;

    User newUser = new User(data.getLogin(), encryptedPassword, data.getNome(), userRole);

    userRepository.save(newUser);
    return ResponseEntity.ok().build();
  }
}

