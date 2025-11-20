package rei_da_quadra_be.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rei_da_quadra_be.model.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {
  @Value("${api.security.token.secret}") //variável de ambiente, na application.properties
  private String secret;

  public String generateToken(User user) {
    try {
      //Algoritmo que recebe uma secret, com base na qual vai gerar os tokens
      Algorithm alg = Algorithm.HMAC256(secret);

      String token = JWT.create()
              .withIssuer("login_jwt") //nome do emissor
              .withSubject(user.getEmail()) //subject é o usuário
              .withExpiresAt(generateExpirationDate())
              .sign(alg); //assina e gera o token

      return token;
    } catch (JWTCreationException e) {
      throw new RuntimeException("Error while generating token: " + e);
    }
  }

  public String validateToken(String token) {
    try {
      Algorithm alg = Algorithm.HMAC256(secret);
      return JWT.require(alg)
              .withIssuer("login_jwt")
              .build()
              .verify(token)
              .getSubject(); //pega o subject User passado ao criar o token
    } catch (JWTVerificationException e) {
      return "";
    }
  }

  private Instant generateExpirationDate() {
    return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
  }
}
