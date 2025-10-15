package rei_da_quadra_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.User;

import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {
  List<Evento> findByUsuario(User usuario);
}
