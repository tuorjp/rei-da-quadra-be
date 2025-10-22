package rei_da_quadra_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rei_da_quadra_be.dto.RegisterDTO;
import rei_da_quadra_be.enums.UserRole;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.repository.UserRepository;
import rei_da_quadra_be.service.exception.UserAlreadyExistsException;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public void createUser(RegisterDTO data) {
    if (this.userRepository.findByLogin(data.login) != null) {
      throw new UserAlreadyExistsException("Usuário já existente: " + data.login);
    }

    String encryptedPassword = passwordEncoder.encode(data.password);

    // Define role padrão como USER se não for informado
    UserRole userRole = data.getRole() != null ? data.getRole() : UserRole.USER;

    User newUser = new User(data.getLogin(), encryptedPassword, data.getNome(), userRole);

    userRepository.save(newUser);
  }
}
