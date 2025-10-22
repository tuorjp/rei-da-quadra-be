package rei_da_quadra_be.dto;

import lombok.Getter;
import lombok.Setter;
import rei_da_quadra_be.enums.UserRole;

@Getter
@Setter
public class UserProfileDTO {
  private Long id;
  private String nome;
  private String email;
  private UserRole role;
}

