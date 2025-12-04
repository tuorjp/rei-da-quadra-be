package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rei_da_quadra_be.dto.JogadorResumoDTO;
import rei_da_quadra_be.dto.PontuacaoManualDTO;
import rei_da_quadra_be.dto.TimeComJogadoresDTO;
import rei_da_quadra_be.model.Inscricao;
import rei_da_quadra_be.model.Partida;
import rei_da_quadra_be.model.Time;
import rei_da_quadra_be.repository.InscricaoRepository;
import rei_da_quadra_be.repository.PartidaRepository;
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
    private final PartidaRepository partidaRepository; // Adicionado repository

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

    @Operation(summary = "Ajuste Manual: Aplica pontuação a um jogador vinculando a uma partida")
    // Rota alterada para exigir o ID da partida
    @PostMapping("/partida/{partidaId}/jogador/{jogadorId}/pontuar")
    public ResponseEntity<Void> pontuarManualmente(
            @PathVariable Long partidaId,
            @PathVariable Long jogadorId,
            @RequestBody PontuacaoManualDTO dto
    ) {
        // 1. Busca a partida (Necessário para o histórico)
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RuntimeException("Partida não encontrada para registrar a pontuação manual."));

        // 2. Chama o serviço passando a partida
        admTimesService.computarAcaoJogador(partida, jogadorId, dto.getAcao());

        return ResponseEntity.ok().build();
    }
}