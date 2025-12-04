package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rei_da_quadra_be.dto.JogadorDTO;
import rei_da_quadra_be.dto.TimeResponseDTO;
import rei_da_quadra_be.dto.TimeUpdateDTO;
import rei_da_quadra_be.model.Time;
import rei_da_quadra_be.service.TimeService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Times", description = "Visualização e edição de times")
public class TimeController {

  private final TimeService timeService;

  @Operation(summary = "Lista todos os times de um evento")
  @GetMapping(value = "/eventos/{eventoId}/times", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<TimeResponseDTO>> listarPorEvento(@PathVariable Long eventoId) {
    List<Time> times = timeService.listarTimesDoEvento(eventoId);
    List<TimeResponseDTO> dtos = times
      .stream()
      .map(this::toResponseDTO)
      .toList();

    return ResponseEntity.ok(dtos);
  }

  @Operation(summary = "Busca detalhes de um time")
  @GetMapping(value = "/times/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TimeResponseDTO> buscarPorId(@PathVariable Long id) {
    Time time = timeService.buscarPorId(id);
    return ResponseEntity.ok(toResponseDTO(time));
  }

  @Operation(summary = "Atualiza nome ou cor do time")
  @PatchMapping(value = "/times/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TimeResponseDTO> atualizar(@PathVariable Long id, @RequestBody TimeUpdateDTO dto) {
    Time time = timeService.atualizarTime(id, dto.getNome(), dto.getCor());
    return ResponseEntity.ok(toResponseDTO(time));
  }

  private TimeResponseDTO toResponseDTO(Time time) {
    TimeResponseDTO dto = new TimeResponseDTO();
    dto.setId(time.getId());
    dto.setNome(time.getNome());
    dto.setCor(time.getCor());
    dto.setTimeDeEspera(time.getTimeDeEspera());
    dto.setStatus(time.getStatus().toString());

    if (time.getInscricoes() != null) {
      List<JogadorDTO> jogadoresDto = time.getInscricoes().stream()
        .map(inscricao -> new JogadorDTO(
          inscricao.getJogador().getId(),
          inscricao.getEvento().getId(),
          time.getId(),
          inscricao.getJogador().getNome(),
          inscricao.getJogador().getNivelHabilidade(),
          inscricao.getJogador().getFotoPerfil()
        ))
        .toList();

      dto.setJogadores(jogadoresDto);
    }

    return dto;
  }
}