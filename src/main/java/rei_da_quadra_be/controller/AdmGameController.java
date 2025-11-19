package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rei_da_quadra_be.model.Partida;
import rei_da_quadra_be.model.Time;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.model.Inscricao;
import rei_da_quadra_be.repository.PartidaRepository;
import rei_da_quadra_be.repository.TimeRepository;
import rei_da_quadra_be.repository.EventoRepository;
import rei_da_quadra_be.repository.UserRepository;
import rei_da_quadra_be.repository.InscricaoRepository;
import rei_da_quadra_be.service.AdmTimesService;

import java.util.List;

@RestController
@RequestMapping("/adm-game")
@RequiredArgsConstructor
public class AdmGameController {

  private final AdmTimesService admTimesService;
  private final PartidaRepository partidaRepository;
  private final TimeRepository timeRepository;
  private final EventoRepository eventoRepository;
  private final UserRepository userRepository;
  private final InscricaoRepository inscricaoRepository;

  // --- 1. Inscrição de Jogadores (Para facilitar o teste) ---
  @PostMapping("/inscrever")
  @Operation(summary = "Inscreve um usuário em um evento")
  public ResponseEntity<String> inscreverUsuario(@RequestBody InscricaoDTO dto) {
    Evento evento = eventoRepository.findById(dto.eventoId()).orElseThrow();
    User user = userRepository.findById(dto.userId()).orElseThrow();

    Inscricao inscricao = new Inscricao();
    inscricao.setEvento(evento);
    inscricao.setJogador(user);
    inscricao.setPartidasJogadas(0);
    inscricaoRepository.save(inscricao);

    return ResponseEntity.ok("Usuário " + user.getNome() + " inscrito no evento " + evento.getNome());
  }

  // --- 2. Distribuição Inicial de Times ---
  @PostMapping("/evento/{eventoId}/distribuir-times")
  @Operation(summary = "Distribui os inscritos nos times baseados em Elo e Regras")
  public ResponseEntity<String> distribuirTimes(@PathVariable Long eventoId) {
    try {
      admTimesService.montarTimesInicial(eventoId);
      return ResponseEntity.ok("Times montados e jogadores distribuídos com sucesso!");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Erro ao montar times: " + e.getMessage());
    }
  }

  // --- 3. Criar Partida ---
  @PostMapping("/partida")
  @Operation(summary = "Cria uma nova partida entre dois times")
  public ResponseEntity<Partida> criarPartida(@RequestBody CriarPartidaDTO dto) {
    Evento evento = eventoRepository.findById(dto.eventoId()).orElseThrow();
    Time timeA = timeRepository.findById(dto.timeAId()).orElseThrow();
    Time timeB = timeRepository.findById(dto.timeBId()).orElseThrow();

    Partida partida = new Partida();
    partida.setEvento(evento);
    partida.setTimeA(timeA);
    partida.setTimeB(timeB);
    partida.setTimeAPlacar(0);
    partida.setTimeBPlacar(0);
    partida.setStatus("jogada"); // ou "em_andamento"

    Partida novaPartida = partidaRepository.save(partida);
    return ResponseEntity.ok(novaPartida);
  }

  // --- 4. Finalizar Partida e Redistribuir (Rodízio) ---
  @PostMapping("/partida/{partidaId}/finalizar")
  @Operation(summary = "Salva o placar e executa o rodízio de jogadores")
  public ResponseEntity<String> finalizarPartida(@PathVariable Long partidaId, @RequestBody FinalizarPartidaDTO dto) {
    Partida partida = partidaRepository.findById(partidaId)
      .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

    // Atualiza Placar
    partida.setTimeAPlacar(dto.placarA());
    partida.setTimeBPlacar(dto.placarB());
    partida.setStatus("concluido");
    partidaRepository.save(partida);

    // Chama o serviço para processar lógica de Elo e Rodízio
    admTimesService.processarFimDePartida(partidaId, dto.timeVencedorId());

    return ResponseEntity.ok("Partida finalizada e rodízio realizado.");
  }
}

// DTOs auxiliares (Podem ser arquivos separados)
record InscricaoDTO(Long userId, Long eventoId) {}
record CriarPartidaDTO(Long eventoId, Long timeAId, Long timeBId) {}
record FinalizarPartidaDTO(Integer placarA, Integer placarB, Long timeVencedorId) {}