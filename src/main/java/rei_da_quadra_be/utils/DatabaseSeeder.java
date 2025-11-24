package rei_da_quadra_be.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import rei_da_quadra_be.enums.NivelHabilidade;
import rei_da_quadra_be.enums.StatusEvento;
import rei_da_quadra_be.enums.StatusPartida;
import rei_da_quadra_be.enums.TipoAcaoEmJogo;
import rei_da_quadra_be.model.*;
import rei_da_quadra_be.repository.*;
import rei_da_quadra_be.service.AdmTimesService;

import java.time.LocalDateTime;
import java.util.*;

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
      System.out.println("=== Iniciando Seeding Completo do Banco de Dados ===");

      limparBanco();
      User userAdm = criarUser("ADMIN", "admin@gmail.com", 5000, NivelHabilidade.CRAQUE);

      //cria usuários para torneio
      List<User> users = criarMuitosUsuarios();
      System.out.println(users.size() + " usuários criados.");

      //cria evento
      Evento evento = criarEvento(userAdm);
      System.out.println("Evento criado: " + evento.getNome());

      //inscreve todos os usuários
      users.forEach(user -> inscreverUsuario(user, evento));
      System.out.println("Usuários inscritos.");

      //montar Times (Service)
      System.out.println("Executando algoritmo de distribuição de times...");
      admTimesService.montarTimesInicial(evento.getId());

      //simula torneio (partidas, gols e rodízio)
      System.out.println("=== Iniciando Simulação de Torneio (10 Partidas) ===");
      simularTorneioCompleto(evento);

      System.out.println("=== Seeding Concluído com Sucesso! ===");
    };
  }

  @Transactional
  public void simularTorneioCompleto(Evento evento) {
    List<Time> times = timeRepository.findByEventoId(evento.getId());

    //filtra times jogáveis (exclui o time de espera "reserva")
    List<Time> timesJogaveis = new ArrayList<>(
      times.stream()
      .filter(t -> !t.getTimeDeEspera())
      .toList()
    );

    if (timesJogaveis.size() < 2) {
      System.out.println("Não há times suficientes para simular partidas.");
      return;
    }

    //fila de desafiantes
    //o primeiro da lista é o rei da quadra atual, o segundo é o desafiante.
    //os outros esperam na fila.
    Queue<Time> filaDeTimes = new LinkedList<>(timesJogaveis);

    Time timeRei = filaDeTimes.poll(); //começa como rei (Time 1)

    Random random = new Random();
    int totalPartidasASimular = 10;

    for (int i = 1; i <= totalPartidasASimular; i++) {
      if(filaDeTimes.isEmpty()) break;

      Time timeDesafiante = filaDeTimes.poll(); //próximo da fila (Time 2, depois 3, etc)

      System.out.println("\n--- Simulando Partida " + i + ": " + timeRei.getNome() + " vs " + timeDesafiante.getNome() + " ---");

      //define placar aleatório
      int golsRei = random.nextInt(5); //0 a 4 gols
      int golsDesafiante = random.nextInt(5);

      //evita empate para forçar rodízio (regra simplificada)
      if (golsRei == golsDesafiante) golsRei++;

      //cria a partida
      Partida partida = new Partida();
      partida.setEvento(evento);
      partida.setTimeA(timeRei);
      partida.setTimeB(timeDesafiante);
      partida.setTimeAPlacar(golsRei);
      partida.setTimeBPlacar(golsDesafiante);
      partida.setStatus(StatusPartida.JOGADA); //já nasce finalizada na simulação
      partida = partidaRepository.save(partida);

      //simula gols e assistências (para subir elo dos jogadores)
      simularAcoesDeJogo(partida, timeRei, golsRei);
      simularAcoesDeJogo(partida, timeDesafiante, golsDesafiante);

      //define vencedor
      Time vencedor = (golsRei > golsDesafiante) ? timeRei : timeDesafiante;
      Time perdedor = (golsRei > golsDesafiante) ? timeDesafiante : timeRei;

      System.out.println("Placar: " + golsRei + " x " + golsDesafiante + ". Vencedor: " + vencedor.getNome());

      //processa o Rodízio (Serviço Principal)
      //o serviço vai pegar o perdedor, tirar gente e colocar reservas
      admTimesService.processarFimDePartida(partida.getId(), vencedor.getId());

      System.out.println("Rodízio aplicado ao time: " + perdedor.getNome());

      //lógica da Fila para a próxima partida:
      //vencedor continua como Rei.
      //perdedor vai para o final da fila de espera.
      timeRei = vencedor;
      filaDeTimes.offer(perdedor);
    }
  }

  private void simularAcoesDeJogo(Partida partida, Time time, int totalGols) {
    List<Inscricao> jogadoresDoTime = inscricaoRepository.findByTimeAtualAndEvento(time, partida.getEvento());

    if(jogadoresDoTime.isEmpty()) return;

    Random random = new Random();

    //para cada gol, escolhe um autor aleatório do time
    for (int k = 0; k < totalGols; k++) {
      User autorGol = jogadoresDoTime.get(random.nextInt(jogadoresDoTime.size())).getJogador();

      //chama o serviço para computar elo
      admTimesService.computarAcaoJogador(autorGol.getId(), TipoAcaoEmJogo.GOL);

      //50% de chance de ter assistência
      if (random.nextBoolean()) {
        User autorAssistencia = jogadoresDoTime.get(random.nextInt(jogadoresDoTime.size())).getJogador();
        if(!autorAssistencia.getId().equals(autorGol.getId())) {
          admTimesService.computarAcaoJogador(autorAssistencia.getId(), TipoAcaoEmJogo.ASSISTENCIA);
        }
      }
    }
  }

  private List<User> criarMuitosUsuarios() {
    List<User> lista = new ArrayList<>();

    //5 craques
    for (int i = 1; i <= 5; i++) {
      lista.add(criarUser("Craque " + i, "craque" + i + "@teste.com", 1500 + (i*10), NivelHabilidade.CRAQUE));
    }

    //15 medianos
    for (int i = 1; i <= 15; i++) {
      lista.add(criarUser("Mediano " + i, "mediano" + i + "@teste.com", 1000 + (i * 10), NivelHabilidade.MEDIANO));
    }

    //5 pernas de pau
    for (int i = 1; i <= 5; i++) {
      lista.add(criarUser("Perna " + i, "perna" + i + "@teste.com", 800 + (i * 5), NivelHabilidade.PERNA_DE_PAU));
    }

    return lista;
  }

  //métodos auxiliares

  private User criarUser(String nome, String email, Integer elo, NivelHabilidade nivel) {
    User u = new User();
    u.setNome(nome);
    u.setEmail(email);
    u.setPassword(passwordEncoder != null ? passwordEncoder.encode("123456") : "123456");
    u.setRole("USER");
    u.setEnabled(true);
    u.setPontosHabilidade(elo);
    u.setNivelHabilidade(nivel);
    u.setDataCriacao(LocalDateTime.now());
    return userRepository.save(u);
  }

  private Evento criarEvento(User organizador) {
    Evento e = new Evento();
    e.setUsuario(organizador);
    e.setNome("Copa Seeder");
    e.setLocalEvento("Arena Seeder");
    e.setDataHorarioEvento(LocalDateTime.now().plusDays(2));
    e.setJogadoresPorTime(5);
    e.setTotalPartidasDefinidas(15);
    e.setCorPrimaria("#FF0000");
    e.setCorSecundaria("#00FF00");
    e.setStatus(StatusEvento.ATIVO);
    return eventoRepository.save(e);
  }

  private void inscreverUsuario(User user, Evento evento) {
    Inscricao i = new Inscricao();
    i.setEvento(evento);
    i.setJogador(user);
    i.setPartidasJogadas(0);
    inscricaoRepository.save(i);
  }

  private void limparBanco() {
    partidaRepository.deleteAll();
    inscricaoRepository.deleteAll();
    timeRepository.deleteAll();
    eventoRepository.deleteAll();
    userRepository.deleteAll();
  }
}