package com.brandongcobb.vegan.store.config;

import com.brandongcobb.vegan.store.repo.CustomerRepository;
import com.brandongcobb.vegan.store.ui.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpMethod;

/**
 * Combined security configuration for Vaadin UI and REST API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
          // Disable CSRF for simplicity (enable for production)
          .csrf(csrf -> csrf.disable())

          // Authorize requests
          .authorizeHttpRequests(auth -> auth
              // Vaadin internal and static resources
              .requestMatchers(new AntPathRequestMatcher("/vaadinServlet/**")).permitAll()
              .requestMatchers(new AntPathRequestMatcher("/VAADIN/**")).permitAll()
              .requestMatchers(new AntPathRequestMatcher("/robots.txt")).permitAll()
              .requestMatchers(new AntPathRequestMatcher("/manifest.webmanifest")).permitAll()
              .requestMatchers(new AntPathRequestMatcher("/favicon.ico")).permitAll()
              .requestMatchers(new AntPathRequestMatcher("/frontend/**")).permitAll()
              .requestMatchers(new AntPathRequestMatcher("/webjars/**")).permitAll()
              .requestMatchers(new AntPathRequestMatcher("/PUSH/**")).permitAll()
              .requestMatchers(new AntPathRequestMatcher("/HEARTBEAT/**")).permitAll()
              .requestMatchers(new AntPathRequestMatcher("/UIDL/**")).permitAll()

              // Public REST endpoints
              .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
              .requestMatchers(
                  new AntPathRequestMatcher("/api/products/**", HttpMethod.GET.name()),
                  new AntPathRequestMatcher("/api/categories/**", HttpMethod.GET.name())
              ).permitAll()

              // Public UI endpoints
              .requestMatchers(new AntPathRequestMatcher("/"),
                               new AntPathRequestMatcher("/login"),
                               new AntPathRequestMatcher("/store"),
                               new AntPathRequestMatcher("/register")).permitAll()

              // Any other request requires authentication
              .anyRequest().authenticated()
          )

          // Form login configuration
          .formLogin(form -> form
              .loginPage("/login")
              .permitAll()
          )

          // Logout configuration
          .logout(logout -> logout
              .logoutUrl("/logout")
              .logoutSuccessUrl("/login")
              .permitAll()
          );
        return http.build();
    }

}
