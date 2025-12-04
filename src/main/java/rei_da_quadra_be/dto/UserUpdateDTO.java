package rei_da_quadra_be.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {
    @Size(max = 30, message = "O nome deve ter no máximo 30 caracteres")
    private String nome;
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    private String senha;
    private String confirmarSenha;
    private String fotoPerfil;
}