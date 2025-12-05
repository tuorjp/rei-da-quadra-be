package rei_da_quadra_be.enums;

/**
 * Representa o resultado de uma partida do ponto de vista de um time específico.
 */
public enum ResultadoPartida {
  VITORIA(1.0),
  EMPATE(0.5),
  DERROTA(0.0);

  private final double valorElo;

  ResultadoPartida(double valorElo) {
    this.valorElo = valorElo;
  }

  public double getValorElo() {
    return valorElo;
  }

  public TipoAcaoEmJogo getTipoAcaoElo() {
    return switch (this) {
      case VITORIA -> TipoAcaoEmJogo.VITORIA_ELO;
      case EMPATE -> TipoAcaoEmJogo.EMPATE_ELO;
      case DERROTA -> TipoAcaoEmJogo.DERROTA_ELO;
    };
  }
}
