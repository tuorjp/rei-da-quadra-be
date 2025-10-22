package rei_da_quadra_be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class EventoRequestDTO {
  private String nome;
  private String local;
  private LocalDateTime dataHorario;

  public Evento toEvento() {
    Evento evento = new Evento();
    evento.setNome(this.nome);
    evento.setLocal(this.local);
    evento.setDataHorario(this.dataHorario);
    return evento;
  }
}


