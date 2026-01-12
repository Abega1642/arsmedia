package dev.razafindratelo.arsmedia.service;

import static org.owasp.encoder.Encode.forJava;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.UserCreationRequest;
import dev.razafindratelo.arsmedia.mapper.UserMapper;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import dev.razafindratelo.arsmedia.repository.model.JUser;
import dev.razafindratelo.arsmedia.service.util.Paginator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@AllArgsConstructor
@Slf4j
@Profile("prod")
@Validated
public class UserService implements UserDetailsService {

  private UserRepository repository;
  private Paginator paginator;
  private BCryptPasswordEncoder encoder;

  public User findById(@NotBlank @NotNull String id) {

    var jUser =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "User with id : %s not found".formatted(forJava(id))));

    return UserMapper.toUser(jUser);
  }

  /**
   * Stub implementation to allow app startup without DB
   */
  public Object findByEmail(String email) {
    // Stub implementation to allow app startup without DB
    return null;
  }

  public User create(UserCreationRequest user) {
    if (user == null) throw new IllegalArgumentException("User cannot be null");

    var jUser = UserMapper.toJUser(UserMapper.toUser(user));
    var encodedPassword = encoder.encode(jUser.getPassword());
    jUser.setPassword(encodedPassword);

    return UserMapper.toUser(repository.save(jUser));
  }

  public User update(JUser user) {
    if (user == null) throw new IllegalArgumentException("User cannot be null");
    if (user.getId() == null) throw new IllegalArgumentException("User ID cannot be null");

    JUser existing =
        repository
            .findById(user.getId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "User not found: %s".formatted(user.getId())));

    if (user.getPseudo() != null) existing.setPseudo(user.getPseudo());
    if (user.getPhoneNumber() != null) existing.setPhoneNumber(user.getPhoneNumber());
    if (user.getImageProfileBucketKey() != null)
      existing.setImageProfileBucketKey(user.getImageProfileBucketKey());
    if (user.getRole() != null) existing.setRole(user.getRole());
    if (user.getPassword() != null)
      existing.setPassword(encoder.encode(user.getPassword()));

    existing.setActivated(user.isActivated());
    existing.setUpdatedAt(LocalDateTime.now());

    return UserMapper.toUser(repository.save(existing));
  }

  public Page<User> findAll(Integer page, Integer size) {
    var pagination = paginator.apply(page, size);

    Pageable pageable =
        PageRequest.of(
            pagination.get("page"),
            pagination.get("size"),
            Sort.by("createdAt").descending());

    var results = repository.findAll(pageable);
    return results.map(UserMapper::toUser);
  }

  @Override
  public UserDetails loadUserByUsername(String username)
      throws UsernameNotFoundException {
    return null; // stubbed for startup
  }
}
