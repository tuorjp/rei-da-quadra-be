package rei_da_quadra_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import rei_da_quadra_be.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    UserDetails findByEmail(String email);
}
