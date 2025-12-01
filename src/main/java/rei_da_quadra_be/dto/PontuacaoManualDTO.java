package rei_da_quadra_be.dto;

import lombok.Data;
import rei_da_quadra_be.enums.TipoAcaoEmJogo;

@Data
public class PontuacaoManualDTO {
  private TipoAcaoEmJogo acao;
}