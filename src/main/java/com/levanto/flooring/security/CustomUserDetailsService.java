package com.levanto.flooring.security;

import com.levanto.flooring.entity.AppUser;
import com.levanto.flooring.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AppUserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = repo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new User(u.getUsername(), u.getPassword(), u.isEnabled(), true, true, true,
            List.of(new SimpleGrantedAuthority(u.getRole().name())));
    }
}
