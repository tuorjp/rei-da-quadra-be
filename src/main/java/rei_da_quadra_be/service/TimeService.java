package rei_da_quadra_be.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rei_da_quadra_be.model.Evento;
import rei_da_quadra_be.model.Time;
import rei_da_quadra_be.repository.EventoRepository;
import rei_da_quadra_be.repository.TimeRepository;
import rei_da_quadra_be.service.exception.EventoNaoEncontradoException;
import rei_da_quadra_be.service.exception.TimeNaoEncontradoException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeService {

  private final TimeRepository timeRepository;
  private final EventoRepository eventoRepository;

  public Time buscarPorId(Long id) {
    return timeRepository.findById(id)
      .orElseThrow(() -> new TimeNaoEncontradoException("Time não encontrado com id: " + id));
  }

  public List<Time> listarTimesDoEvento(Long eventoId) {
    if (!eventoRepository.existsById(eventoId)) {
      throw new EventoNaoEncontradoException("Evento não encontrado.");
    }
    return timeRepository.findByEventoId(eventoId);
  }

  //busca especificamente o time de espera (reserva) de um evento
  public Time buscarTimeDeEspera(Long eventoId) {
    Evento evento = eventoRepository.findById(eventoId)
      .orElseThrow(() -> new EventoNaoEncontradoException("Evento não encontrado."));

    return timeRepository.findByEventoAndTimeDeEsperaTrue(evento)
      .orElseThrow(() -> new TimeNaoEncontradoException("Time de espera não configurado para este evento."));
  }

  @Transactional
  public Time atualizarTime(Long timeId, String novoNome, String novaCor) {
    Time time = buscarPorId(timeId);
    if (novoNome != null && !novoNome.isBlank()) time.setNome(novoNome);
    if (novaCor != null && !novaCor.isBlank()) time.setCor(novaCor);
    return timeRepository.save(time);
  }
}

