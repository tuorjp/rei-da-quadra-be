package rei_da_quadra_be.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import rei_da_quadra_be.enums.TipoAcaoEmJogo;

@Data
public class AcaoJogoDTO {
  @NotNull
  private Long jogadorId;
  @NotNull
  private TipoAcaoEmJogo tipoAcao;
}