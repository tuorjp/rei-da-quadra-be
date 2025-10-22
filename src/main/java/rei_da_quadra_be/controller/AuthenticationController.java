package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rei_da_quadra_be.dto.AuthenticationDTO;
import rei_da_quadra_be.dto.LoginResponseDTO;
import rei_da_quadra_be.dto.RegisterDTO;
import rei_da_quadra_be.dto.UserProfileDTO;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.security.TokenService;
import rei_da_quadra_be.service.UserService;
import rei_da_quadra_be.service.exception.UserAlreadyExistsException;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthenticationController {
  private final AuthenticationManager authenticationManager;
  private final TokenService tokenService;
  private final UserService userService;

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
  public ResponseEntity<String> register(@RequestBody @Valid RegisterDTO data) {
    try {
      userService.createUser(data);
    } catch (UserAlreadyExistsException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    return ResponseEntity.ok().build();
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
    profile.setEmail(user.getLogin());
    profile.setRole(user.getRole());
    
    return ResponseEntity.ok(profile);
  }
}

