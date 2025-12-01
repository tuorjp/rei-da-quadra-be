package rei_da_quadra_be.dto;

import lombok.Data;
import rei_da_quadra_be.enums.StatusTime;

import java.util.List;

@Data
public class TimeComJogadoresDTO {
  private Long timeId;
  private String nome;
  private StatusTime status;
  private Boolean isTimeDeEspera;
  private List<JogadorResumoDTO> jogadores;
}