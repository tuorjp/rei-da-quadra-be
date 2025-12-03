package rei_da_quadra_be.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rei_da_quadra_be.enums.StatusEvento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "evento_id")
  private Long id;

  @ManyToOne
  @JoinColumn(name = "usuario_id", nullable = false)
  @JsonBackReference
  private User usuario; //criador do evento

  @Column(columnDefinition = "TIMESTAMP", name = "data_evento")
  public LocalDateTime dataHorarioEvento;

  @Column(name = "nome", length = 150, nullable = false)
  private String nome;

  @Column(name = "data_criacao", updatable = false)
  private LocalDateTime dataCriacao = LocalDateTime.now();

  @Column(name = "local_evento", length = 150)
  private String localEvento;

  @Column(nullable = true)
  private Double latitude;

  @Column(nullable = true)
  private Double longitude;

  @Column(name = "jogadores_por_time")
  private Integer jogadoresPorTime; //quantidade de jogadores por time

  @Column(name = "total_partidas_definidas")
  private Integer totalPartidasDefinidas; //total de partidas no evento

  @Column(name = "status", length = 20, nullable = false)
  private StatusEvento status = StatusEvento.ATIVO;

  @Column(name = "cor_primaria", length = 7)
  private String corPrimaria;

  @Column(name = "cor_secundaria", length = 7)
  private String corSecundaria;

  @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Time> times = new ArrayList<>(); //times do evento

  /* inscrições: nem todo inscrito começa em um time, ele pode ser reserva*/
  @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Inscricao> inscricoes = new ArrayList<>();

  /*partidas realizadas e a realizar no evento*/
  @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Partida> partidas = new ArrayList<>();

  /*tabela de controle: onde um jogador estava e para onde foi*/
  @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<HistoricoTransferencia> historicoTransferencias = new ArrayList<>();
}