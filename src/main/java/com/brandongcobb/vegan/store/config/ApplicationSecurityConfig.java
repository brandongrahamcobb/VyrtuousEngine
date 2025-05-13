package com.brandongcobb.vegan.store.config;

import com.brandongcobb.vegan.store.repo.CustomerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application-wide security beans.
 */
@Configuration
public class ApplicationSecurityConfig {
    @Bean
    public UserDetailsService userDetailsService(CustomerRepository repo) {
        return username -> repo.findByEmail(username)
            .map(c -> new org.springframework.security.core.userdetails.User(
                c.getEmail(),
                c.getPassword(),
                java.util.Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER")
                )
            ))
            .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                "User not found: " + username
            ));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}