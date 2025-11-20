package rei_da_quadra_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_transferencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoTransferencia {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "transferencia_id")
  private Long id;

  @Column(name = "data_transferencia")
  private LocalDateTime dataTransferencia = LocalDateTime.now();

  @ManyToOne
  @JoinColumn(name = "evento_id", nullable = false)
  private Evento evento;

  //partida que causou a transferência
  @ManyToOne
  @JoinColumn(name = "partida_id_gatilho", nullable = false)
  private Partida partidaGatilho;

  @ManyToOne
  @JoinColumn(name = "jogador_id", nullable = true)
  private User jogador;

  @ManyToOne
  @JoinColumn(name = "time_origem_id", nullable = true)
  private Time timeOrigem;

  @ManyToOne
  @JoinColumn(name = "time_destino_id", nullable = true)
  private Time timeDestino;
}