package rei_da_quadra_be.dto;

import lombok.Data;

@Data
public class TimeResponseDTO {
  private Long id;
  private String nome;
  private String cor;
  private Boolean timeDeEspera;
  private String status;
}