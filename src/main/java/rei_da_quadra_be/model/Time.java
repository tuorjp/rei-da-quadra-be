package rei_da_quadra_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "time", uniqueConstraints = {
  @UniqueConstraint(columnNames = {"evento_id", "nome"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Time {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "time_id")
  private Long id;

  @Column(name = "nome", length = 100, nullable = false)
  private String nome;

  @Column(name = "cor", length = 7)
  private String cor;

  // Identifica o time especial para onde vão os jogadores "sobrando"
  @Column(name = "time_de_espera", nullable = false)
  private Boolean timeDeEspera = false;

  // Status do time: 'ativo', 'inoperante'
  @Column(name = "status", length = 20, nullable = false)
  private String status = "ativo";

  // --- Relacionamentos ---

  @ManyToOne
  @JoinColumn(name = "evento_id", nullable = false)
  private Evento evento;

  @OneToMany(mappedBy = "timeAtual")
  private List<Inscricao> inscricoes = new ArrayList<>();

  // Partidas onde este time jogou como Time A
  @OneToMany(mappedBy = "timeA")
  private List<Partida> partidasComoTimeA = new ArrayList<>();

  // Partidas onde este time jogou como Time B
  @OneToMany(mappedBy = "timeB")
  private List<Partida> partidasComoTimeB = new ArrayList<>();

  // Histórico de desempenho vinculado a este time
  @OneToMany(mappedBy = "timeNaPartida")
  private List<ParticipacaoDesempenho> participacoes = new ArrayList<>();
}