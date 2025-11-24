package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rei_da_quadra_be.dto.JogadorResumoDTO;
import rei_da_quadra_be.dto.PontuacaoManualDTO;
import rei_da_quadra_be.dto.TimeComJogadoresDTO;
import rei_da_quadra_be.model.Inscricao;
import rei_da_quadra_be.model.Time;
import rei_da_quadra_be.repository.InscricaoRepository;
import rei_da_quadra_be.repository.TimeRepository;
import rei_da_quadra_be.service.AdmTimesService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/times")
@RequiredArgsConstructor
@Tag(name = "Administração de Times", description = "Gerenciamento de distribuição, visualização e ajustes de times")
public class AdmTimesController {

  private final AdmTimesService admTimesService;
  private final TimeRepository timeRepository;
  private final InscricaoRepository inscricaoRepository;

  @Operation(summary = "Distribui os jogadores e cria os times iniciais")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Times criados e balanceados com sucesso"),
    @ApiResponse(responseCode = "400", description = "Número insuficiente de inscritos")
  })
  @PostMapping("/evento/{eventoId}/distribuir")
  public ResponseEntity<Void> distribuirTimes(@PathVariable Long eventoId) {
    admTimesService.montarTimesInicial(eventoId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Retorna a estrutura completa de times e seus jogadores atuais")
  @GetMapping("/evento/{eventoId}/detalhado")
  public ResponseEntity<List<TimeComJogadoresDTO>> listarTimesDetalhados(@PathVariable Long eventoId) {
    List<Time> times = timeRepository.findByEventoId(eventoId);
    List<TimeComJogadoresDTO> response = new ArrayList<>();

    for (Time time : times) {
      TimeComJogadoresDTO dto = new TimeComJogadoresDTO();
      dto.setTimeId(time.getId());
      dto.setNome(time.getNome());
      dto.setStatus(time.getStatus());
      dto.setIsTimeDeEspera(time.getTimeDeEspera());

      List<Inscricao> inscricoesDoTime = inscricaoRepository.findByTimeAtualAndEvento(time, time.getEvento());
      List<JogadorResumoDTO> jogadoresDto = inscricoesDoTime.stream().map(i -> {
        JogadorResumoDTO j = new JogadorResumoDTO();
        j.setId(i.getJogador().getId());
        j.setNome(i.getJogador().getNome());
        j.setPontosHabilidade(i.getJogador().getPontosHabilidade());
        j.setNivelHabilidade(i.getJogador().getNivelHabilidade());
        j.setPartidasJogadas(i.getPartidasJogadas());
        return j;
      }).toList();

      dto.setJogadores(jogadoresDto);
      response.add(dto);
    }

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Ajuste Manual: Aplica pontuação a um jogador fora do fluxo de partida")
  @PostMapping("/jogador/{jogadorId}/pontuar")
  public ResponseEntity<Void> pontuarManualmente(@PathVariable Long jogadorId, @RequestBody PontuacaoManualDTO dto) {
    admTimesService.computarAcaoJogador(jogadorId, dto.getAcao());
    return ResponseEntity.ok().build();
  }
}