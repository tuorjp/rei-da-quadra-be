package rei_da_quadra_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.Time;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeRepository extends JpaRepository<Time, Long> {
  Optional<Time> findByEventoAndTimeDeEsperaTrue(Evento evento);
  List<Time> findByEventoId(Long eventoId);
}
