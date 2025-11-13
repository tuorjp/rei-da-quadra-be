package rei_da_quadra_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import rei_da_quadra_be.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthorizationService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserDetails user = userRepository.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("Usuário não encontrado");
        }

        return user;
    }
}
