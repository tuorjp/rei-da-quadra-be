package rei_da_quadra_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rei_da_quadra_be.dto.InscricaoRequestDTO;
import rei_da_quadra_be.dto.InscricaoResponseDTO;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.Inscricao;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.repository.EventoRepository;
import rei_da_quadra_be.repository.InscricaoRepository;
import rei_da_quadra_be.repository.UserRepository;
import rei_da_quadra_be.service.exception.EventoNaoEncontradoException;
import rei_da_quadra_be.service.exception.RegraDeNegocioException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InscricaoService {
    
    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;
    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public List<InscricaoResponseDTO> listarInscricoes(Long eventoId, User currentUser) {
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));
        
        validarPermissaoLeitura(evento, currentUser);
        
        return inscricaoRepository.findByEventoId(eventoId).stream()
            .map(InscricaoResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public InscricaoResponseDTO adicionarInscricao(Long eventoId, InscricaoRequestDTO request, User currentUser) {
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));
        
        if (!evento.getUsuario().getId().equals(currentUser.getId())) {
            throw new RegraDeNegocioException("Apenas o organizador pode adicionar jogadores");
        }
        
        User jogador;
        if (request.getJogadorId() != null) {
            jogador = userRepository.findById(request.getJogadorId())
                .orElseThrow(() -> new RegraDeNegocioException("Jogador não encontrado"));
        } else {
            UserDetails userDetails = userRepository.findByEmail(request.getJogadorEmail());
            if (userDetails == null) {
                throw new RegraDeNegocioException("Jogador não encontrado com este email");
            }
            jogador = (User) userDetails;
        }
        
        if (inscricaoRepository.existsByEventoIdAndJogadorId(eventoId, jogador.getId())) {
            throw new RegraDeNegocioException("Jogador já está inscrito neste evento");
        }
        
        Inscricao inscricao = new Inscricao();
        inscricao.setEvento(evento);
        inscricao.setJogador(jogador);
        inscricao.setPartidasJogadas(0);
        inscricao.setDataInscricao(LocalDateTime.now());
        
        inscricao = inscricaoRepository.save(inscricao);
        
        return InscricaoResponseDTO.fromEntity(inscricao);
    }
    
    @Transactional
    public void removerInscricao(Long eventoId, Long inscricaoId, User currentUser) {
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));
        
        if (!evento.getUsuario().getId().equals(currentUser.getId())) {
            throw new RegraDeNegocioException("Apenas o organizador pode remover jogadores");
        }
        
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
            .orElseThrow(() -> new RegraDeNegocioException("Inscrição não encontrada"));
        
        if (!inscricao.getEvento().getId().equals(eventoId)) {
            throw new RegraDeNegocioException("Inscrição não pertence a este evento");
        }
        
        inscricaoRepository.delete(inscricao);
    }
    
    @Transactional(readOnly = true)
    public InscricaoResponseDTO buscarInscricao(Long eventoId, Long inscricaoId, User currentUser) {
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));
        
        validarPermissaoLeitura(evento, currentUser);
        
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
            .orElseThrow(() -> new RegraDeNegocioException("Inscrição não encontrada"));
        
        if (!inscricao.getEvento().getId().equals(eventoId)) {
            throw new RegraDeNegocioException("Inscrição não pertence a este evento");
        }
        
        return InscricaoResponseDTO.fromEntity(inscricao);
    }
    
    private void validarPermissaoLeitura(Evento evento, User currentUser) {
        boolean isOrganizador = evento.getUsuario().getId().equals(currentUser.getId());
        boolean isParticipante = inscricaoRepository.existsByEventoIdAndJogadorId(
            evento.getId(), 
            currentUser.getId()
        );
        
        if (!isOrganizador && !isParticipante) {
            throw new RegraDeNegocioException("Você não tem permissão para acessar este evento");
        }
    }
}
