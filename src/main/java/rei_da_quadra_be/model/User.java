package rei_da_quadra_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rei_da_quadra_be.enums.UserRole;

import java.util.Collection;
import java.util.List;

@Table(name = "users")
@Entity(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements UserDetails {
  @EqualsAndHashCode.Include
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String login;
  private String password;
  @Enumerated(EnumType.STRING)
  private UserRole role;

  public User(String login, String password, UserRole role) {
    this.login =  login;
    this.password = password;
    this.role = role;
  }

  @Override
  public String getUsername() {
    return login;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    if(this.role == UserRole.ADMIN) {
      return List.of(
              new SimpleGrantedAuthority("ROLE_ADMIN"),
              new SimpleGrantedAuthority("ROLE_USER")
      );
    } else {
      return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
  }

  @Override
  public boolean isAccountNonExpired() {
    return true; // Conta ativa
  }

  @Override
  public boolean isAccountNonLocked() {
    return true; // Conta não bloqueada
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true; // Senha não expirada
  }

  @Override
  public boolean isEnabled() {
    return true; // Usuário ativo
  }
}
