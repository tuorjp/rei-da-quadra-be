package rei_da_quadra_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "participacao_desempenho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipacaoDesempenho {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "participacao_id")
  private Long id;

  @Column(name = "gols", nullable = false)
  private Integer gols = 0;

  @Column(name = "passes", nullable = false)
  private Integer passes = 0;

  @Column(name = "defesas", nullable = false)
  private Integer defesas = 0;

  // --- Relacionamentos ---

  @ManyToOne
  @JoinColumn(name = "partida_id", nullable = false)
  private Partida partida;

  @ManyToOne
  @JoinColumn(name = "jogador_id", nullable = false)
  private User jogador;

  // Armazena qual time o jogador defendia nesta partida específica
  @ManyToOne
  @JoinColumn(name = "time_id_na_partida", nullable = false)
  private Time timeNaPartida;
}