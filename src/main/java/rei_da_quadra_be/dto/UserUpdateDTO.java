package rei_da_quadra_be.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {
    private String nome;
    private String senha;
    private String confirmarSenha;
    private String fotoPerfil;
}