package rei_da_quadra_be.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import rei_da_quadra_be.model.*;
import rei_da_quadra_be.repository.*;
import rei_da_quadra_be.service.AdmTimesService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {

  private final UserRepository userRepository;
  private final EventoRepository eventoRepository;
  private final InscricaoRepository inscricaoRepository;
  private final TimeRepository timeRepository;
  private final PartidaRepository partidaRepository;
  private final AdmTimesService admTimesService;
  private final PasswordEncoder passwordEncoder;

  @Bean
  public CommandLineRunner seedDatabase() {
    return args -> {
      System.out.println("🌱 Iniciando Seeding do Banco de Dados...");

      // 1. Limpeza (Opcional: Cuidado ao usar em bancos persistentes)
      limparBanco();

      if (userRepository.count() > 0) {
        System.out.println("🛑 Banco já populado. Pulando seeding.");
        return;
      }

      // 2. Criar Usuários (15 Jogadores: 3 Craques, 8 Medianos, 4 Pernas)
      List<User> users = criarUsuarios();
      System.out.println("✅ 15 Usuários criados.");

      // 3. Criar Evento
      User organizador = users.get(0);

      // 2. Criar Evento (Passando o organizador)
      Evento evento = criarEvento(organizador);
      System.out.println("✅ Evento criado: " + evento.getNome());

      // 4. Inscrever Usuários no Evento
      users.forEach(user -> inscreverUsuario(user, evento));
      System.out.println("✅ Usuários inscritos.");

      // 5. Montar Times (Usa seu Service)
      System.out.println("⚙️ Executando algoritmo de distribuição de times...");
      admTimesService.montarTimesInicial(evento.getId());
      System.out.println("✅ Times distribuídos e jogadores alocados.");

      // 6. Criar uma Partida Inicial (Time 1 vs Time 2)
      criarPartidaInicial(evento);
      System.out.println("✅ Partida inicial criada e pronta para jogar.");

      System.out.println("🌱 Seeding concluído com sucesso!");
    };
  }

  private List<User> criarUsuarios() {
    List<User> lista = new ArrayList<>();

    // Craques (Nível 3, Elo alto)
    lista.add(criarUser("Neymar Jr", "neymar@teste.com", 1500, 3));
    lista.add(criarUser("Messi", "messi@teste.com", 1480, 3));
    lista.add(criarUser("CR7", "cr7@teste.com", 1490, 3));

    // Medianos (Nível 2, Elo médio)
    for (int i = 1; i <= 8; i++) {
      lista.add(criarUser("Jogador Mediano " + i, "mediano" + i + "@teste.com", 1000 + (i * 10), 2));
    }

    // Pernas de Pau (Nível 1, Elo baixo)
    for (int i = 1; i <= 4; i++) {
      lista.add(criarUser("Perna de Pau " + i, "perna" + i + "@teste.com", 800 + (i * 5), 1));
    }

    return lista; // Retorna a lista já salva
  }

  private User criarUser(String nome, String email, Integer elo, Integer nivel) {
    User u = new User();
    u.setNome(nome);
    u.setEmail(email);
    // Se não tiver PasswordEncoder configurado, use apenas "123456"
    u.setPassword(passwordEncoder != null ? passwordEncoder.encode("123456") : "123456");
    u.setRole("USER");
    u.setEnabled(true);
    u.setPontosHabilidade(elo);
    u.setNivelHabilidade(nivel);
    return userRepository.save(u);
  }

  private Evento criarEvento(User organizador) {
    Evento e = new Evento();
    e.setUsuario(organizador);
    e.setNome("Futebol de Quinta - Seeder");
    e.setLocalEvento("Quadra Central");
    e.setDataHorario(LocalDateTime.now().plusDays(2)); // Data futura (campo do seu model antigo)
    e.setDataEvento(LocalDateTime.now().plusDays(2)); // Campo do DDL novo
    e.setJogadoresPorTime(5); // Times de 5
    e.setTotalPartidasDefinidas(10);
    e.setCorPrimaria("#0000FF");
    e.setCorSecundaria("#FFFFFF");
    e.setStatus("ativo");
    return eventoRepository.save(e);
  }

  private void inscreverUsuario(User user, Evento evento) {
    Inscricao i = new Inscricao();
    i.setEvento(evento);
    i.setJogador(user);
    i.setPartidasJogadas(0);
    inscricaoRepository.save(i);
  }

  private void criarPartidaInicial(Evento evento) {
    // Busca os times que o Service criou
    List<Time> times = timeRepository.findByEventoId(evento.getId());

    // Filtra apenas os ativos (ignora o de espera)
    List<Time> timesJogaveis = times.stream()
      .filter(t -> !t.getTimeDeEspera())
      .toList();

    if (timesJogaveis.size() >= 2) {
      Partida p = new Partida();
      p.setEvento(evento);
      p.setTimeA(timesJogaveis.get(0));
      p.setTimeB(timesJogaveis.get(1));
      p.setTimeAPlacar(0);
      p.setTimeBPlacar(0);
      p.setStatus("jogada");
      partidaRepository.save(p);
    }
  }

  private void limparBanco() {
    partidaRepository.deleteAll();
    inscricaoRepository.deleteAll();
    timeRepository.deleteAll();
    eventoRepository.deleteAll();
    userRepository.deleteAll();
  }
}
