package rei_da_quadra_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "partida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Partida {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "partida_id")
  private Long id;

  @Column(name = "data_partida")
  private LocalDateTime dataPartida = LocalDateTime.now();

  // Status da partida: 'jogada', 'cancelada'
  @Column(name = "status", length = 20, nullable = false)
  private String status = "jogada";

  @Column(name = "time_a_placar", nullable = false)
  private Integer timeAPlacar;

  @Column(name = "time_b_placar", nullable = false)
  private Integer timeBPlacar;

  // --- Relacionamentos ---

  @ManyToOne
  @JoinColumn(name = "evento_id", nullable = false)
  private Evento evento;

  @ManyToOne
  @JoinColumn(name = "time_a_id", nullable = false)
  private Time timeA;

  @ManyToOne
  @JoinColumn(name = "time_b_id", nullable = false)
  private Time timeB;

  // Lista de desempenho individual dos jogadores nesta partida
  @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ParticipacaoDesempenho> participacoes = new ArrayList<>();

  // Registros de alteração de pontos (Elo) gerados por esta partida
  @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<HistoricoPontuacao> historicoPontuacao = new ArrayList<>();

  // Transferências causadas por esta partida (ex: time perdedor saindo)
  @OneToMany(mappedBy = "partidaGatilho", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<HistoricoTransferencia> historicoTransferencias = new ArrayList<>();
}