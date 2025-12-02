package rei_da_quadra_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rei_da_quadra_be.model.Partida;

import java.util.List;

@Repository
public interface PartidaRepository extends JpaRepository<Partida, Long> {
    List<Partida> findByEventoId(Long eventoId);

    // Conta quantas partidas o jogador venceu
    @Query("SELECT COUNT(p) FROM Partida p " +
            "JOIN Inscricao i ON (i.timeAtual.id = p.timeA.id OR i.timeAtual.id = p.timeB.id) " +
            "WHERE i.jogador.id = :jogadorId " +
            "AND p.status = rei_da_quadra_be.enums.StatusPartida.JOGADA " +
            "AND ( " +
            "  (i.timeAtual.id = p.timeA.id AND p.timeAPlacar > p.timeBPlacar) OR " +
            "  (i.timeAtual.id = p.timeB.id AND p.timeBPlacar > p.timeAPlacar) " +
            ")")
    long countVitoriasDoJogador(@Param("jogadorId") Long jogadorId);

    // Conta quantas partidas o jogador jogou (ganhando ou perdendo)
    @Query("SELECT COUNT(p) FROM Partida p " +
            "JOIN Inscricao i ON (i.timeAtual.id = p.timeA.id OR i.timeAtual.id = p.timeB.id) " +
            "WHERE i.jogador.id = :jogadorId " +
            "AND p.status = rei_da_quadra_be.enums.StatusPartida.JOGADA")
    long countPartidasJogadas(@Param("jogadorId") Long jogadorId);
}