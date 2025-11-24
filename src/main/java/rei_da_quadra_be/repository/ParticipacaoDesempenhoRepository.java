package rei_da_quadra_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rei_da_quadra_be.model.ParticipacaoDesempenho;

import java.util.Optional;

public interface ParticipacaoDesempenhoRepository extends JpaRepository<ParticipacaoDesempenho, Long> {
  Optional<ParticipacaoDesempenho> findByPartidaIdAndJogadorId(Long partidaId, Long jogadorId);
}
