package rei_da_quadra_be.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rei_da_quadra_be.model.Evento;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoResponseDTO {
  private Long id;
  private String nome;
  private String local;
  private LocalDateTime dataHorario;
  private Long usuarioId;
  private String usuarioLogin;

  public static EventoResponseDTO fromEvento(Evento evento) {
    EventoResponseDTO dto = new EventoResponseDTO();
    dto.setId(evento.getId());
    dto.setNome(evento.getNome());
    dto.setLocal(evento.getLocalEvento());
    dto.setDataHorario(evento.getDataHorarioEvento());
    dto.setUsuarioId(evento.getUsuario().getId());
    dto.setUsuarioLogin(evento.getUsuario().getEmail());
    return dto;
  }
}


