package rei_da_quadra_be.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthenticationDTO {
  public String email;
  public String password;
}
