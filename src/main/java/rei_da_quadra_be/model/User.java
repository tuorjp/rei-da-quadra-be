package rei_da_quadra_be.model;

import jakarta.persistence.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String role = "USER"; // ou "ROLE_USER" dependendo do seu padrão

  @Column(nullable = false)
  private boolean enabled = false; // começa desativado até confirmar o email

  @Column(name = "pontos_habilidade", nullable = false)
  private Integer pontosHabilidade = 1000;

  @Column(name = "nivel_habilidade", nullable = false)
  private Integer nivelHabilidade = 3;

  @OneToMany(mappedBy = "jogador")
  private List<Inscricao> inscricoes;

  @OneToMany(mappedBy = "jogador")
  private List<ParticipacaoDesempenho> desempenhos;

  @OneToMany(mappedBy = "jogador")
  private List<HistoricoPontuacao> historicoPontuacao;

  @OneToMany(mappedBy = "jogador")
  private List<HistoricoTransferencia> historicoTransferencia;

  // --- Construtores ---
  public User() {}

  public User(String nome, String email, String password, String role) {
    this.nome = nome;
    this.email = email;
    this.password = password;
    this.role = role;
    this.enabled = false;
  }

  // --- Getters e Setters ---
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

  // Metodos exigidos por UserDetails (Spring Security)
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
      return List.of(() -> "ROLE_USER");
  }


    @Override
  public String getUsername() {
    return this.email; // o login é feito pelo email
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }
}
