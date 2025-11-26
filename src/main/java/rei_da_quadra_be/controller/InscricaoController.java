package rei_da_quadra_be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rei_da_quadra_be.dto.InscricaoRequestDTO;
import rei_da_quadra_be.dto.InscricaoResponseDTO;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.service.InscricaoService;

import java.util.List;

@RestController
@RequestMapping("/eventos/{eventoId}/inscricoes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-key")
public class InscricaoController {
    
    private final InscricaoService inscricaoService;
    
    @GetMapping
    @Operation(summary = "Lista todas as inscrições de um evento")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de inscrições retornada com sucesso", content = {
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = InscricaoResponseDTO.class))
            )
        }),
        @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Sem permissão para acessar este evento", content = @Content)
    })
    public ResponseEntity<List<InscricaoResponseDTO>> listarInscricoes(
        @PathVariable Long eventoId,
        @AuthenticationPrincipal User user
    ) {
        List<InscricaoResponseDTO> inscricoes = inscricaoService.listarInscricoes(eventoId, user);
        return ResponseEntity.ok(inscricoes);
    }
    
    @PostMapping
    @Operation(summary = "Adiciona um jogador ao evento")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Jogador adicionado com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = InscricaoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
        @ApiResponse(responseCode = "403", description = "Apenas o organizador pode adicionar jogadores", content = @Content),
        @ApiResponse(responseCode = "404", description = "Evento ou jogador não encontrado", content = @Content)
    })
    public ResponseEntity<InscricaoResponseDTO> adicionarInscricao(
        @PathVariable Long eventoId,
        @RequestBody @Valid InscricaoRequestDTO request,
        @AuthenticationPrincipal User user
    ) {
        InscricaoResponseDTO inscricao = inscricaoService.adicionarInscricao(eventoId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricao);
    }
    
    @DeleteMapping("/{inscricaoId}")
    @Operation(summary = "Remove um jogador do evento")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Jogador removido com sucesso"),
        @ApiResponse(responseCode = "403", description = "Apenas o organizador pode remover jogadores", content = @Content),
        @ApiResponse(responseCode = "404", description = "Evento ou inscrição não encontrada", content = @Content)
    })
    public ResponseEntity<Void> removerInscricao(
        @PathVariable Long eventoId,
        @PathVariable Long inscricaoId,
        @AuthenticationPrincipal User user
    ) {
        inscricaoService.removerInscricao(eventoId, inscricaoId, user);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{inscricaoId}")
    @Operation(summary = "Busca uma inscrição específica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inscrição encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = InscricaoResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Inscrição não encontrada", content = @Content),
        @ApiResponse(responseCode = "403", description = "Sem permissão para acessar este evento", content = @Content)
    })
    public ResponseEntity<InscricaoResponseDTO> buscarInscricao(
        @PathVariable Long eventoId,
        @PathVariable Long inscricaoId,
        @AuthenticationPrincipal User user
    ) {
        InscricaoResponseDTO inscricao = inscricaoService.buscarInscricao(eventoId, inscricaoId, user);
        return ResponseEntity.ok(inscricao);
    }
}
