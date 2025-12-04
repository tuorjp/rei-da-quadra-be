package rei_da_quadra_be.dto;

import lombok.Data;

import java.util.List;

@Data
public class TimeResponseDTO {
  private Long id;
  private String nome;
  private String cor;
  private Boolean timeDeEspera;
  private String status;
  private List<JogadorDTO> jogadores;
}