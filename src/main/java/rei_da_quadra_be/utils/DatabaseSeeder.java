package rei_da_quadra_be.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import rei_da_quadra_be.enums.NivelHabilidade;
import rei_da_quadra_be.enums.StatusEvento;
import rei_da_quadra_be.enums.TipoAcaoEmJogo;
import rei_da_quadra_be.model.*;
import rei_da_quadra_be.repository.*;
import rei_da_quadra_be.service.AdmTimesService;
import rei_da_quadra_be.service.EventoService;
import rei_da_quadra_be.service.PartidaService;
import rei_da_quadra_be.service.TimeService;

import java.time.LocalDateTime;
import java.util.*;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {

  //repositories
  private final UserRepository userRepository;
  private final EventoRepository eventoRepository;
  private final InscricaoRepository inscricaoRepository;
  private final TimeRepository timeRepository;
  private final PartidaRepository partidaRepository;
  private final PasswordEncoder passwordEncoder;

  //services
  private final EventoService eventoService;
  private final PartidaService partidaService;
  private final TimeService timeService;
  private final AdmTimesService admTimesService;

  @Bean
  public CommandLineRunner seedDatabase() {
    return args -> {
      System.out.println("===Iniciando seeding===");

      limparBanco();

      // 1. Criar Usuários
      User userAdm = criarUser("ADMIN", "admin@gmail.com", 5000, NivelHabilidade.CRAQUE);
      List<User> users = criarMuitosUsuarios();

      // 2. Criar Evento via Service
      Evento evento = criarEventoViaService(userAdm);
      System.out.println("Evento criado via Service: " + evento.getNome());

      // 3. Inscrever Usuários
      users.forEach(user -> inscreverUsuario(user, evento));
      System.out.println("Usuários inscritos.");

      // 4. Montar Times via Service
      System.out.println("Executando algoritmo de distribuição de times...");
      admTimesService.montarTimesInicial(evento.getId());
      System.out.println("Times montados e balanceados.");

      // 5. Simular Torneio usando PartidaService
      System.out.println("===Iniciando Simulação de Partidas via Services===");
      simularTorneioComServices(evento);

      System.out.println("===Seeding concluído===");
    };
  }

  @Transactional
  public void simularTorneioComServices(Evento evento) {
    //usa o TimeService para buscar os times do evento criado
    List<Time> times = timeService.listarTimesDoEvento(evento.getId());

    //filtra times jogáveis (exclui o time de espera)
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
    Queue<Time> filaDeTimes = new LinkedList<>(timesJogaveis);

    /*
     * O primeiro rei da quadra vai ser um time aleatório,
     * por que não tem pontuação do evento ainda
     */
    Time timeRei = filaDeTimes.poll();

    Random random = new Random();
    int totalPartidasASimular = 10;

    for (int i = 1; i <= totalPartidasASimular; i++) {
      if (filaDeTimes.isEmpty()) break;

      Time timeDesafiante = filaDeTimes.poll();

      System.out.println("\n---Partida " + i + ": " + timeRei.getNome() + " vs " + timeDesafiante.getNome() + "---");

      // 1. Criar Partida (Service)
      Partida partida = partidaService.criarPartida(evento.getId(), timeRei.getId(), timeDesafiante.getId());

      // 2. Iniciar Partida (Service)
      partidaService.iniciarPartida(partida.getId());

      //define quantos gols cada um vai fazer no loop
      int golsRei = random.nextInt(5);
      int golsDesafiante = random.nextInt(5);
      if (golsRei == golsDesafiante) golsRei++; //evitar empate na simulação

      // 3. Simular Ações de Jogo (Gols/Assistências) via Service
      //teste do registro de desempenho e cálculo de elo em tempo real
      simularGolsViaService(partida, timeRei, golsRei);
      simularGolsViaService(partida, timeDesafiante, golsDesafiante);

      // 4. Finalizar Partida (Service)
      //o Service vai calcular o vencedor, atualizar status e chamar o RODÍZIO automaticamente
      Partida partidaFinalizada = partidaService.finalizarPartida(partida.getId());

      //verificar quem ganhou
      boolean reiGanhou = partidaFinalizada.getTimeAPlacar() > partidaFinalizada.getTimeBPlacar();
      Time vencedor = reiGanhou ? timeRei : timeDesafiante;
      Time perdedor = reiGanhou ? timeDesafiante : timeRei;

      System.out.println("Placar Final: " + partidaFinalizada.getTimeAPlacar() + " x " + partidaFinalizada.getTimeBPlacar());
      System.out.println("Vencedor: " + vencedor.getNome());
      System.out.println("Rodízio automático aplicado pelo Service ao time: " + perdedor.getNome());

      //lógica da fila:
      //rei continua, perdedor vai para o fim da fila
      timeRei = vencedor;
      filaDeTimes.offer(perdedor);
    }

    // Finalizar Evento
    eventoService.finalizarEvento(evento.getId(), evento.getUsuario());
    System.out.println("✅ Evento finalizado.");
  }

  private void simularGolsViaService(Partida partida, Time time, int totalGols) {
    List<Inscricao> jogadoresDoTime = inscricaoRepository.findByTimeAtualAndEvento(time, partida.getEvento());

    if (jogadoresDoTime.isEmpty()) return;

    Random random = new Random();

    for (int k = 0; k < totalGols; k++) {
      // Escolhe quem fez o gol
      User autorGol = jogadoresDoTime.get(random.nextInt(jogadoresDoTime.size())).getJogador();

      // CHAMA O SERVICE: Isso valida se a regra de negócio permite o gol e atualiza placar/elo
      partidaService.registrarAcao(partida.getId(), autorGol.getId(), TipoAcaoEmJogo.GOL);

      // Assistência (50% chance)
      if (random.nextBoolean()) {
        User autorAssistencia = jogadoresDoTime.get(random.nextInt(jogadoresDoTime.size())).getJogador();
        if (!autorAssistencia.getId().equals(autorGol.getId())) {
          partidaService.registrarAcao(partida.getId(), autorAssistencia.getId(), TipoAcaoEmJogo.ASSISTENCIA);
        }
      }
    }
  }

  // --- Métodos de Criação de Dados de uso local ---

  private Evento criarEventoViaService(User organizador) {
    Evento e = new Evento();
    e.setNome("Copa Services Integration");
    e.setLocalEvento("Arena Teste");
    e.setDataHorarioEvento(LocalDateTime.now().plusDays(1));
    e.setJogadoresPorTime(5);
    e.setTotalPartidasDefinidas(15);
    e.setCorPrimaria("#0000FF");
    e.setCorSecundaria("#FFFFFF");
    e.setStatus(StatusEvento.ATIVO);

    return eventoService.salvarEvento(e, organizador);
  }

  private List<User> criarMuitosUsuarios() {
    List<User> lista = new ArrayList<>();
    // 5 Craques
    for (int i = 1; i <= 5; i++)
      lista.add(criarUser("Craque " + i, "craque" + i + "@teste.com", 2500, NivelHabilidade.CRAQUE));
    // 15 Medianos
    for (int i = 1; i <= 15; i++)
      lista.add(criarUser("Mediano " + i, "mediano" + i + "@teste.com", 1500, NivelHabilidade.MEDIANO));
    // 5 Pernas
    for (int i = 1; i <= 5; i++)
      lista.add(criarUser("Perna " + i, "perna" + i + "@teste.com", 800, NivelHabilidade.PERNA_DE_PAU));
    return lista;
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
    u.setDataCriacao(LocalDateTime.now());
    return userRepository.save(u);
  }

  private void inscreverUsuario(User user, Evento evento) {
    Inscricao i = new Inscricao();
    i.setEvento(evento);
    i.setJogador(user);
    i.setPartidasJogadas(0);
    inscricaoRepository.save(i);
  }

  private void limparBanco() {
    partidaRepository.deleteAll(); //partidas primeiro
    inscricaoRepository.deleteAll();
    timeRepository.deleteAll();
    eventoRepository.deleteAll();
    userRepository.deleteAll();
  }
}