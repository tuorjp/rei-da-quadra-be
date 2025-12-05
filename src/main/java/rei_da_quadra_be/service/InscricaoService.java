package rei_da_quadra_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rei_da_quadra_be.dto.InscricaoRequestDTO;
import rei_da_quadra_be.dto.InscricaoResponseDTO;
import rei_da_quadra_be.enums.StatusInscricao;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.Inscricao;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.repository.EventoRepository;
import rei_da_quadra_be.repository.InscricaoRepository;
import rei_da_quadra_be.repository.UserRepository;
import rei_da_quadra_be.service.exception.EventoNaoEncontradoException;
import rei_da_quadra_be.service.exception.RegraDeNegocioException;
import rei_da_quadra_be.service.exception.UsuarioNaoEncontradoException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
        
        // Retorna apenas inscrições APROVADAS
        return inscricaoRepository.findByEventoIdAndStatus(eventoId, StatusInscricao.APROVADA).stream()
            .map(InscricaoResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public InscricaoResponseDTO adicionarInscricao(Long eventoId, InscricaoRequestDTO request, User currentUser) {
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));
        
        User jogador;
        if (request.getJogadorId() != null) {
            jogador = userRepository.findById(request.getJogadorId())
                .orElseThrow(() -> new RegraDeNegocioException("Jogador não encontrado"));
        } else if (request.getJogadorEmail() != null) {
            UserDetails userDetails = userRepository.findByEmail(request.getJogadorEmail());
            if (userDetails == null) {
                throw new UsuarioNaoEncontradoException("Jogador não encontrado com este email");
            }
            jogador = (User) userDetails;
        } else {
            // Se não forneceu nem ID nem email, usa o usuário autenticado
            jogador = currentUser;
        }
        
        // Verificar se já existe inscrição
        Optional<Inscricao> inscricaoExistente = inscricaoRepository.findByEventoIdAndJogadorId(eventoId, jogador.getId());
        if (inscricaoExistente.isPresent()) {
            StatusInscricao status = inscricaoExistente.get().getStatus();
            if (status == StatusInscricao.APROVADA) {
                throw new RegraDeNegocioException("Jogador já está inscrito neste evento");
            } else if (status == StatusInscricao.PENDENTE) {
                throw new RegraDeNegocioException("Já existe uma solicitação pendente para este jogador");
            } else if (status == StatusInscricao.REJEITADA) {
                // Permite criar nova solicitação se foi rejeitada anteriormente
                inscricaoRepository.delete(inscricaoExistente.get());
            }
        }
        
        Inscricao inscricao = new Inscricao();
        inscricao.setEvento(evento);
        inscricao.setJogador(jogador);
        inscricao.setPartidasJogadas(0);
        inscricao.setDataInscricao(LocalDateTime.now());
        
        // Se for o organizador adicionando, aprova automaticamente
        if (evento.getUsuario().getId().equals(currentUser.getId())) {
            inscricao.setStatus(StatusInscricao.APROVADA);
        } else {
            inscricao.setStatus(StatusInscricao.PENDENTE);
        }
        
        inscricao = inscricaoRepository.save(inscricao);
        
        return InscricaoResponseDTO.fromEntity(inscricao);
    }
    
    @Transactional
    public void removerInscricao(Long eventoId, Long inscricaoId, User currentUser) {
        Evento evento = eventoRepository
            .findById(eventoId)
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
    
    @Transactional(readOnly = true)
    public List<InscricaoResponseDTO> listarSolicitacoesPendentes(Long eventoId, User currentUser) {
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));
        
        // Apenas o organizador pode ver solicitações pendentes
        if (!evento.getUsuario().getId().equals(currentUser.getId())) {
            throw new RegraDeNegocioException("Apenas o organizador pode ver solicitações pendentes");
        }
        
        return inscricaoRepository.findByEventoIdAndStatus(eventoId, StatusInscricao.PENDENTE).stream()
            .map(InscricaoResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public InscricaoResponseDTO aprovarSolicitacao(Long eventoId, Long inscricaoId, User currentUser) {
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));
        
        // Apenas o organizador pode aprovar
        if (!evento.getUsuario().getId().equals(currentUser.getId())) {
            throw new RegraDeNegocioException("Apenas o organizador pode aprovar solicitações");
        }
        
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
            .orElseThrow(() -> new RegraDeNegocioException("Inscrição não encontrada"));
        
        // Verificar se a inscrição pertence ao evento
        if (!inscricao.getEvento().getId().equals(eventoId)) {
            throw new RegraDeNegocioException("Inscrição não pertence a este evento");
        }
        
        // Verificar se está pendente
        if (inscricao.getStatus() != StatusInscricao.PENDENTE) {
            throw new RegraDeNegocioException("Apenas solicitações pendentes podem ser aprovadas");
        }
        
        // Aprovar
        inscricao.setStatus(StatusInscricao.APROVADA);
        inscricao = inscricaoRepository.save(inscricao);
        
        return InscricaoResponseDTO.fromEntity(inscricao);
    }
    
    @Transactional
    public void rejeitarSolicitacao(Long eventoId, Long inscricaoId, User currentUser) {
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));
        
        // Apenas o organizador pode rejeitar
        if (!evento.getUsuario().getId().equals(currentUser.getId())) {
            throw new RegraDeNegocioException("Apenas o organizador pode rejeitar solicitações");
        }
        
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
            .orElseThrow(() -> new RegraDeNegocioException("Inscrição não encontrada"));
        
        // Verificar se a inscrição pertence ao evento
        if (!inscricao.getEvento().getId().equals(eventoId)) {
            throw new RegraDeNegocioException("Inscrição não pertence a este evento");
        }
        
        // Verificar se está pendente
        if (inscricao.getStatus() != StatusInscricao.PENDENTE) {
            throw new RegraDeNegocioException("Apenas solicitações pendentes podem ser rejeitadas");
        }
        
        // Rejeitar
        inscricao.setStatus(StatusInscricao.REJEITADA);
        inscricaoRepository.save(inscricao);
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
