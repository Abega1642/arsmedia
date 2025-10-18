package dev.razafindratelo.arsmedia.config;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import dev.razafindratelo.arsmedia.InfraGenerated;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@InfraGenerated
@EnableWebSecurity
@Configuration
@AllArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConf {
  private final BCryptPasswordEncoder encoder;
  private final ApiKeyFilter apiKeyFilter;
  private final UserDetailsService userDetailsService;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.csrf(Customizer.withDefaults())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(GET, "/ping")
                    .permitAll()
                    .requestMatchers(GET, "/health/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers(GET, "/")
                    .permitAll()
                    .requestMatchers(
                        "/", "/doc", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml")
                    .permitAll()
                    .requestMatchers(POST, "/users/sign-up")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .sessionManagement(
            configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(apiKeyFilter, BasicAuthenticationFilter.class)
        .build();
  }

  @Bean
  public AuthenticationManager authManager(HttpSecurity http) throws Exception {
    AuthenticationManagerBuilder authenticationManagerBuilder =
        http.getSharedObject(AuthenticationManagerBuilder.class);
    authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(encoder);
    return authenticationManagerBuilder.build();
  }
}
