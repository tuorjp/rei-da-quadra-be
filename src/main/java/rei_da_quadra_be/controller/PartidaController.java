package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rei_da_quadra_be.dto.AcaoJogoDTO;
import rei_da_quadra_be.dto.PartidaCreateDTO;
import rei_da_quadra_be.dto.PartidaResponseDTO;
import rei_da_quadra_be.model.Partida;
import rei_da_quadra_be.service.PartidaService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Partidas", description = "Controle de partidas, placar e ações de jogo")
public class PartidaController {

  private final PartidaService partidaService;

  @Operation(summary = "Lista todas as partidas de um evento")
  @GetMapping("/eventos/{eventoId}/partidas")
  public ResponseEntity<List<PartidaResponseDTO>> listarPorEvento(@PathVariable Long eventoId) {
    List<Partida> partidas = partidaService.listarPartidasDoEvento(eventoId);
    List<PartidaResponseDTO> dtos = partidas.stream().map(this::toResponseDTO).toList();
    return ResponseEntity.ok(dtos);
  }

  @Operation(summary = "Cria uma nova partida manualmente")
  @PostMapping("/eventos/{eventoId}/partidas")
  public ResponseEntity<PartidaResponseDTO> criarPartida(@PathVariable Long eventoId, @RequestBody @Valid PartidaCreateDTO dto) {
    Partida partida = partidaService.criarPartida(eventoId, dto.getTimeAId(), dto.getTimeBId());
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(partida));
  }

  @Operation(summary = "Inicia uma partida")
  @PostMapping("/partidas/{id}/iniciar")
  public ResponseEntity<PartidaResponseDTO> iniciarPartida(@PathVariable Long id) {
    Partida partida = partidaService.iniciarPartida(id);
    return ResponseEntity.ok(toResponseDTO(partida));
  }

  @Operation(summary = "Registra uma ação (Gol, Assistência, Defesa)")
  @PostMapping("/partidas/{id}/acoes")
  public ResponseEntity<Void> registrarAcao(@PathVariable Long id, @RequestBody @Valid AcaoJogoDTO dto) {
    partidaService.registrarAcao(id, dto.getJogadorId(), dto.getTipoAcao());
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Finaliza a partida e executa o rodízio de times")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Partida finalizada e rodízio aplicado"),
    @ApiResponse(responseCode = "400", description = "Partida já finalizada ou inválida", content = @Content)
  })
  @PostMapping("/partidas/{id}/finalizar")
  public ResponseEntity<PartidaResponseDTO> finalizarPartida(@PathVariable Long id) {
    Partida partida = partidaService.finalizarPartida(id);
    return ResponseEntity.ok(toResponseDTO(partida));
  }

  private PartidaResponseDTO toResponseDTO(Partida p) {
    PartidaResponseDTO dto = new PartidaResponseDTO();
    dto.setId(p.getId());
    dto.setEventoId(p.getEvento().getId());
    dto.setDataPartida(p.getDataPartida());
    dto.setStatus(p.getStatus().toString());
    dto.setTimeANome(p.getTimeA().getNome());
    dto.setTimeAPlacar(p.getTimeAPlacar());
    dto.setTimeBNome(p.getTimeB().getNome());
    dto.setTimeBPlacar(p.getTimeBPlacar());
    return dto;
  }
}