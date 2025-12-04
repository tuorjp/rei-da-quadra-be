package rei_da_quadra_be.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PartidaResponseDTO {
  private Long id;
  private LocalDateTime dataPartida;
  private String status;
  private String timeANome;
  private Integer timeAPlacar;
  private Long timeAId;
  private String timeBNome;
  private Integer timeBPlacar;
  private Long timeBId;
  private Long eventoId;
}