package rei_da_quadra_be.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rei_da_quadra_be.enums.StatusEvento;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.User;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByUsuario(User usuario);

    @Query("SELECT e FROM Evento e WHERE " +
            "e.dataHorario BETWEEN :dataInicio AND :dataFim " +
            "AND e.status = :status " +
            "AND (6371 * acos(cos(radians(:lat)) * cos(radians(e.latitude)) * cos(radians(e.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(e.latitude)))) <= :raio " +
            "ORDER BY e.dataHorario ASC, " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(e.latitude)) * cos(radians(e.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(e.latitude)))) ASC")
    List<Evento> buscarEventosProximos(
            @Param("lat") Double latitudeUsuario,
            @Param("lon") Double longitudeUsuario,
            @Param("raio") Double raioKm,
            @Param("status") StatusEvento status,
            @Param("dataInicio") OffsetDateTime dataInicio,
            @Param("dataFim") OffsetDateTime dataFim,
            Pageable pageable);
}