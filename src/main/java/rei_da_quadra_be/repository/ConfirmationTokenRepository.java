package rei_da_quadra_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rei_da_quadra_be.model.ConfirmationToken;
import rei_da_quadra_be.model.User;
import java.util.Optional;

@Repository
public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {

    Optional<ConfirmationToken> findByToken(String token);

    // Permite buscar o token associado a um usuário específico
    Optional<ConfirmationToken> findByUser(User user);
}