package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rei_da_quadra_be.dto.EventoRequestDTO;
import rei_da_quadra_be.dto.EventoResponseDTO;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.service.EventoService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-key")
public class EventoController {
  private final EventoService eventoService;

  @PostMapping
  @Operation(summary = "Cria um novo evento")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Evento criado com sucesso",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventoResponseDTO.class))),
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
  })
  public ResponseEntity<EventoResponseDTO> criarEvento(
    @RequestBody EventoRequestDTO eventoRequest,
    @AuthenticationPrincipal User usuario
  ) {
    Evento evento = eventoRequest.toEvento();
    Evento eventoSalvo = eventoService.salvarEvento(evento, usuario);
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(EventoResponseDTO.fromEvento(eventoSalvo));
  }

  @GetMapping
  @Operation(summary = "Lista todos os eventos do usuário autenticado")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Lista de eventos retornada com sucesso")
  })
  public ResponseEntity<List<EventoResponseDTO>> listarEventos(
    @AuthenticationPrincipal User usuario
  ) {
    List<Evento> eventos = eventoService.listarEventosDoUsuario(usuario);
    List<EventoResponseDTO> eventosDTO = eventos.stream()
      .map(EventoResponseDTO::fromEvento)
      .collect(Collectors.toList());
    return ResponseEntity.ok(eventosDTO);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Busca um evento específico por ID")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Evento encontrado",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventoResponseDTO.class))),
    @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content)
  })
  public ResponseEntity<EventoResponseDTO> buscarEvento(
    @PathVariable Long id,
    @AuthenticationPrincipal User usuario
  ) {
    return eventoService.buscarEventoPorId(id)
      .filter(evento -> evento.getUsuario().getId().equals(usuario.getId()))
      .map(evento -> ResponseEntity.ok(EventoResponseDTO.fromEvento(evento)))
      .orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Atualiza parcialmente um evento (programação reflexiva)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Evento atualizado com sucesso",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventoResponseDTO.class))),
    @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content),
    @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
  })
  public ResponseEntity<EventoResponseDTO> atualizarEventoParcial(
    @PathVariable Long id,
    @RequestBody Map<String, Object> fields,
    @AuthenticationPrincipal User usuario
  ) {
    try {
      Evento eventoAtualizado = eventoService.atualizaEventoParcial(id, fields, usuario);
      return ResponseEntity.ok(EventoResponseDTO.fromEvento(eventoAtualizado));
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Deleta um evento")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "Evento deletado com sucesso"),
    @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content),
    @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
  })
  public ResponseEntity<Void> deletarEvento(
    @PathVariable Long id,
    @AuthenticationPrincipal User usuario
  ) {
    try {
      eventoService.deletarEvento(id, usuario);
      return ResponseEntity.noContent().build();
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }
}

