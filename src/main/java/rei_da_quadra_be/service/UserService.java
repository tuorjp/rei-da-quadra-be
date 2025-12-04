package rei_da_quadra_be.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rei_da_quadra_be.dto.UserUpdateDTO;
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
    user.setEnabled(false);
    user.setDataCriacao(LocalDateTime.now());
    userRepository.save(user);

    String token = UUID.randomUUID().toString();
    salvarOuAtualizarToken(user, token);

    emailService.enviarEmailConfirmacao(user, token);
  }

  public User atualizarUsuario(User userAtual, UserUpdateDTO dados) {
    // Atualiza nome
    if (dados.getNome() != null && !dados.getNome().isBlank()) {
      userAtual.setNome(dados.getNome());
    }

    // Atualiza senha se fornecida
    if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
      userAtual.setPassword(passwordEncoder.encode(dados.getSenha()));
    }

    // Se vier uma string, salva. Se vier null, mantém.
    // Se quiser permitir remover, o front deve mandar uma string vazia "" e aqui tratamos.
    if (dados.getFotoPerfil() != null) {
      userAtual.setFotoPerfil(dados.getFotoPerfil());
    }

    return userRepository.save(userAtual);
  }
    // SoftDelete
    public void deletarConta(User user) {
        // 1. Remove tokens de confirmação pendentes (limpeza)
        tokenRepository.findByUser(user).ifPresent(token -> tokenRepository.delete(token));

        // 2. Anonimizar dados pessoais
        user.setNome("Usuário Excluído");
        user.setFotoPerfil(null); // Remove a foto

        // 3. Liberar o E-mail para novo cadastro
        // Alteramos o email atual para algo único que não conflite, mas libere o original
        String emailOriginal = user.getEmail();
        String emailAnonimizado = System.currentTimeMillis() + "_deleted_" + emailOriginal;
        user.setEmail(emailAnonimizado);

        // 4. Inutilizar a senha (gera hash de UUID aleatório para impedir login)
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        // 5. Desativar a conta
        user.setEnabled(false);

        // 6. Salvar as alterações (Update ao invés de Delete)
        userRepository.save(user);
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