package rei_da_quadra_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rei_da_quadra_be.model.HistoricoPontuacao;

import java.util.List;

public interface HistoricoPontuacaoRepository extends JpaRepository<HistoricoPontuacao, Long> {

    List<HistoricoPontuacao> findByJogadorIdOrderByDataRegistroDesc(Long jogadorId);
}
