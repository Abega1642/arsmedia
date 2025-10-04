package dev.razafindratelo.arsmedia.config;

import static org.springframework.http.HttpMethod.GET;

import dev.razafindratelo.arsmedia.InfraGenerated;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@InfraGenerated
@EnableWebSecurity
@Configuration
public class SecurityConf {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(GET, "/ping")
                    .permitAll()
                    .requestMatchers(GET, "/health/**")
                    .permitAll())
        .sessionManagement(
            configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
  }
}
