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
import org.springframework.security.core.userdetails.UserDetails;

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
  public void solicitarRecuperacaoSenha(String email) throws MessagingException, UnsupportedEncodingException {
    UserDetails userDetails = userRepository.findByEmail(email);

    if (userDetails != null && userDetails instanceof User) {
      User user = (User) userDetails;

      String token = UUID.randomUUID().toString();

      // Agora chamamos o metodo que trata a duplicação
      salvarOuAtualizarToken(user, token);

      emailService.enviarEmailRecuperacao(user, token);
    }
  }

  private void salvarOuAtualizarToken(User user, String tokenStr) {
    // Tenta buscar um token existente para esse usuário
    ConfirmationToken tokenEntity = tokenRepository.findByUser(user)
            .orElse(new ConfirmationToken()); // Se não existir, cria um novo objeto

    // Atualiza os dados (seja novo ou existente)
    tokenEntity.setToken(tokenStr);
    tokenEntity.setUser(user);
    tokenEntity.setCreatedAt(LocalDateTime.now());
    tokenEntity.setExpiresAt(LocalDateTime.now().plusHours(24));

    // O JPA vai saber se é UPDATE (se já tiver ID) ou INSERT (se for novo)
    tokenRepository.save(tokenEntity);
  }

  public void redefinirSenha(String token, String newPassword) {
    // Busca o token no banco
    ConfirmationToken confirmationToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Token inválido ou não encontrado."));

    // Verifica se expirou
    if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new RuntimeException("Este link expirou. Solicite uma nova redefinição.");
    }

    // Atualiza a senha do usuário
    User user = confirmationToken.getUser();
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    // Deleta o token ou marca como usado para não ser usado de novo
    // Aqui vamos deletar para limpar o banco e garantir uso único
    tokenRepository.delete(confirmationToken);
  }
}