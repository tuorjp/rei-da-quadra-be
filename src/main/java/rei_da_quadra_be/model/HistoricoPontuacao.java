package rei_da_quadra_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rei_da_quadra_be.enums.TipoAcaoEmJogo;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_pontuacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoPontuacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historico_id")
    private Long id;

    @Column(name = "pontos_antes", nullable = false)
    private Integer pontosAntes;

    @Column(name = "pontos_depois", nullable = false)
    private Integer pontosDepois;

    @Column(name = "variacao", nullable = false)
    private Integer variacao;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", length = 40, nullable = false)
    private TipoAcaoEmJogo acao;

    @ManyToOne
    @JoinColumn(name = "jogador_id", nullable = false)
    private User jogador;

    // Partida que gerou a alteração de pontos
    @ManyToOne
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;
}
