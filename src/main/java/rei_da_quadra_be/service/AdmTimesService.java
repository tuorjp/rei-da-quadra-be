package rei_da_quadra_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rei_da_quadra_be.model.*;
import rei_da_quadra_be.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdmTimesService {

  private final EventoRepository eventoRepository;
  private final TimeRepository timeRepository;
  private final InscricaoRepository inscricaoRepository;
  private final PartidaRepository partidaRepository;
  private final UserRepository userRepository; // Para salvar atualizações de Elo no User

  // Constantes de Nível
  private static final int NIVEL_CRAQUE = 3;
  private static final int NIVEL_MEDIANO = 2;
  private static final int NIVEL_PERNA_DE_PAU = 1;

  /**
   * Passo 1: Inicialização do Evento.
   * Cria os times (baldes vazios) e distribui os jogadores inscritos conforme regras.
   */
  @Transactional
  public void montarTimesInicial(Long eventoId) {
    Evento evento = eventoRepository.findById(eventoId)
      .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

    List<Inscricao> inscricoes = inscricaoRepository.findByEventoId(eventoId);
    int totalInscritos = inscricoes.size();
    int jogadoresPorTime = evento.getJogadoresPorTime();

    if (totalInscritos < jogadoresPorTime * 2) {
      throw new RuntimeException("Número insuficiente de inscritos para formar ao menos 2 times.");
    }

    // 1. Calcular quantos times "Ativos" cabem
    int qtdTimesAtivos = totalInscritos / jogadoresPorTime;
    int sobra = totalInscritos % jogadoresPorTime;

    // 2. Criar os Times no Banco
    List<Time> timesCriados = new ArrayList<>();

    // Times Ativos (Time 1, Time 2...)
    for (int i = 1; i <= qtdTimesAtivos; i++) {
      Time t = new Time();
      t.setEvento(evento);
      t.setNome("Time " + i);
      t.setTimeDeEspera(false);
      t.setStatus("ativo");
      timesCriados.add(timeRepository.save(t));
    }

    // Time de Espera (se houver sobra ou se a lógica for ter sempre uma reserva cheia)
    // A regra diz: "Caso haja incompatibilidade... sistema irá montar um time de reservas"
    // Aqui assumimos que se sobrar 1 pessoa, já existe um balde de reserva.
    Time timeEspera = new Time();
    timeEspera.setEvento(evento);
    timeEspera.setNome("Time de Espera");
    timeEspera.setTimeDeEspera(true);
    timeEspera.setStatus("ativo"); // Está ativo, mas é de espera
    timeEspera = timeRepository.save(timeEspera); // Salva referência

    // 3. Lógica de Distribuição (Algoritmo de Balanceamento)
    distribuirJogadoresNosTimes(inscricoes, timesCriados, timeEspera, jogadoresPorTime);
  }

  /**
   * Algoritmo de Distribuição Inicial
   * Regra: Pelo menos 1 craque por time, depois equilibra por Elo.
   */
  private void distribuirJogadoresNosTimes(List<Inscricao> todosInscritos, List<Time> timesAtivos, Time timeEspera, int maxPorTime) {
    // Separa os Craques
    List<Inscricao> craques = todosInscritos.stream()
      .filter(i -> i.getJogador().getNivelHabilidade() == NIVEL_CRAQUE)
      .sorted(Comparator.comparingInt(i -> -i.getJogador().getPontosHabilidade())) // Elo Decrescente
      .collect(Collectors.toList());

    // O resto (Medianos e Pernas)
    List<Inscricao> outros = todosInscritos.stream()
      .filter(i -> i.getJogador().getNivelHabilidade() != NIVEL_CRAQUE)
      .sorted(Comparator.comparingInt(i -> -i.getJogador().getPontosHabilidade())) // Elo Decrescente
      .collect(Collectors.toList());

    // Fila auxiliar para distribuição
    Queue<Inscricao> filaCraques = new LinkedList<>(craques);
    Queue<Inscricao> filaGeral = new LinkedList<>(outros);

    // Passo A: Garantir 1 Craque por time ativo (enquanto houver craques)
    for (Time time : timesAtivos) {
      if (!filaCraques.isEmpty()) {
        alocarJogador(filaCraques.poll(), time);
      } else {
        // Se acabaram os craques, pega o melhor do geral
        if (!filaGeral.isEmpty()) alocarJogador(filaGeral.poll(), time);
      }
    }

    // Se sobraram craques, joga para o topo da fila geral para serem distribuídos
    while (!filaCraques.isEmpty()) {
      // Adiciona no início da lista (uma simplificação, ideal seria reordenar)
      List<Inscricao> temp = new ArrayList<>();
      temp.add(filaCraques.poll());
      temp.addAll(filaGeral);
      filaGeral = new LinkedList<>(temp);
    }

    // Passo B: Distribuição "Snake Draft" (Serpente) para equilibrar o restante
    // Vai do Time 1 ao N, depois do N ao 1.
    boolean ida = true;
    while (!filaGeral.isEmpty()) {
      // Verifica se todos os times ativos estão cheios
      boolean temVaga = timesAtivos.stream().anyMatch(t -> contarJogadoresNoTime(t) < maxPorTime);

      if (!temVaga) {
        // Se todos os ativos estão cheios, o resto vai para a ESPERA
        alocarJogador(filaGeral.poll(), timeEspera);
        continue;
      }

      if (ida) {
        for (int i = 0; i < timesAtivos.size(); i++) {
          if (!filaGeral.isEmpty() && contarJogadoresNoTime(timesAtivos.get(i)) < maxPorTime) {
            alocarJogador(filaGeral.poll(), timesAtivos.get(i));
          }
        }
      } else {
        for (int i = timesAtivos.size() - 1; i >= 0; i--) {
          if (!filaGeral.isEmpty() && contarJogadoresNoTime(timesAtivos.get(i)) < maxPorTime) {
            alocarJogador(filaGeral.poll(), timesAtivos.get(i));
          }
        }
      }
      ida = !ida; // Inverte direção
    }
  }

  /**
   * Atualiza pontuação e processa o rodízio após uma partida.
   */
  @Transactional
  public void processarFimDePartida(Long partidaId, Long timeVencedorId) {
    Partida partida = partidaRepository.findById(partidaId)
      .orElseThrow(() -> new RuntimeException("Partida não encontrada"));
    Evento evento = partida.getEvento();

    // 1. Identificar Perdedor
    Time timePerdedor = partida.getTimeA().getId().equals(timeVencedorId) ? partida.getTimeB() : partida.getTimeA();

    // 2. Atualizar Elo e Stats dos Jogadores (Incremento por gol/defesa deve ser feito antes, ao registrar o scout)
    // Aqui focamos na regra de rodízio.

    // Incrementar contador de partidas jogadas para quem estava em campo
    List<Inscricao> jogadoresTimeA = inscricaoRepository.findByTimeAtualAndEvento(partida.getTimeA(), evento);
    List<Inscricao> jogadoresTimeB = inscricaoRepository.findByTimeAtualAndEvento(partida.getTimeB(), evento);

    jogadoresTimeA.forEach(this::incrementarPartidaJogada);
    jogadoresTimeB.forEach(this::incrementarPartidaJogada);

    // 3. Realizar Rodízio (Perdedor sai <-> Reserva entra)
    rodizioDeJogadores(evento, timePerdedor);
  }

  /**
   * Lógica Central do Rodízio com "Tickets"
   */
  private void rodizioDeJogadores(Evento evento, Time timePerdedor) {
    // Busca o time de espera do evento
    Time timeEspera = timeRepository.findByEventoAndTimeDeEsperaTrue(evento)
      .orElseThrow(() -> new RuntimeException("Time de espera não configurado"));

    List<Inscricao> jogadoresNoBanco = inscricaoRepository.findByTimeAtualAndEvento(timeEspera, evento);
    List<Inscricao> jogadoresNoTimePerdedor = inscricaoRepository.findByTimeAtualAndEvento(timePerdedor, evento);

    if (jogadoresNoBanco.isEmpty()) {
      return; // Ninguém pra trocar, segue o jogo
    }

    int tamanhoDoTime = evento.getJogadoresPorTime();

    // --- QUEM ENTRA (Sai do Banco -> Vai pro Time Perdedor/Campo) ---
    // Regra de Ticket: Prioridade para quem jogou MENOS.
    // Desempate: Nivel de Habilidade (para equilibrar) ou Ordem de Chegada (ID).
    List<Inscricao> quemEntra = jogadoresNoBanco.stream()
      .sorted(Comparator
        .comparingInt(Inscricao::getPartidasJogadas) // Menor número de partidas primeiro (Ticket)
        .thenComparingInt(i -> i.getJogador().getPontosHabilidade())) // Desempate por Elo (opcional)
      .limit(tamanhoDoTime)
      .collect(Collectors.toList());

    // --- QUEM SAI (Sai do Time Perdedor -> Vai pro Banco) ---
    // A regra diz "jogadores do time perdedor serão selecionados para compor time de reserva".
    // Normalmente sai o time inteiro, mas se o banco for menor que o time, fazemos troca parcial?
    // Assumindo troca total ou até esvaziar o banco.
    List<Inscricao> quemSai = new ArrayList<>(jogadoresNoTimePerdedor);

    // Executa a troca no banco de dados
    for (Inscricao entrando : quemEntra) {
      entrando.setTimeAtual(timePerdedor); // Entra em campo (no lugar do perdedor)
      inscricaoRepository.save(entrando);
    }

    for (Inscricao saindo : quemSai) {
      saindo.setTimeAtual(timeEspera); // Vai pro banco
      inscricaoRepository.save(saindo);
    }

    // Nota: Dependendo da regra exata de "Rei da Quadra", o time vencedor continua.
    // O time perdedor recebe os novos jogadores e vira o "Desafiante".
  }

  /**
   * Método para atualizar pontuação individual (chamado a cada gol/ação)
   */
  @Transactional
  public void computarAcaoJogador(Long jogadorId, String tipoAcao) {
    User user = userRepository.findById(jogadorId).orElseThrow();

    int pontosGanhos = 0;
    switch (tipoAcao) {
      case "GOL": pontosGanhos = 15; break;
      case "ASSISTENCIA": pontosGanhos = 10; break;
      case "DEFESA": pontosGanhos = 5; break;
      // etc
    }

    // Atualiza Elo
    user.setPontosHabilidade(user.getPontosHabilidade() + pontosGanhos);

    // Pode atualizar o Nível se bater certas metas
    if (user.getPontosHabilidade() > 2400) user.setNivelHabilidade(NIVEL_CRAQUE);
    else if (user.getPontosHabilidade() > 800) user.setNivelHabilidade(NIVEL_MEDIANO);

    userRepository.save(user);
  }

  // --- Métodos Auxiliares ---

  private void alocarJogador(Inscricao inscricao, Time time) {
    inscricao.setTimeAtual(time);
    inscricaoRepository.save(inscricao);
  }

  private long contarJogadoresNoTime(Time time) {
    // Idealmente usar countByTimeAtual no repository para performance
    return inscricaoRepository.countByTimeAtual(time);
  }

  private void incrementarPartidaJogada(Inscricao i) {
    i.setPartidasJogadas(i.getPartidasJogadas() + 1);
    inscricaoRepository.save(i);
  }
}