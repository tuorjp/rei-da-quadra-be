package rei_da_quadra_be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rei_da_quadra_be.model.Evento;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.STRING)
public class EventoRequestDTO {
  private String nome;
  private String local;
  private OffsetDateTime dataHorario;
  private Double latitude;
  private Double longitude;

  public Evento toEvento() {
    Evento evento = new Evento();
    evento.setNome(this.nome);
    evento.setLocalEvento(this.local);
    evento.setDataHorarioEvento(this.dataHorario);
    evento.setLatitude(this.latitude);
    evento.setLongitude(this.longitude);

    return evento;
  }
}


