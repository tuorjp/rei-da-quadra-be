package rei_da_quadra_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rei_da_quadra_be.dto.HistoricoPontuacaoDTO;
import rei_da_quadra_be.enums.TipoAcaoEmJogo;
import rei_da_quadra_be.model.HistoricoPontuacao;
import rei_da_quadra_be.model.Partida;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.repository.HistoricoPontuacaoRepository;
import rei_da_quadra_be.repository.UserRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricoPontuacaoService {

    private final HistoricoPontuacaoRepository historicoRepository;
    private final UserRepository userRepository;

    // -------------------------------------
    // REGISTRO DE ALTERAÇÃO DE PONTOS
    // -------------------------------------
    @Transactional
    public void registrarAlteracao(
            User jogador,
            Partida partida,
            TipoAcaoEmJogo acao,
            int variacao
    ) {
        int antes = jogador.getPontosHabilidade();
        int depois = antes + variacao;

        jogador.setPontosHabilidade(depois);
        userRepository.save(jogador);

        HistoricoPontuacao h = new HistoricoPontuacao();
        h.setJogador(jogador);
        h.setPartida(partida);
        h.setAcao(acao);
        h.setVariacao(variacao);
        h.setPontosAntes(antes);
        h.setPontosDepois(depois);
        h.setDataRegistro(java.time.LocalDateTime.now());

        historicoRepository.save(h);
    }

    // -------------------------------------
    // LISTAGEM DO EXTRATO PARA O ANGULAR
    // -------------------------------------
    public List<HistoricoPontuacaoDTO> listarExtrato(Long jogadorId) {

        List<HistoricoPontuacao> lista =
                historicoRepository.findByJogadorIdOrderByDataRegistroDesc(jogadorId);

        DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE_TIME;

        return lista.stream().map(h -> {

            String nomeJogo = (h.getPartida() != null && h.getPartida().getEvento() != null)
                    ? h.getPartida().getEvento().getNome()
                    : null;

            return new HistoricoPontuacaoDTO(
                    h.getDataRegistro().format(fmt),
                    h.getAcao().name(),
                    h.getVariacao(),
                    h.getPontosDepois(),
                    nomeJogo
            );
        }).toList();
    }
}
