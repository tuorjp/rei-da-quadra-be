package rei_da_quadra_be.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.io.UnsupportedEncodingException;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.model.ConfirmationToken;
import rei_da_quadra_be.repository.UserRepository;
import rei_da_quadra_be.repository.ConfirmationTokenRepository;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ConfirmationTokenRepository tokenRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private EmailService emailService;

  public void registrarUsuario(User user) throws MessagingException, UnsupportedEncodingException {
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setEnabled(false); // usuário começa inativo
    userRepository.save(user);

    // Gera token de confirmação
    String token = UUID.randomUUID().toString();

    ConfirmationToken confirmationToken = new ConfirmationToken();
    confirmationToken.setToken(token);
    confirmationToken.setUser(user);
    confirmationToken.setCreatedAt(LocalDateTime.now());
    confirmationToken.setExpiresAt(LocalDateTime.now().plusHours(24));

    tokenRepository.save(confirmationToken);

    // Envia email de confirmação
    emailService.enviarEmailConfirmacao(user, token);
  }
}