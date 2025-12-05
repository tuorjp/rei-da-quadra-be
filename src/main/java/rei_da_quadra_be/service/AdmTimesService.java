package rei_da_quadra_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rei_da_quadra_be.enums.NivelHabilidade;
import rei_da_quadra_be.enums.StatusTime;
import rei_da_quadra_be.enums.TipoAcaoEmJogo;
import rei_da_quadra_be.model.*;
import rei_da_quadra_be.repository.*;
import rei_da_quadra_be.service.exception.EventoNaoEncontradoException;
import rei_da_quadra_be.service.exception.NumeroInsuficienteInscritosException;
import rei_da_quadra_be.service.exception.TimeDeEsperaNaoConfiguradoException;
import rei_da_quadra_be.utils.EloCalculator;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdmTimesService {

  private final EventoRepository eventoRepository;
  private final TimeRepository timeRepository;
  private final InscricaoRepository inscricaoRepository;
  private final PartidaRepository partidaRepository;
  private final UserRepository userRepository;

  private final HistoricoPontuacaoService historicoService;

  //cria os times de um evento que foi criado
  //nenhuma partida ocorreu ainda
  @Transactional
  public void montarTimesInicial(Long eventoId) {
    Evento evento = eventoRepository
      .findById(eventoId)
      .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));

    List<Inscricao> inscricoes = inscricaoRepository.findByEventoId(eventoId);
    int totalInscritos = inscricoes.size();
    int jogadoresPorTime = evento.getJogadoresPorTime();

    if (totalInscritos < jogadoresPorTime * 2) {
      throw new NumeroInsuficienteInscritosException("Número insuficiente de inscritos para formar ao menos 2 times.");
    }

    //calcular quantos times "Ativos" cabem
    int qtdTimesAtivos = totalInscritos / jogadoresPorTime;
    int sobra = totalInscritos % jogadoresPorTime;

    //cria os times no Banco
    List<Time> timesCriados = new ArrayList<>();

    //times ativos
    for (int i = 1; i <= qtdTimesAtivos; i++) {
      Time t = new Time();
      t.setEvento(evento);
      t.setNome("Time " + i);
      t.setTimeDeEspera(false);
      t.setStatus(StatusTime.ATIVO);
      timesCriados.add(timeRepository.save(t));
    }

    /*
     * Time de Espera
     * A regra diz: "Caso haja incompatibilidade... o sistema irá montar um time de reservas"
     * Aqui assumimos que se sobrar 1 pessoa, já existe um balde de reserva.
     */
    Time timeEspera = new Time();
    timeEspera.setEvento(evento);
    timeEspera.setNome("Time de Espera");
    timeEspera.setTimeDeEspera(true);
    timeEspera.setStatus(StatusTime.ESPERA); //ativo, mas é de espera
    timeEspera = timeRepository.save(timeEspera);

    //Lógica de Distribuição (Algoritmo de Balanceamento)
    distribuirJogadoresNosTimes(inscricoes, timesCriados, timeEspera, jogadoresPorTime);
  }

  //distribuição Inicial
  //regra: pelo menos 1 craque por time, depois equilibra por Elo.
  private void distribuirJogadoresNosTimes(List<Inscricao> todosInscritos, List<Time> timesAtivos, Time timeEspera, int maxPorTime) {
    //separa os Craques
    List<Inscricao> craques = todosInscritos
      .stream()
      .filter(i -> i.getJogador().getNivelHabilidade() == NivelHabilidade.CRAQUE)
      .sorted(Comparator.comparingInt(i -> -i.getJogador().getPontosHabilidade())) //elo decrescente
      .toList();

    //medianos e pernas-de-pau
    List<Inscricao> outros = todosInscritos
      .stream()
      .filter(i -> i.getJogador().getNivelHabilidade() != NivelHabilidade.CRAQUE)
      .sorted(Comparator.comparingInt(i -> -i.getJogador().getPontosHabilidade())) //elo decrescente
      .toList();

    //fila auxiliar para distribuição
    Queue<Inscricao> filaCraques = new LinkedList<>(craques);
    Queue<Inscricao> filaGeral = new LinkedList<>(outros);

    //garantir 1 Craque por time ativo (enquanto houver craques)
    for (Time time : timesAtivos) {
      if (!filaCraques.isEmpty()) {
        alocarJogador(filaCraques.poll(), time);
      } else {
        //se acabaram os craques, pega o melhor do geral
        if (!filaGeral.isEmpty()) alocarJogador(filaGeral.poll(), time);
      }
    }

    //se sobraram craques, joga para o topo da fila geral para serem distribuídos
    while (!filaCraques.isEmpty()) {
      //adiciona no início da lista (uma simplificação, ideal seria reordenar)
      List<Inscricao> temp = new ArrayList<>();
      temp.add(filaCraques.poll());
      temp.addAll(filaGeral);
      filaGeral = new LinkedList<>(temp);
    }

    //distribuição "Snake Draft" (Serpente) para equilibrar o restante
    //pai do Time 1 ao N, depois do N ao 1.
    boolean ida = true;
    while (!filaGeral.isEmpty()) {
      //verifica se todos os times ativos estão cheios
      boolean temVaga = timesAtivos.stream().anyMatch(t -> contarJogadoresNoTime(t) < maxPorTime);

      if (!temVaga) {
        //se todos os ativos estão cheios, o resto vai para a ESPERA
        alocarJogador(Objects.requireNonNull(filaGeral.poll()), timeEspera);
        continue;
      }

      if (ida) {
        for (int i = 0; i < timesAtivos.size(); i++) {
          if (!filaGeral.isEmpty() && contarJogadoresNoTime(timesAtivos.get(i)) < maxPorTime) {
            alocarJogador(Objects.requireNonNull(filaGeral.poll()), timesAtivos.get(i));
          }
        }
      } else {
        for (int i = timesAtivos.size() - 1; i >= 0; i--) {
          if (!filaGeral.isEmpty() && contarJogadoresNoTime(timesAtivos.get(i)) < maxPorTime) {
            alocarJogador(Objects.requireNonNull(filaGeral.poll()), timesAtivos.get(i));
          }
        }
      }
      ida = !ida; //inverte direção
    }
  }

  //atualiza pontuação e processa o rodízio após uma partida.
  @Transactional
  public Long processarFimDePartida(Long partidaId, Long timeVencedorId) {
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

    // 2.5. Atualizar pontuação com base no sistema ELO
    boolean timeAVenceu = partida.getTimeA().getId().equals(timeVencedorId);
    atualizarPontuacaoElo(partida, partida.getTimeA(), partida.getTimeB(), timeAVenceu);
    atualizarPontuacaoElo(partida, partida.getTimeB(), partida.getTimeA(), !timeAVenceu);

    // 3. Realizar Rodízio (Perdedor sai <-> Reserva entra)
    Time timeEspera = timeRepository
      .findByEventoAndTimeDeEsperaTrue(evento)
      .orElseThrow(() -> new TimeDeEsperaNaoConfiguradoException("Time de espera não configurado"));

    List<Inscricao> jogadoresNoBanco = inscricaoRepository.findByTimeAtualAndEvento(timeEspera, evento);

    // Se houver jogadores no banco, executa rodízio padrão imediatamente para atualizar os times
    if (!jogadoresNoBanco.isEmpty()) {
      rodizioDeJogadores(evento, timePerdedor);
    }

    // Agora escolhe o próximo desafiante entre os times ativos (não reservas), excluindo o vencedor.
    List<Time> timesAtivos = timeRepository.findByEventoId(evento.getId())
      .stream()
      .filter(t -> !t.getTimeDeEspera() && t.getStatus() == StatusTime.ATIVO)
      .toList();

    // Se só existir o vencedor como time ativo, retorna o perdedor original como fallback
    List<Time> candidatos = timesAtivos.stream()
      .filter(t -> !t.getId().equals(timeVencedorId))
      .toList();

    if (candidatos.isEmpty()) {
      return timePerdedor.getId();
    }

    // Verifica se todos os times não-reserva já jogaram ao menos uma vez
    boolean todosJaJogarom = timesAtivos.stream().allMatch(t ->
      inscricaoRepository.findByTimeAtualAndEvento(t, evento).stream().mapToInt(Inscricao::getPartidasJogadas).sum() > 0
    );

    // Se nem todos jogaram, limitamos candidatos aos que jogaram menos (partidasJogadas == 0 preferencialmente)
    List<Time> candidatosFiltrados = candidatos;
    if (!todosJaJogarom) {
      List<Time> aindaNaoJogaram = candidatos.stream()
        .filter(t -> inscricaoRepository.findByTimeAtualAndEvento(t, evento).stream().mapToInt(Inscricao::getPartidasJogadas).sum() == 0)
        .toList();
      if (!aindaNaoJogaram.isEmpty()) {
        candidatosFiltrados = aindaNaoJogaram;
      }
    }

    // Escolher o time com menor soma de partidas jogadas (prioriza quem jogou menos)
    Time escolhido = candidatosFiltrados.stream()
      .min((t1, t2) -> {
        int s1 = inscricaoRepository.findByTimeAtualAndEvento(t1, evento).stream().mapToInt(Inscricao::getPartidasJogadas).sum();
        int s2 = inscricaoRepository.findByTimeAtualAndEvento(t2, evento).stream().mapToInt(Inscricao::getPartidasJogadas).sum();
        return Integer.compare(s1, s2);
      })
      .orElse(timePerdedor);

    // Se todos já jogaram e o banco ainda está vazio, o time reserva só entra quando todos os não-reserva tiverem jogado.
    // A lógica acima já garante que reserva não será escolhido entre 'timesAtivos' porque 'timeEspera' foi excluído.

    return escolhido.getId();
  }

  //lógica central do rodízio com tickets
  private void rodizioDeJogadores(Evento evento, Time timePerdedor) {
    //busca o time de espera do evento
    Time timeEspera = timeRepository
      .findByEventoAndTimeDeEsperaTrue(evento)
      .orElseThrow(() -> new TimeDeEsperaNaoConfiguradoException("Time de espera não configurado"));

    List<Inscricao> jogadoresNoBanco = inscricaoRepository.findByTimeAtualAndEvento(timeEspera, evento);
    List<Inscricao> jogadoresNoTimePerdedor = inscricaoRepository.findByTimeAtualAndEvento(timePerdedor, evento);

    if (jogadoresNoBanco.isEmpty()) {
      return; //ninguém para trocar, segue o jogo
    }

    int tamanhoDoTime = evento.getJogadoresPorTime();

    //QUEM ENTRA (Sai do Banco -> Vai pro Time Perdedor/Campo)
    //Regra de Ticket: Prioridade para quem jogou MENOS.
    //Desempate: Nivel de Habilidade (para equilibrar) ou Ordem de Chegada (ID).
    List<Inscricao> quemEntra = jogadoresNoBanco
      .stream()
      .sorted(Comparator
        .comparingInt(Inscricao::getPartidasJogadas) //menor número de partidas primeiro (Ticket)
        .thenComparingInt(i -> i.getJogador().getPontosHabilidade())) //desempate por elo
      .limit(tamanhoDoTime)
      .toList();

    //QUEM SAI (Sai do Time Perdedor → Vai para o Banco)
    //a regra diz "jogadores do time perdedor serão selecionados para compor time de reserva".
    //Normalmente sai o time inteiro, mas se o banco for menor que o time, fazemos troca parcial:
    //devemos trocar apenas a mesma quantidade de jogadores que entram do banco.
    int numEntrando = quemEntra.size();

    // Seleciona os jogadores do time perdedor que irão para o banco.
    // Prioriza enviar ao banco os jogadores que já jogaram mais (descendente), para descanso.
    List<Inscricao> quemSai = jogadoresNoTimePerdedor.stream()
      .sorted(Comparator.comparingInt(Inscricao::getPartidasJogadas).reversed())
      .limit(numEntrando)
      .toList();

    //executa a troca no banco de dados (quantidades correspondentes)
    for (Inscricao entrando : quemEntra) {
      entrando.setTimeAtual(timePerdedor); //entra em campo (no lugar do perdedor)
      inscricaoRepository.save(entrando);
    }

    for (Inscricao saindo : quemSai) {
      saindo.setTimeAtual(timeEspera); //vai para o banco
      inscricaoRepository.save(saindo);
    }

    //dependendo da regra exata, o time vencedor continua.
    //o time perdedor recebe os novos jogadores e vira o "Desafiante".
  }

  //metodo para atualizar pontuação individual chamado a cada gol/ação do tipo TipoAcaoEmJogo
  @Transactional
  public void computarAcaoJogador(Partida partida, Long jogadorId, TipoAcaoEmJogo tipoAcao) {
      User user = userRepository.findById(jogadorId).orElseThrow();

      int pontosGanhos = 0;
      switch (tipoAcao) {
          case GOL:
              pontosGanhos = 15;
              break;
          case ASSISTENCIA:
              pontosGanhos = 10;
              break;
          case DEFESA:
              pontosGanhos = 5;
              break;
          case FALTA:
              pontosGanhos = -15;
              break;
          case IMPEDIMENTO:
              pontosGanhos = -5;
              break;
      }

      historicoService.registrarAlteracao(user, partida, tipoAcao, pontosGanhos);

      // Verifica evolução de nível
      if (user.getPontosHabilidade() > 2400) user.setNivelHabilidade(NivelHabilidade.CRAQUE);
      else if (user.getPontosHabilidade() > 800) user.setNivelHabilidade(NivelHabilidade.MEDIANO);
      else user.setNivelHabilidade(NivelHabilidade.PERNA_DE_PAU);

      userRepository.save(user);
  }

  //métodos auxiliares
  private void alocarJogador(Inscricao inscricao, Time time) {
    inscricao.setTimeAtual(time);
    inscricaoRepository.save(inscricao);
  }

  private long contarJogadoresNoTime(Time time) {
    //countByTimeAtual no repository para performance
    return inscricaoRepository.countByTimeAtual(time);
  }

  private void incrementarPartidaJogada(Inscricao i) {
    i.setPartidasJogadas(i.getPartidasJogadas() + 1);
    inscricaoRepository.save(i);
  }

  public boolean jogadorEstaNoTime(Long eventoId, Long timeId, Long jogadorId) {
    return inscricaoRepository.findByEventoId(eventoId)
      .stream()
      .anyMatch(i ->
        i.getJogador().getId().equals(jogadorId) &&
          i.getTimeAtual() != null &&
          i.getTimeAtual().getId().equals(timeId)
      );
  }

  /**
   * Atualiza a pontuação dos jogadores de um time usando o sistema de ELO.
   *
   * Fórmulas utilizadas:
   * - Ea = 1.0 / (1 + Math.pow(10, (Rb - Ra) / 400.0))
   *   Probabilidade esperada de vitória
   * - R'a = Ra + K * (Sa − Ea)
   *   Nova pontuação após a partida
   *
   * @param partida A partida que foi finalizada
   * @param timeDeste O time do qual atualizar jogadores
   * @param timeAdversario O time adversário
   * @param venceu true se timeDeste venceu, false se perdeu
   */
  @Transactional
  protected void atualizarPontuacaoElo(Partida partida, Time timeDeste, Time timeAdversario, boolean venceu) {
    // Buscar todos os jogadores do time
    List<Inscricao> jogadoresTime = inscricaoRepository.findByTimeAtualAndEvento(timeDeste, partida.getEvento());

    if (jogadoresTime.isEmpty()) {
      return;
    }

    // Calcular a média de pontos do time adversário
    double mediaAdversario = calcularMediaPontosTIme(timeAdversario, partida.getEvento());

    // Para cada jogador do time, calcular nova pontuação usando ELO
    for (Inscricao inscricao : jogadoresTime) {
      User jogador = inscricao.getJogador();
      double pontoAtual = jogador.getPontosHabilidade();

      // Calcular a variação de pontos usando as fórmulas de ELO
      int variacao = EloCalculator.calcularVariacao(pontoAtual, mediaAdversario, venceu);

      // Determinar o tipo de ação para registrar no histórico
      TipoAcaoEmJogo tipoAcao = venceu ? TipoAcaoEmJogo.GOL : TipoAcaoEmJogo.IMPEDIMENTO;

      // Registrar a alteração no histórico
      historicoService.registrarAlteracao(jogador, partida, tipoAcao, variacao);

      // Atualizar o nível de habilidade baseado na nova pontuação
      atualizarNivelHabilidade(jogador);

      // Salvar o jogador
      userRepository.save(jogador);
    }
  }

  /**
   * Calcula a média de pontos de habilidade de todos os jogadores de um time.
   *
   * @param time O time
   * @param evento O evento
   * @return A média simples dos pontos de habilidade
   */
  private double calcularMediaPontosTIme(Time time, Evento evento) {
    List<Inscricao> jogadoresTime = inscricaoRepository.findByTimeAtualAndEvento(time, evento);

    if (jogadoresTime.isEmpty()) {
      // Retorna 1000 (pontuação padrão) se não houver jogadores
      return 1000.0;
    }

    double soma = jogadoresTime.stream()
      .mapToDouble(i -> i.getJogador().getPontosHabilidade())
      .sum();

    return soma / jogadoresTime.size();
  }

  /**
   * Atualiza o nível de habilidade do jogador com base na pontuação atual.
   *
   * @param jogador O jogador a atualizar
   */
  private void atualizarNivelHabilidade(User jogador) {
    if (jogador.getPontosHabilidade() > 2400) {
      jogador.setNivelHabilidade(NivelHabilidade.CRAQUE);
    } else if (jogador.getPontosHabilidade() > 800) {
      jogador.setNivelHabilidade(NivelHabilidade.MEDIANO);
    } else {
      jogador.setNivelHabilidade(NivelHabilidade.PERNA_DE_PAU);
    }
  }
}