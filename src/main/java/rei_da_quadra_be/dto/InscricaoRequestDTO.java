package rei_da_quadra_be.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscricaoRequestDTO {
    
    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    private String jogadorEmail;
    
    private Long jogadorId; // Opcional
}
