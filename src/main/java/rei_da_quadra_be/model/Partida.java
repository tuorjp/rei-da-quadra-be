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

    @Column(name = "data_partida", nullable = false)
    private LocalDateTime dataPartida = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StatusPartida status = StatusPartida.AGUARDANDO_INICIO;

    @Column(name = "time_a_placar", nullable = false)
    private Integer timeAPlacar = 0;

    @Column(name = "time_b_placar", nullable = false)
    private Integer timeBPlacar = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_a_id", nullable = false)
    private Time timeA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_b_id", nullable = false)
    private Time timeB;

    // Participações dos jogadores (LAZY para performance; carregado via JOIN FETCH no seeder)
    @OneToMany(
            mappedBy = "partida",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ParticipacaoDesempenho> participacoes = new ArrayList<>();

    // Histórico de pontuação gerado por ações desta partida
    @OneToMany(
            mappedBy = "partida",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<HistoricoPontuacao> historicoPontuacao = new ArrayList<>();

    // Histórico de transferências causadas por esta partida
    @OneToMany(
            mappedBy = "partidaGatilho",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<HistoricoTransferencia> historicoTransferencias = new ArrayList<>();
}
