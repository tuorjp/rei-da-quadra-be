package rei_da_quadra_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HistoricoPontuacaoDTO {
    private String data;
    private String acao;
    private Integer variacao;
    private Integer pontosFinais;
    private String nomePartida;
}
