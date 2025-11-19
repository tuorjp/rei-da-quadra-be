package rei_da_quadra_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inscricao", uniqueConstraints = {
  @UniqueConstraint(columnNames = {"evento_id", "jogador_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inscricao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "inscricao_id")
  private Long id;

  // Contador para garantir que todos joguem (Regra de rodízio)
  @Column(name = "partidas_jogadas", nullable = false)
  private Integer partidasJogadas = 0;

  // --- Relacionamentos ---

  @ManyToOne
  @JoinColumn(name = "evento_id", nullable = false)
  private Evento evento;

  // Conecta diretamente à sua classe User (que representa o Jogador)
  @ManyToOne
  @JoinColumn(name = "jogador_id", nullable = false)
  private User jogador;

  // O time atual do jogador. Pode ser NULL se ele ainda não foi alocado
  @ManyToOne
  @JoinColumn(name = "time_atual_id", nullable = true)
  private Time timeAtual;
}