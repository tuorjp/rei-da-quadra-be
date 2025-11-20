package rei_da_quadra_be.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import rei_da_quadra_be.enums.NivelHabilidade;
import rei_da_quadra_be.enums.StatusPartida;
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
      System.out.println("Iniciando Seeding do Banco de Dados...");

      limparBanco();

      List<User> users = criarUsuarios();
      System.out.println("15 Usuários criados.");

      User organizador = users.get(0);

      Evento evento = criarEvento(organizador);
      System.out.println("Evento criado: " + evento.getNome());

      users.forEach(user -> inscreverUsuario(user, evento));
      System.out.println("Usuários inscritos.");

      //monta Times usando service
      System.out.println("Executando algoritmo de distribuição de times...");
      admTimesService.montarTimesInicial(evento.getId());
      System.out.println("Times distribuídos e jogadores alocados.");

      //partida Inicial (Time 1 vs Time 2)
      criarPartidaInicial(evento);
      System.out.println("Partida inicial criada e pronta para jogar.");

      System.out.println("Seeding concluído com sucesso!");
    };
  }

  private List<User> criarUsuarios() {
    List<User> lista = new ArrayList<>();

    //craques
    lista.add(criarUser("Craque1", "craque1@teste.com", 1500, NivelHabilidade.CRAQUE));
    lista.add(criarUser("Craque2", "craque2@teste.com", 1480, NivelHabilidade.CRAQUE));
    lista.add(criarUser("Craque3", "craque3@teste.com", 1490, NivelHabilidade.CRAQUE));

    //medianos
    for (int i = 1; i <= 8; i++) {
      lista.add(criarUser("Jogador Mediano " + i, "mediano" + i + "@teste.com", 1000 + (i * 10), NivelHabilidade.MEDIANO));
    }

    // Pernas de Pau (Nível 1, Elo baixo)
    for (int i = 1; i <= 4; i++) {
      lista.add(criarUser("Perna de Pau " + i, "perna" + i + "@teste.com", 800 + (i * 5), NivelHabilidade.PERNA_DE_PAU));
    }

    return lista; // Retorna a lista já salva
  }

  private User criarUser(String nome, String email, Integer elo, NivelHabilidade nivel) {
    User u = new User();
    u.setNome(nome);
    u.setEmail(email);
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
    e.setDataHorarioEvento(LocalDateTime.now().plusDays(2));
    e.setJogadoresPorTime(5); //times de 5 jogadores
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
    //busca os times que o Service criou
    List<Time> times = timeRepository.findByEventoId(evento.getId());

    //filtra apenas os ativos, ignora o de espera
    List<Time> timesJogaveis = times
      .stream()
      .filter(t -> !t.getTimeDeEspera())
      .toList();

    if (timesJogaveis.size() >= 2) {
      Partida p = new Partida();
      p.setEvento(evento);
      p.setTimeA(timesJogaveis.get(0));
      p.setTimeB(timesJogaveis.get(1));
      p.setTimeAPlacar(0);
      p.setTimeBPlacar(0);
      p.setStatus(StatusPartida.JOGADA);
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
