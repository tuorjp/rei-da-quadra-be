package rei_da_quadra_be.dto;

import lombok.Getter;
import lombok.Setter;
import rei_da_quadra_be.enums.UserRole;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserProfileDTO {
  private Long id;
  private String nome;
  private String email;
  private UserRole role;
  private LocalDateTime dataCriacao;
}