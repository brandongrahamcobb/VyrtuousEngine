package com.brandongcobb.vegan.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          // Disable CSRF to allow H2 console and easier prototyping
          .csrf(csrf -> csrf.disable())

          // Authorize requests
          .authorizeHttpRequests(auth -> auth
            // Vaadin static resources, dev console, login/logout
            .requestMatchers(
              "/VAADIN/**",
              "/favicon.ico",
              "/robots.txt",
              "/manifest.webmanifest",
              "/icons/**",
              "/images/**",
              "/frontend/**",
              "/webjars/**",
              "/h2-console/**",
              "/login",
              "/logout"
            ).permitAll()
            // All other requests require authentication
            .anyRequest().authenticated()
          )

          // Form login at /login
          .formLogin(form -> form
            .loginPage("/login")      // you can implement a Vaadin LoginView at this route
            .permitAll()
          )

          // Logout handling
          .logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessUrl("/login")
            .permitAll()
          );

        // Allow H2 console to be rendered in frames
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
