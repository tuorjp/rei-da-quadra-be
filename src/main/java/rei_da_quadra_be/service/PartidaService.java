package rei_da_quadra_be.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rei_da_quadra_be.enums.StatusPartida;
import rei_da_quadra_be.enums.StatusTime;
import rei_da_quadra_be.enums.TipoAcaoEmJogo;
import rei_da_quadra_be.model.*;
import rei_da_quadra_be.repository.ParticipacaoDesempenhoRepository;
import rei_da_quadra_be.repository.PartidaRepository;
import rei_da_quadra_be.service.exception.PartidaNaoEncontradaException;
import rei_da_quadra_be.service.exception.RegraDeNegocioException;

import java.util.List;

/*
* Essa classe junto a AdmTimesService são o coração da lógica e do projeto
* CUIDADO ao alterar qualquer coisa nelas
* Atenciosamente.
*/

@Service
@RequiredArgsConstructor
public class PartidaService {
  private final PartidaRepository partidaRepository;
  private final ParticipacaoDesempenhoRepository participacaoRepository;
  private final TimeService timeService;
  private final EventoService eventoService;
  private final AdmTimesService admTimesService;

  public List<Partida> listarPartidasDoEvento(Long eventoId) {
    return partidaRepository.findByEventoId(eventoId);
  }

  //cria uma nova partida manualmente entre dois times definidos
  @Transactional
  public Partida criarPartida(Long eventoId, Long timeAId, Long timeBId) {
    Evento evento = eventoService.buscarEventoPorId(eventoId)
      .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

    Time timeA = timeService.buscarPorId(timeAId);
    Time timeB = timeService.buscarPorId(timeBId);

    if (timeA.getStatus() == StatusTime.INOPERANTE || timeB.getStatus() == StatusTime.INOPERANTE) {
      throw new RegraDeNegocioException("Não é possível criar partida com times inoperantes.");
    }

    Partida partida = new Partida();
    partida.setEvento(evento);
    partida.setTimeA(timeA);
    partida.setTimeB(timeB);
    partida.setTimeAPlacar(0);
    partida.setTimeBPlacar(0);
    partida.setStatus(StatusPartida.AGUARDANDO_INICIO);

    return partidaRepository.save(partida);
  }

  //inicia a partida, muda status para EM_ANDAMENTO
  @Transactional
  public Partida iniciarPartida(Long partidaId) {
    Partida partida = buscarPorId(partidaId);
    partida.setStatus(StatusPartida.EM_ANDAMENTO);
    return partidaRepository.save(partida);
  }

  //registra um gol, assistência ou defesa, atualiza o placar e computa o elo do jogador
  @Transactional
  public void registrarAcao(Long partidaId, Long jogadorId, TipoAcaoEmJogo acao) {
    Partida partida = buscarPorId(partidaId);

    if (partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
      throw new RegraDeNegocioException("Só é possível registrar ações em partidas em andamento.");
    }

    ParticipacaoDesempenho desempenho = obterOuCriarParticipacao(partida, jogadorId);

    switch (acao) {
      case GOL:
        desempenho.setGols(desempenho.getGols() + 1);
        atualizarPlacarPartida(partida, desempenho.getTimeNaPartida());
        break;
      case ASSISTENCIA:
        desempenho.setPasses(desempenho.getPasses() + 1);
        break;
      case DEFESA:
        desempenho.setDefesas(desempenho.getDefesas() + 1);
        break;
      default:
        break;
    }

    participacaoRepository.save(desempenho);

    admTimesService.computarAcaoJogador(partida, jogadorId, acao);
  }

  //finaliza a partida, define o vencedor e chama o serviço de rodízio de times
  @Transactional
  public Partida finalizarPartida(Long partidaId) {
    Partida partida = buscarPorId(partidaId);

    if (partida.getStatus() == StatusPartida.JOGADA) {
      throw new RegraDeNegocioException("Esta partida já foi finalizada.");
    }

    partida.setStatus(StatusPartida.JOGADA);
    partida = partidaRepository.save(partida);

    Long idVencedor;
    if (partida.getTimeAPlacar() > partida.getTimeBPlacar()) {
      idVencedor = partida.getTimeA().getId();
    } else if (partida.getTimeBPlacar() > partida.getTimeAPlacar()) {
      idVencedor = partida.getTimeB().getId();
    } else {
      // Regra de empate: No Rei da Quadra não pode haver empate.
      // Assume-se Time A (Rei) como vencedor ou lança erro. Aqui mantemos Time A.
      idVencedor = partida.getTimeA().getId();
    }

    admTimesService.processarFimDePartida(partida.getId(), idVencedor);

    return partida;
  }

  /* AUXILIARES */

  public Partida buscarPorId(Long id) {
    return partidaRepository.findById(id)
      .orElseThrow(() -> new PartidaNaoEncontradaException("Partida não encontrada com id: " + id));
  }

  //método auxiliar interno para gerenciar a tabela de desempenho
  private ParticipacaoDesempenho obterOuCriarParticipacao(Partida partida, Long jogadorId) {
    return participacaoRepository.findByPartidaIdAndJogadorId(partida.getId(), jogadorId)
      .orElseGet(() -> {
        ParticipacaoDesempenho nova = new ParticipacaoDesempenho();
        nova.setPartida(partida);

        User userStub = new User();
        userStub.setId(jogadorId);
        nova.setJogador(userStub);

        //descobre qual time o jogador defende nesta partida
        nova.setTimeNaPartida(descobrirTimeDoJogadorNaPartida(partida, jogadorId));

        return nova;
      });
  }

  //auxiliar para descobrir se o jogador é do Time A ou B
  private Time descobrirTimeDoJogadorNaPartida(Partida partida, Long jogadorId) {
    boolean noTimeA = admTimesService.jogadorEstaNoTime(partida.getEvento().getId(), partida.getTimeA().getId(), jogadorId);
    if (noTimeA) return partida.getTimeA();

    boolean noTimeB = admTimesService.jogadorEstaNoTime(partida.getEvento().getId(), partida.getTimeB().getId(), jogadorId);
    if (noTimeB) return partida.getTimeB();

    throw new RegraDeNegocioException("Jogador não pertence a nenhum dos times desta partida.");
  }

  private void atualizarPlacarPartida(Partida partida, Time timeQueMarcou) {
    if (partida.getTimeA().getId().equals(timeQueMarcou.getId())) {
      partida.setTimeAPlacar(partida.getTimeAPlacar() + 1);
    } else {
      partida.setTimeBPlacar(partida.getTimeBPlacar() + 1);
    }
    partidaRepository.save(partida);
  }
}
