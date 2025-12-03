package rei_da_quadra_be.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rei_da_quadra_be.enums.StatusEvento;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.User;
import rei_da_quadra_be.repository.EventoRepository;
import rei_da_quadra_be.service.exception.EventoNaoEncontradoException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventoService {
    private final EventoRepository eventoRepository;

    public Evento salvarEvento(Evento evento, User usuario) {
        evento.setUsuario(usuario);
        return eventoRepository.save(evento);
    }

    public List<Evento> listarEventosDoUsuario(User usuario) {
        return eventoRepository.findByUsuario(usuario);
    }

    /**
     * Busca eventos próximos (Raio 20km) que ocorrerão nos próximos 7 dias.
     * Filtra apenas eventos com status ATIVO.
     */
    public List<Evento> buscarEventosProximos(Double lat, Double lon, int limitSize) {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime limiteData = agora.plusDays(10);

        // Usa o limite dinâmico recebido do Controller
        PageRequest limit = PageRequest.of(0, limitSize);
        Double raio = 20.0; //20km

        return eventoRepository.buscarEventosProximos(
                lat,
                lon,
                raio,
                StatusEvento.ATIVO,
                agora,
                limiteData,
                limit
        );
    }

    public Optional<Evento> buscarEventoPorId(Long id) {
        return eventoRepository.findById(id);
    }

    public void deletarEvento(Long id, User usuario) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        if(!evento.getUsuario().getId().equals(usuario.getId())) {
            throw new SecurityException("Acesso negado: evento não vinculado ao seu usuário");
        }

        eventoRepository.delete(evento);
    }

    @Transactional
    public void finalizarEvento(Long id, User usuario) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado"));

        if (!evento.getUsuario().getId().equals(usuario.getId())) {
            throw new SecurityException("Acesso negado: evento não pertence ao usuário.");
        }

        evento.setStatus(StatusEvento.FINALIZADO);
        eventoRepository.save(evento);
    }

    public Evento atualizaEventoParcial(Long id, Map<String, Object> fields, User usuario) {
        Evento eventoExistente = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado para o id: " + id));

        if (!eventoExistente.getUsuario().getId().equals(usuario.getId())) {
            throw new SecurityException("Acesso negado: você não pode alterar um evento de outro usuário.");
        }

        Class<?> clazz = Evento.class;

        // para cada campo da classe evento
        fields.forEach((key, value) -> {
            try {
                if (key.equals("id") || key.equals("usuario")) {
                    return; // Pula para a próxima iteração
                }

                Field field = clazz.getDeclaredField(key);

                field.setAccessible(true);

                if (field.getType().equals(OffsetDateTime.class) && value instanceof String) {
                    try {
                        OffsetDateTime parsedDate = OffsetDateTime.parse((String) value);
                        field.set(eventoExistente, parsedDate);
                    } catch (DateTimeParseException e) {
                        System.err.println("Formato inválido para dataHorario. Valor recebido: " + value);
                        return;
                    }
                } else {
                    field.set(eventoExistente, value);
                }

                // TRATAMENTO LOCALDATETIME (caso exista outro campo no futuro)
                if (field.getType().equals(LocalDateTime.class) && value instanceof String) {
                    try {
                        LocalDateTime parsedDate = LocalDateTime.parse((String) value);
                        field.set(eventoExistente, parsedDate);
                        return;
                    } catch (DateTimeParseException e) {
                        System.err.println("Formato inválido para LocalDateTime no campo '" + key + "'");
                        return;
                    }
                }

            } catch (NoSuchFieldException e) {
                System.err.println("Campo não encontrado na entidade Evento: " + key);
            } catch (IllegalAccessException e) {
                System.err.println("Não foi possível acessar o campo: " + key);
            }
        });

        return eventoRepository.save(eventoExistente);
    }
}