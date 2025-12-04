package rei_da_quadra_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rei_da_quadra_be.dto.AcaoJogoDTO;
import rei_da_quadra_be.dto.HistoricoPontuacaoDTO;
import rei_da_quadra_be.dto.PontuacaoManualDTO;
import rei_da_quadra_be.model.Partida;
import rei_da_quadra_be.repository.PartidaRepository;
import rei_da_quadra_be.service.AdmTimesService;
import rei_da_quadra_be.service.HistoricoPontuacaoService;

import java.util.List;

@RestController
@RequestMapping("/pontuacao")
@RequiredArgsConstructor
public class HistoricoPontuacaoController {

    private final HistoricoPontuacaoService historicoService;
    private final PartidaRepository partidaRepository;
    private final AdmTimesService admTimesService;

    @PostMapping("/acao/{partidaId}")
    public ResponseEntity<Void> registrarAcao(
            @PathVariable Long partidaId,
            @RequestBody AcaoJogoDTO dto
    ) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

        // Chama o serviço central que calcula pontos, nível e salva histórico
        admTimesService.computarAcaoJogador(partida, dto.getJogadorId(), dto.getTipoAcao());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/manual/{partidaId}/{jogadorId}")
    public ResponseEntity<Void> registrarManual(
            @PathVariable Long partidaId,
            @PathVariable Long jogadorId,
            @RequestBody PontuacaoManualDTO dto
    ) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

        // Chama o serviço central que calcula pontos, nível e salva histórico
        admTimesService.computarAcaoJogador(partida, jogadorId, dto.getAcao());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/historico/{jogadorId}")
    public ResponseEntity<List<HistoricoPontuacaoDTO>> historico(@PathVariable Long jogadorId) {
        List<HistoricoPontuacaoDTO> historico = historicoService.listarExtrato(jogadorId);
        return ResponseEntity.ok(historico);
    }
}