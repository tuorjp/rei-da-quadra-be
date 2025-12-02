package rei_da_quadra_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rei_da_quadra_be.model.Inscricao;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscricaoResponseDTO {
    
    private Long id;
    private Long jogadorId;
    private String jogadorNome;
    private String jogadorEmail;
    private Integer partidasJogadas;
    private Long timeAtualId;
    private String timeAtualNome;
    private LocalDateTime dataInscricao;
    
    public static InscricaoResponseDTO fromEntity(Inscricao inscricao) {
        return InscricaoResponseDTO.builder()
            .id(inscricao.getId())
            .jogadorId(inscricao.getJogador().getId())
            .jogadorNome(inscricao.getJogador().getNome())
            .jogadorEmail(inscricao.getJogador().getEmail())
            .partidasJogadas(inscricao.getPartidasJogadas())
            .timeAtualId(inscricao.getTimeAtual() != null ? inscricao.getTimeAtual().getId() : null)
            .timeAtualNome(inscricao.getTimeAtual() != null ? inscricao.getTimeAtual().getNome() : null)
            .dataInscricao(inscricao.getDataInscricao())
            .build();
    }
}
