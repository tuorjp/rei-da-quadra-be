package rei_da_quadra_be.utils;

/**
 * Utilitário para cálculos de ELO rating (sistema de ranking dinâmico).
 *
 * Fórmulas utilizadas:
 * - Ea = 1.0 / (1 + Math.pow(10, (Rb - Ra) / 400.0))
 *   Onde:
 *     Ea = probabilidade esperada de vitória do jogador A
 *     Ra = pontuação atual do jogador A
 *     Rb = pontuação média do time adversário B
 *
 * - R'a = Ra + K * (Sa − Ea)
 *   Onde:
 *     R'a = nova pontuação do jogador A
 *     Ra = pontuação atual do jogador A
 *     K = fator de desenvolvimento (constante que controla a volatilidade)
 *     Sa = resultado da partida (1.0 para vitória, 0.0 para derrota)
 *     Ea = probabilidade esperada calculada acima
 */
public class EloCalculator {

    private static final double K_FACTOR = 32.0;

    public static double calcularProbabilidadeEsperada(double ra, double rb) {
        return 1.0 / (1.0 + Math.pow(10.0, (rb - ra) / 400.0));
    }

    public static double calcularNovaAvaliacao(double ra, double ea, double resultado) {
        return ra + K_FACTOR * (resultado - ea);
    }

    public static double calcularNovaAvaliacao(double raAtual, double rbMedia, boolean venceu) {
        double ea = calcularProbabilidadeEsperada(raAtual, rbMedia);
    double resultado = venceu ? 1.0 : 0.0;
    return calcularNovaAvaliacao(raAtual, ea, resultado);
  }

  public static int calcularVariacao(double raAtual, double rbMedia, boolean venceu) {
    double novaAvaliacao = calcularNovaAvaliacao(raAtual, rbMedia, venceu);
    return (int) Math.round(novaAvaliacao - raAtual);
  }
}

