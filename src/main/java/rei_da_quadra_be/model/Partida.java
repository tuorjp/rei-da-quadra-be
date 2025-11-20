package rei_da_quadra_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rei_da_quadra_be.enums.StatusPartida;

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

  @Column(name = "status", length = 20, nullable = false)
  private StatusPartida status = StatusPartida.AGUARDANDO_INICIO;

  @Column(name = "time_a_placar", nullable = false)
  private Integer timeAPlacar;

  @Column(name = "time_b_placar", nullable = false)
  private Integer timeBPlacar;

  @ManyToOne
  @JoinColumn(name = "evento_id", nullable = false)
  private Evento evento;

  @ManyToOne
  @JoinColumn(name = "time_a_id", nullable = false)
  private Time timeA;

  @ManyToOne
  @JoinColumn(name = "time_b_id", nullable = false)
  private Time timeB;

  //lista de desempenho individual dos jogadores nesta partida
  @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ParticipacaoDesempenho> participacoes = new ArrayList<>();

  //registros de alteração de pontos gerados por esta partida
  @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<HistoricoPontuacao> historicoPontuacao = new ArrayList<>();

  //transferências causadas por esta partida
  @OneToMany(mappedBy = "partidaGatilho", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<HistoricoTransferencia> historicoTransferencias = new ArrayList<>();
}