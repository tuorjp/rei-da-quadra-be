package rei_da_quadra_be.dto;

import lombok.Getter;
import lombok.Setter;
import rei_da_quadra_be.enums.UserRole;

@Getter
@Setter
public class RegisterDTO {
  public String login;
  public String password;
  public UserRole role;
}