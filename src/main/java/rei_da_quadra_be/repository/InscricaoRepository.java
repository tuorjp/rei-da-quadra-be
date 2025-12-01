package rei_da_quadra_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.Inscricao;
import rei_da_quadra_be.model.Time;

import java.util.List;

@Repository
public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {
  List<Inscricao> findByEventoId(Long eventoId);
  List<Inscricao> findByTimeAtualAndEvento(Time timeAtual, Evento evento);
  long countByTimeAtual(Time timeAtual);
}
