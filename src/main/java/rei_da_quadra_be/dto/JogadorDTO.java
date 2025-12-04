package rei_da_quadra_be.dto;

import lombok.Getter;
import lombok.Setter;
import rei_da_quadra_be.enums.NivelHabilidade;

@Getter
@Setter
public class JogadorDTO {
  Long id;
  Long eventoId;
  Long timeId;
  String nome;
  NivelHabilidade nivel;
  String foto;

  public JogadorDTO(Long id, Long eventoId, Long timeId, String nome, NivelHabilidade nivel, String foto) {
    this.id = id;
    this.eventoId = eventoId;
    this.timeId = timeId;
    this.nome = nome;
    this.nivel = nivel;
    this.foto = foto;
  }

  public JogadorDTO(Long id, Long eventoId, Long timeId) {
    this.id = id;
    this.eventoId = eventoId;
    this.timeId = timeId;
  }
}
