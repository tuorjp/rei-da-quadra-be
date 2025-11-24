package rei_da_quadra_be.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PartidaCreateDTO {
  @NotNull
  private Long timeAId;
  @NotNull
  private Long timeBId;
}