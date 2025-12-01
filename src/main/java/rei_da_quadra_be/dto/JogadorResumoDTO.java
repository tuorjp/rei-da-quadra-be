package rei_da_quadra_be.dto;

import lombok.Data;
import rei_da_quadra_be.enums.NivelHabilidade;

@Data
public class JogadorResumoDTO {
  private Long id;
  private String nome;
  private Integer pontosHabilidade;
  private NivelHabilidade nivelHabilidade;
  private Integer partidasJogadas; // Do ticket
}