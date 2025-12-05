package rei_da_quadra_be.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import rei_da_quadra_be.enums.NivelHabilidade;
import rei_da_quadra_be.enums.StatusEvento;
import rei_da_quadra_be.enums.StatusInscricao;
import rei_da_quadra_be.enums.TipoAcaoEmJogo;
import rei_da_quadra_be.model.*;
import rei_da_quadra_be.repository.*;
import rei_da_quadra_be.service.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {

    // repositories
    private final UserRepository userRepository;
    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final TimeRepository timeRepository;
    private final PartidaRepository partidaRepository;
    private final PasswordEncoder passwordEncoder;

    // services
    private final EventoService eventoService;
    private final PartidaService partidaService;
    private final TimeService timeService;
    private final AdmTimesService admTimesService;
    private final UserService userService;
    private final AuthorizationService authorizationService;

    // Contador para controle das imagens 1.png a 25.png
    private int contadorImagens = 1;

    @Bean
    public CommandLineRunner seedDatabase() {
        return args -> {
            System.out.println("===Iniciando seeding===");

            limparBanco();

            // Reinicia contador para garantir ordem correta a cada execução
            contadorImagens = 1;

            // 1. Criar Usuários
            // Tenta carregar a imagem do admin, se não conseguir, gera avatar
            String fotoAdmin = carregarImagem("avatares/admin.png");
            if (fotoAdmin == null)
                fotoAdmin = gerarAvatarBase64("ADMIN");

            User userAdm = criarUser("ADMIN", "admin@gmail.com", 5000, NivelHabilidade.CRAQUE, fotoAdmin);

            criarCenarioPersonalizado();
            criarCenarioComReserva();

            // Cria os demais usuários distribuindo as imagens 1 a 25
            List<User> users = criarMuitosUsuarios();

            // 2. Criar Múltiplos Eventos em Anápolis
            // Gera 5 eventos principais + 25 aleatórios
            List<Evento> eventosAnapolis = criarEventosAnapolis(userAdm, users);

            // O evento principal para simulação será o primeiro da lista (Copa Ipiranga)
            Evento eventoPrincipal = eventosAnapolis.get(0);
            System.out.println("Evento Principal selecionado para Simulação: " + eventoPrincipal.getNome());

            // 3. Inscrever Usuários (Apenas no evento principal para a simulação funcionar
            // e ter times)
            users.forEach(user -> inscreverUsuario(user, eventoPrincipal));
            System.out.println("Usuários inscritos no evento principal.");

            // 4. Montar Times via Service (Apenas no evento principal)
            System.out.println("Executando algoritmo de distribuição de times...");
            admTimesService.montarTimesInicial(eventoPrincipal.getId());
            System.out.println("Times montados e balanceados.");

            // 5. Simular Torneio usando PartidaService (Apenas no evento principal)
            System.out.println("===Iniciando Simulação de Partidas via Services===");
            simularTorneioComServices(eventoPrincipal);

            System.out.println("===Seeding concluído===");
        };
    }

    // --- NOVO METODO: Cria eventos com coordenadas reais de Anápolis ---
    private List<Evento> criarEventosAnapolis(User admin, List<User> outrosUsuarios) {
        List<Evento> eventosCriados = new ArrayList<>();

        // Pega usuários comuns para serem donos de alguns eventos, variando os
        // organizadores
        User organizadorSecundario = outrosUsuarios.isEmpty() ? admin : outrosUsuarios.get(0);
        User organizadorTerciario = outrosUsuarios.size() > 1 ? outrosUsuarios.get(1) : admin;

        // --- 5 Eventos Principais (Locais Reais) ---

        // Evento 1: Parque Ipiranga (Principal - será simulado)
        Evento e1 = new Evento();
        e1.setNome("Copa Ipiranga de Futsal");
        e1.setLocalEvento("Parque Ipiranga - Jundiaí, Anápolis");
        e1.setLatitude(-16.3443);
        e1.setLongitude(-48.9478);
        e1.setDataHorarioEvento(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).withHour(19).withMinute(0)); // Amanhã
                                                                                                            // 19h
        e1.setJogadoresPorTime(5);
        e1.setTotalPartidasDefinidas(15);
        e1.setCorPrimaria("#0000FF");
        e1.setCorSecundaria("#FFFFFF");
        e1.setStatus(StatusEvento.ATIVO);
        eventosCriados.add(eventoService.salvarEvento(e1, admin));

        // Evento 2: Ginásio Newton de Faria
        Evento e2 = new Evento();
        e2.setNome("Torneio Interbairros");
        e2.setLocalEvento("Ginásio Internacional Newton de Faria");
        e2.setLatitude(-16.3350);
        e2.setLongitude(-48.9500);
        e2.setDataHorarioEvento(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).withHour(20).withMinute(30));
        e2.setJogadoresPorTime(6);
        e2.setTotalPartidasDefinidas(10);
        e2.setCorPrimaria("#FF0000");
        e2.setStatus(StatusEvento.ATIVO);
        eventosCriados.add(eventoService.salvarEvento(e2, admin));

        // Evento 3: Jaiara
        Evento e3 = new Evento();
        e3.setNome("Racha da Jaiara");
        e3.setLocalEvento("Quadra da Av. Fernando Costa - Jaiara");
        e3.setLatitude(-16.2955);
        e3.setLongitude(-48.9602);
        e3.setDataHorarioEvento(OffsetDateTime.now(ZoneOffset.UTC).plusHours(5)); // Hoje mais tarde
        e3.setJogadoresPorTime(5);
        e3.setTotalPartidasDefinidas(5);
        e3.setStatus(StatusEvento.ATIVO);
        eventosCriados.add(eventoService.salvarEvento(e3, organizadorSecundario));

        // Evento 4: UniEVANGÉLICA
        Evento e4 = new Evento();
        e4.setNome("Treino Universitário");
        e4.setLocalEvento("UniEVANGÉLICA - Cidade Universitária");
        e4.setLatitude(-16.3089);
        e4.setLongitude(-48.9450);
        e4.setDataHorarioEvento(OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withHour(17).withMinute(0));
        e4.setJogadoresPorTime(7);
        e4.setStatus(StatusEvento.ATIVO);
        eventosCriados.add(eventoService.salvarEvento(e4, organizadorSecundario));

        // Evento 5: DAIA
        Evento e5 = new Evento();
        e5.setNome("Futebol dos Trabalhadores");
        e5.setLocalEvento("Campo do DAIA");
        e5.setLatitude(-16.4005);
        e5.setLongitude(-48.9208);
        e5.setDataHorarioEvento(OffsetDateTime.now(ZoneOffset.UTC).plusDays(4).withHour(18).withMinute(30));
        e5.setJogadoresPorTime(11);
        e5.setStatus(StatusEvento.ATIVO);
        eventosCriados.add(eventoService.salvarEvento(e5, admin));

        // --- GERAR MAIS 25 EVENTOS ESPALHADOS ---
        eventosCriados.addAll(gerarMais25Eventos(admin, organizadorSecundario, organizadorTerciario));

        return eventosCriados;
    }

    private List<Evento> gerarMais25Eventos(User u1, User u2, User u3) {
        List<Evento> extras = new ArrayList<>();
        Random rand = new Random();
        User[] organizadores = { u1, u2, u3 };

        // Dados para geração aleatória
        String[] nomesEventos = {
                "Racha dos Amigos", "Fut de Quinta", "Domingueira da Bola", "Clássico do Bairro",
                "Desafio anual", "Liga Amadora", "Desafio dos Campeões", "Futebol Solidário",
                "Copa da Várzea", "Treino Tático", "Jogo Beneficente", "Encontro dos Veteranos",
                "Supercopa Anápolis", "Torneio Relâmpago", "Racha Semanal", "Fut Manhã de Sol",
                "Desafio da Galera", "Pelada dos Amigos", "Copa Anapolina", "Futebol Arte",
                "Copa Jundiaí", "Torneio Jaiara", "Racha do Centro", "Fut Vila Góis", "Pelada Maracanã"
        };

        String[] locais = {
                "Quadra da Praça Bom Jesus", "Campo do Maracanã", "Clube Ipiranga", "SESC Anápolis",
                "Estádio Jonas Duarte", "Quadra do Bairro de Lourdes", "Campo da Vila Jaiara",
                "Centro Esportivo da Vila Góis", "Quadra do Recanto do Sol", "Campo do Filostro",
                "Arena Jundiaí", "Society do Centro", "Quadra da Escola Militar", "Campo do Itamaraty",
                "Poliesportivo da Vila União", "Campo do Boa Vista", "Quadra do São Joaquim",
                "Arena Anápolis City", "Campo do Vivian Parque", "Quadra do Santa Maria",
                "Society da Avenida Brasil", "Campo do Jardim Europa", "Quadra do Calixtolândia",
                "Centro de Treinamento", "Quadra da Praça Jamel Cecílio"
        };

        // Centro aproximado de Anápolis
        double latBase = -16.3289;
        double lonBase = -48.9534;

        for (int i = 0; i < 25; i++) {
            Evento e = new Evento();
            e.setNome(nomesEventos[i % nomesEventos.length] + " #" + (i + 1));
            e.setLocalEvento(locais[i % locais.length]);

            // Gera coordenada aleatória num raio próximo (~5-7km do centro)
            // 0.1 grau é aprox 11km, então 0.06 é aprox 6km
            double latOffset = (rand.nextDouble() - 0.5) * 0.12;
            double lonOffset = (rand.nextDouble() - 0.5) * 0.12;
            e.setLatitude(latBase + latOffset);
            e.setLongitude(lonBase + lonOffset);

            // Datas variadas nos próximos 10 dias
            int diasFuturos = rand.nextInt(10);
            int hora = 8 + rand.nextInt(14); // Entre 8h e 22h
            e.setDataHorarioEvento(
                    OffsetDateTime.now(ZoneOffset.UTC).plusDays(diasFuturos).withHour(hora).withMinute(0));

            e.setJogadoresPorTime(5 + rand.nextInt(7)); // 5 a 11 jogadores
            e.setStatus(StatusEvento.ATIVO);
            e.setTotalPartidasDefinidas(1);

            // Alterna organizadores
            User org = organizadores[rand.nextInt(organizadores.length)];

            extras.add(eventoService.salvarEvento(e, org));
        }

        return extras;
    }

    @Transactional
    public void simularTorneioComServices(Evento evento) {
        // usa o TimeService para buscar os times do evento criado
        List<Time> times = timeService.listarTimesDoEvento(evento.getId());

        // filtra times jogáveis (exclui o time de espera)
        List<Time> timesJogaveis = new ArrayList<>(
                times.stream()
                        .filter(t -> !t.getTimeDeEspera())
                        .toList());

        if (timesJogaveis.size() < 2) {
            System.out.println("Não há times suficientes para simular partidas.");
            return;
        }

        // fila de desafiantes
        Queue<Time> filaDeTimes = new LinkedList<>(timesJogaveis);

        /*
         * O primeiro rei da quadra vai ser um time aleatório,
         * por que não tem pontuação do evento ainda
         */
        Time timeRei = filaDeTimes.poll();

        Random random = new Random();
        int totalPartidasASimular = 10;

        for (int i = 1; i <= totalPartidasASimular; i++) {
            if (filaDeTimes.isEmpty())
                break;

            Time timeDesafiante = filaDeTimes.poll();

            System.out.println(
                    "\n---Partida " + i + ": " + timeRei.getNome() + " vs " + timeDesafiante.getNome() + "---");

            // 1. Criar Partida (Service)
            Partida partida = partidaService.criarPartida(evento.getId(), timeRei.getId(), timeDesafiante.getId());

            // 2. Iniciar Partida (Service)
            partidaService.iniciarPartida(partida.getId());

            // define quantos gols cada um vai fazer no loop
            int golsRei = random.nextInt(5);
            int golsDesafiante = random.nextInt(5);
            if (golsRei == golsDesafiante)
                golsRei++; // evitar empate na simulação

            // 3. Simular Ações de Jogo (Gols/Assistências) via Service
            // teste do registro de desempenho e cálculo de elo em tempo real
            simularGolsViaService(partida, timeRei, golsRei);
            simularGolsViaService(partida, timeDesafiante, golsDesafiante);

            // 4. Finalizar Partida (Service)
            // o Service vai calcular o vencedor, atualizar status e chamar o RODÍZIO
            // automaticamente
            Partida partidaFinalizada = partidaService.finalizarPartida(partida.getId());

            // verificar quem ganhou
            boolean reiGanhou = partidaFinalizada.getTimeAPlacar() > partidaFinalizada.getTimeBPlacar();
            Time vencedor = reiGanhou ? timeRei : timeDesafiante;
            Time perdedor = reiGanhou ? timeDesafiante : timeRei;

            System.out.println(
                    "Placar Final: " + partidaFinalizada.getTimeAPlacar() + " x " + partidaFinalizada.getTimeBPlacar());
            System.out.println("Vencedor: " + vencedor.getNome());
            System.out.println("Rodízio automático aplicado pelo Service ao time: " + perdedor.getNome());

            // lógica da fila:
            // rei continua, perdedor vai para o fim da fila
            timeRei = vencedor;
            filaDeTimes.offer(perdedor);
        }

        // Finalizar Evento
        eventoService.finalizarEvento(evento.getId(), evento.getUsuario());
        System.out.println("✅ Evento finalizado.");
    }

    private void simularGolsViaService(Partida partida, Time time, int totalGols) {
        List<Inscricao> jogadoresDoTime = inscricaoRepository.findByTimeAtualAndEvento(time, partida.getEvento());

        if (jogadoresDoTime.isEmpty())
            return;

        Random random = new Random();
        Set<Long> jogadoresComAcao = new HashSet<>(); // Controla quem já teve ação registrada

        // 1. Simula Gols (Lógica Original)
        for (int k = 0; k < totalGols; k++) {
            User autorGol = jogadoresDoTime.get(random.nextInt(jogadoresDoTime.size())).getJogador();
            partidaService.registrarAcao(partida.getId(), autorGol.getId(), TipoAcaoEmJogo.GOL);
            jogadoresComAcao.add(autorGol.getId());

            // Assistência (50% chance)
            if (random.nextBoolean()) {
                User autorAssistencia = jogadoresDoTime.get(random.nextInt(jogadoresDoTime.size())).getJogador();
                if (!autorAssistencia.getId().equals(autorGol.getId())) {
                    partidaService.registrarAcao(partida.getId(), autorAssistencia.getId(), TipoAcaoEmJogo.ASSISTENCIA);
                    jogadoresComAcao.add(autorAssistencia.getId());
                }
            }
        }

        // 2. Simula Ações Diversas para quem não fez gol (Para popular o histórico)
        for (Inscricao inscricao : jogadoresDoTime) {
            User jogador = inscricao.getJogador();

            // Se o jogador ainda não teve ação nessa partida, damos uma ação de defesa ou
            // falta
            if (!jogadoresComAcao.contains(jogador.getId())) {
                int acaoAleatoria = random.nextInt(100);

                if (acaoAleatoria < 60) {
                    // 60% de chance de fazer uma DEFESA (Ganhar pontos)
                    partidaService.registrarAcao(partida.getId(), jogador.getId(), TipoAcaoEmJogo.DEFESA);
                } else if (acaoAleatoria < 80) {
                    // 20% de chance de cometer FALTA (Perder pontos)
                    partidaService.registrarAcao(partida.getId(), jogador.getId(), TipoAcaoEmJogo.FALTA);
                }
                // 20% chance de não ter registro específico (apenas jogou)
            }
        }
    }

    private List<User> criarMuitosUsuarios() {
        List<User> lista = new ArrayList<>();
        // 5 Craques
        for (int i = 1; i <= 5; i++) {
            String foto = carregarProximaImagem();
            lista.add(criarUser("Craque " + i, "craque" + i + "@teste.com", 2500, NivelHabilidade.CRAQUE, foto));
        }
        // 15 Medianos
        for (int i = 1; i <= 15; i++) {
            String foto = carregarProximaImagem();
            lista.add(criarUser("Mediano " + i, "mediano" + i + "@teste.com", 1500, NivelHabilidade.MEDIANO, foto));
        }
        // 5 Pernas
        for (int i = 1; i <= 5; i++) {
            String foto = carregarProximaImagem();
            lista.add(criarUser("Perna de Pau " + i, "perna" + i + "@teste.com", 200, NivelHabilidade.PERNA_DE_PAU,
                    foto));
        }
        return lista;
    }

    private User criarUser(String nome, String email, Integer elo, NivelHabilidade nivel, String fotoUrl) {
        User u = new User();
        u.setNome(nome);
        u.setEmail(email);
        u.setPassword(passwordEncoder != null ? passwordEncoder.encode("123456") : "123456");
        u.setRole("USER");
        u.setEnabled(true);
        u.setPontosHabilidade(elo);
        u.setNivelHabilidade(nivel);
        u.setDataCriacao(LocalDateTime.now());

        // Se a foto foi carregada com sucesso, usa ela. Se não, gera um avatar
        // dinâmico.
        if (fotoUrl != null) {
            u.setFotoPerfil(fotoUrl);
        } else {
            u.setFotoPerfil(gerarAvatarBase64(nome));
        }

        return userRepository.save(u);
    }

    private void inscreverUsuario(User user, Evento evento) {
        Inscricao i = new Inscricao();
        i.setEvento(evento);
        i.setJogador(user);
        i.setPartidasJogadas(0);
        i.setDataInscricao(LocalDateTime.now());
        i.setStatus(StatusInscricao.APROVADA);
        inscricaoRepository.save(i);
    }

    private void limparBanco() {
        partidaRepository.deleteAll();
        inscricaoRepository.deleteAll();
        timeRepository.deleteAll();
        eventoRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- MÉTODOS AUXILIARES PARA CARREGAMENTO E GERAÇÃO DE IMAGENS ---

    private String carregarProximaImagem() {
        // Tenta carregar a imagem sequencial (1.png, 2.png...)
        String nomeArquivo = "avatares/" + contadorImagens + ".png"; // Caso use jpg, ajustar aqui
        String base64 = carregarImagem(nomeArquivo);

        // Se a imagem existir, incrementa o contador para o próximo usuário
        if (base64 != null) {
            contadorImagens++;
        }
        return base64;
    }

    private String carregarImagem(String caminho) {
        try {
            ClassPathResource imgFile = new ClassPathResource(caminho);
            if (!imgFile.exists()) {
                // Se não achar o arquivo, retorna null para cair no fallback (gerador de
                // avatar)
                return null;
            }
            byte[] imageBytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            // Ajuste o mimeType se estiver usando .jpg ou .jpeg
            String mimeType = caminho.endsWith(".jpg") || caminho.endsWith(".jpeg") ? "jpeg" : "png";

            return "data:image/" + mimeType + ";base64," + base64;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Fallback: Gera avatar com a inicial se o arquivo não for encontrado
    private String gerarAvatarBase64(String nome) {
        try {
            int width = 150;
            int height = 150;
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedImage.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2d.setColor(new Color((int) (Math.random() * 0x1000000)));
            g2d.fillRect(0, 0, width, height);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 80));
            String letra = (nome != null && !nome.isEmpty()) ? nome.substring(0, 1).toUpperCase() : "?";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (width - fm.stringWidth(letra)) / 2;
            int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(letra, x, y);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    // --- NOVO MÉTODO SOLICITADO: Cria um cenário isolado (Jogadores + Evento +
    // Inscrições) ---
    private void criarCenarioPersonalizado() {
        System.out.println(">>> Iniciando criação de Cenário Personalizado...");

        // 1. Criar o Organizador do Evento
        // Gera um avatar automático passando null na foto
        User organizador = (User) authorizationService.loadUserByUsername("admin@gmail.com");

        // 2. Criar o Evento
        Evento evento = new Evento();
        evento.setNome("AQUIIIIII");
        evento.setLocalEvento("Arena Code - Anápolis");
        evento.setLatitude(-16.3250); // Coordenada ilustrativa
        evento.setLongitude(-48.9550);
        evento.setDataHorarioEvento(OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).withHour(20).withMinute(0));
        evento.setJogadoresPorTime(5);
        evento.setTotalPartidasDefinidas(5);
        evento.setStatus(StatusEvento.ATIVO);

        // Salva o evento definindo o organizador (Service aplica regras de negócio se
        // houver)
        Evento eventoSalvo = eventoService.salvarEvento(evento, organizador);
        System.out.println("ENVENTO IDDDDDDDDDD" + eventoSalvo.getId());

        // 3. Criar Jogadores e gerar Inscrição para cada um
        int totalJogadores = 15; // Ex: Suficiente para 3 times de 5

        for (int i = 1; i <= totalJogadores; i++) {
            // Cria o usuário (Jogador)
            User jogador = criarUser(
                    "Jogador Teste " + i,
                    "jogador.teste" + i + "@gmail.com",
                    1000 + (i * 50), // Varia o ELO ligeiramente
                    (i % 2 == 0) ? NivelHabilidade.MEDIANO : NivelHabilidade.PERNA_DE_PAU, // Intercala níveis
                    null // Gera avatar automático
            );

            // Cria a inscrição para este jogador no evento
            inscreverUsuario(jogador, eventoSalvo);
        }

        System.out.println(
                ">>> Cenário criado: Evento '" + eventoSalvo.getNome() + "' com " + totalJogadores + " inscritos.");
    }

    /**
     * Cria um cenário de demonstração com time de espera (reserva) garantido.
     */
    private void criarCenarioComReserva() {
        System.out.println(">>> Criando cenário com reserva...");

        User organizador = (User) authorizationService.loadUserByUsername("admin@gmail.com");

        Evento evento = new Evento();
        evento.setNome("Cenário Reserva");
        evento.setLocalEvento("Arena Demo");
        evento.setLatitude(-16.3300);
        evento.setLongitude(-48.9500);
        evento.setDataHorarioEvento(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).withHour(18).withMinute(0));
        evento.setJogadoresPorTime(5);
        // Definido para 6 partidas conforme requisito (seis partidas no evento)
        evento.setTotalPartidasDefinidas(6);
        evento.setStatus(StatusEvento.ATIVO);

        Evento eventoSalvo = eventoService.salvarEvento(evento, organizador);
        System.out.println(
                "Evento de demonstração criado: " + eventoSalvo.getNome() + " (id=" + eventoSalvo.getId() + ")");

        // Calcula número de jogadores para garantir sobras (reserva)
        int jogadoresPorTime = eventoSalvo.getJogadoresPorTime() != null ? eventoSalvo.getJogadoresPorTime() : 5;
        int numTimesAtivos = 3; // 3 times principais em campo
        int sobraReserva = 3; // 3 jogadores no banco (time reserva)
        // Total: 3 times * 5 jogadores + 3 reserva = 18 jogadores
        int totalJogadores = jogadoresPorTime * numTimesAtivos + sobraReserva;

        for (int i = 1; i <= totalJogadores; i++) {
            User jogador = criarUser(
                    "Demo Jogador " + i,
                    "demo.jogador" + i + "@example.com",
                    1000 + i * 10,
                    (i % 2 == 0) ? NivelHabilidade.MEDIANO : NivelHabilidade.PERNA_DE_PAU,
                    null);
            inscreverUsuario(jogador, eventoSalvo);
        }

        // Monta times e garante que exista time de espera preenchido com as sobras
        admTimesService.montarTimesInicial(eventoSalvo.getId());

        // Log do time de espera e seus jogadores
        Optional<Time> timeEsperaOpt = timeRepository.findByEventoAndTimeDeEsperaTrue(eventoSalvo);
        if (timeEsperaOpt.isPresent()) {
            Time timeEspera = timeEsperaOpt.get();
            List<Inscricao> inscritosNoBanco = inscricaoRepository.findByTimeAtualAndEvento(timeEspera, eventoSalvo);
            System.out.println("Time de Espera: " + timeEspera.getNome() + " (id=" + timeEspera.getId() + ")");
            System.out.println("Jogadores no banco: " + inscritosNoBanco.size());
            inscritosNoBanco.forEach(ins -> System.out.println(" - " + ins.getJogador().getNome()));
        } else {
            System.out.println("Nenhum time de espera encontrado para o evento de demonstração.");
        }
        
        // Exibe resumo: queremos 4 times no total (3 principais + 1 reserva)
        long totalDePartidas = eventoSalvo.getTotalPartidasDefinidas() != null ? eventoSalvo.getTotalPartidasDefinidas() : 0;
        long totalTimes = timeRepository.countByEvento(eventoSalvo);
        System.out.println(">>> Cenário de demonstração com reserva criado com " + totalJogadores + " inscritos.");
        System.out.println(">>> Total de times criados para o evento: " + totalTimes + " (esperado: 4)");
        System.out.println(">>> Total de partidas para o evento: " + totalDePartidas + " (esperado: 6)");
    }
}