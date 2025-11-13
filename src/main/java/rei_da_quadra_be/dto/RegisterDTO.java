package rei_da_quadra_be.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import rei_da_quadra_be.enums.UserRole;

@Getter
@Setter
public class RegisterDTO {
  @NotBlank(message = "Login é obrigatório")
  public String email;

  @NotBlank(message = "Senha é obrigatória")
  public String password;

  @NotBlank(message = "Nome é obrigatório")
  public String nome;

  public UserRole role; // Opcional - será USER por padrão
}