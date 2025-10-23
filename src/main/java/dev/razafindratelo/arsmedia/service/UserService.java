package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.RUser;
import dev.razafindratelo.arsmedia.mapper.UserMapper;
import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import dev.razafindratelo.arsmedia.repository.model.JUser;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@Validated
@AllArgsConstructor
public class UserService implements UserDetailsService {
  private UserRepository repository;
  private Pagination paginator;
  private BCryptPasswordEncoder encoder;

  public User findByEmail(@Email String email) {
    if (email == null || email.isBlank())
      throw new IllegalArgumentException("Email cannot be null or blank");

    var jUser =
        repository
            .findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Email not found: " + email));

    return UserMapper.toUser(jUser);
  }

  public User create(RUser user) {
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
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + user.getId()));

    if (user.getPseudo() != null) existing.setPseudo(user.getPseudo());
    if (user.getPhoneNumber() != null) existing.setPhoneNumber(user.getPhoneNumber());
    if (user.getImageProfileBucketKey() != null)
      existing.setImageProfileBucketKey(user.getImageProfileBucketKey());
    if (user.getRole() != null) existing.setRole(user.getRole());
    if (user.getPassword() != null) existing.setPassword(encoder.encode(user.getPassword()));
    existing.setActivated(user.isActivated());
    existing.setUpdatedAt(LocalDateTime.now());

    return UserMapper.toUser(repository.save(existing));
  }

  public boolean updateActivationStatusByEmail(@Email String email, boolean isActivated) {
    if (email == null || email.isBlank())
      throw new IllegalArgumentException("Email cannot be null or blank");

    int updated = repository.updateActivationByEmail(email, isActivated, LocalDateTime.now());

    return updated >= 0;
  }

  public Page<User> findAll(Integer page, Integer size) {
    var pagination = paginator.apply(page, size);

    Pageable pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());

    var results = repository.findAll(pageable);

    return results.map(UserMapper::toUser);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return findByEmail(username);
  }
}
