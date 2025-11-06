package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rei_da_quadra_be.dto.AuthenticationDTO;
import rei_da_quadra_be.dto.LoginResponseDTO;
import rei_da_quadra_be.dto.UserProfileDTO;
import rei_da_quadra_be.enums.UserRole;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.model.ConfirmationToken;
import rei_da_quadra_be.repository.UserRepository;
import rei_da_quadra_be.repository.ConfirmationTokenRepository;
import rei_da_quadra_be.security.TokenService;
import rei_da_quadra_be.service.UserService;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthenticationController {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ConfirmationTokenRepository tokenRepository;

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private TokenService tokenService;

  // Cadastro de novo usuário
  @PostMapping("/register")
  public ResponseEntity<String> registrar(@RequestBody User user) {
    try {
      userService.registrarUsuario(user);
      return ResponseEntity.ok("Cadastro realizado com sucesso! Verifique seu email para confirmar.");
    } catch (MessagingException e) {
      return ResponseEntity.internalServerError().body("Erro ao enviar email: " + e.getMessage());
    }
  }

  // Confirmação de email
  @GetMapping("/confirm")
  public ResponseEntity<String> confirmarEmail(@RequestParam("token") String token) {
    ConfirmationToken confToken = tokenRepository.findByToken(token)
            .orElse(null);

    if (confToken == null) {
      return ResponseEntity.badRequest().body("Token inválido.");
    }

    if (confToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      return ResponseEntity.badRequest().body("Token expirado. Faça um novo cadastro.");
    }

    User user = confToken.getUser();
    user.setEnabled(true);
    userRepository.save(user);

    return ResponseEntity.ok("Email confirmado com sucesso! Você já pode fazer login.");
  }

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
    var usernamePassword = new UsernamePasswordAuthenticationToken(data.email, data.password);

    var auth = this.authenticationManager.authenticate(usernamePassword);

    var token = tokenService.generateToken((User) auth.getPrincipal());

    LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
    loginResponseDTO.setToken(token);

    return ResponseEntity.ok(loginResponseDTO);
  }

  @GetMapping("/profile")
  @Operation(summary = "Retorna os dados do perfil do usuário autenticado")
  @ApiResponses(
          value = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "Perfil retornado com sucesso",
                          content = {
                                  @Content(
                                          mediaType = "application/json",
                                          schema = @Schema(implementation = UserProfileDTO.class)
                                  )
                          }
                  ),
                  @ApiResponse(
                          responseCode = "401",
                          description = "Não autenticado",
                          content = @Content
                  )
          }
  )
  public ResponseEntity<UserProfileDTO> getProfile(Authentication authentication) {
    User user = (User) authentication.getPrincipal();

    UserProfileDTO profile = new UserProfileDTO();
    profile.setId(user.getId());
    profile.setNome(user.getNome());
    profile.setEmail(user.getEmail());
    profile.setRole(UserRole.valueOf(user.getRole()));

    return ResponseEntity.ok(profile);
  }
}
