package rei_da_quadra_be.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
  private User usuario;

  public String local;
  @Column(columnDefinition = "TIMESTAMP")
  public LocalDateTime dataHorario;

  @Column(name = "nome", length = 150, nullable = false)
  private String nome;

  @Column(name = "data_criacao", updatable = false)
  private LocalDateTime dataCriacao = LocalDateTime.now();

  @Column(name = "data_evento")
  private LocalDateTime dataEvento;

  @Column(name = "local_evento", length = 150)
  private String localEvento;

  // Configurações definidas pelo usuário (Novas Regras)
  @Column(name = "jogadores_por_time")
  private Integer jogadoresPorTime;

  @Column(name = "total_partidas_definidas")
  private Integer totalPartidasDefinidas;

  // Estado do evento: 'ativo', 'concluido', 'cancelado'
  @Column(name = "status", length = 20, nullable = false)
  private String status = "ativo";

  // Campos de personalização do evento
  @Column(name = "cor_primaria", length = 7)
  private String corPrimaria;

  @Column(name = "cor_secundaria", length = 7)
  private String corSecundaria;

  // --- Relacionamentos (Listas inversas) ---

  @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Time> times = new ArrayList<>();

  @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Inscricao> inscricoes = new ArrayList<>();

  @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Partida> partidas = new ArrayList<>();

  @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<HistoricoTransferencia> historicoTransferencias = new ArrayList<>();
}