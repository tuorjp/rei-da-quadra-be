package rei_da_quadra_be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rei_da_quadra_be.model.Evento;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.STRING)
public class EventoResponseDTO {
    private Long id;
    private String nome;
    private String local;
    private Double latitude;
    private Double longitude;
    private OffsetDateTime dataHorario;
    private Long usuarioId;
    private String usuarioLogin;
    private String usuarioNome;
    private Boolean isOrganizer = false;
    private String status;

    public static EventoResponseDTO fromEvento(Evento evento) {
        EventoResponseDTO dto = new EventoResponseDTO();
        dto.setId(evento.getId());
        dto.setNome(evento.getNome());
        dto.setLocal(evento.getLocalEvento());
        dto.setDataHorario(evento.getDataHorarioEvento());

        if (evento.getUsuario() != null) {
            dto.setUsuarioId(evento.getUsuario().getId());
            dto.setUsuarioLogin(evento.getUsuario().getEmail());
            dto.setUsuarioNome(evento.getUsuario().getNome());
        }

        dto.setLatitude(evento.getLatitude());
        dto.setLongitude(evento.getLongitude());

        if (evento.getStatus() != null) {
            dto.setStatus(evento.getStatus().name());
        }

        return dto;
    }
}