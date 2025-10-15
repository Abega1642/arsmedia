package dev.razafindratelo.arsmedia.service;

import dev.razafindratelo.arsmedia.model.User;
import dev.razafindratelo.arsmedia.repository.UserRepository;
import dev.razafindratelo.arsmedia.repository.mapper.UserMapper;
import dev.razafindratelo.arsmedia.repository.model.JUser;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class UserService {
  private UserRepository repository;
  private Pagination paginator;

  public User findByEmail(@Email String email) {
    if (email == null || email.isBlank())
      throw new IllegalArgumentException("Email cannot be null or blank");

    var jUser =
        repository
            .findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Email not found: " + email));

    return UserMapper.toUser(jUser);
  }

  public User create(JUser user) {
    if (user == null) throw new IllegalArgumentException("User cannot be null");
    return UserMapper.toUser(repository.save(user));
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
    existing.setActivated(user.isActivated());
    existing.setUpdatedAt(LocalDateTime.now());

    return UserMapper.toUser(repository.save(existing));
  }

  public boolean updateActivationStatusByEmail(@Email String email, boolean isActivated) {
    if (email == null || email.isBlank())
      throw new IllegalArgumentException("Email cannot be null or blank");

    int updated = repository.updateActivationByEmail(email, isActivated, LocalDateTime.now());
    if (updated == 0) throw new EntityNotFoundException("User not found with email: " + email);
    return true;
  }

  public Page<JUser> findAll(Integer page, Integer size) {
    var pagination = paginator.apply(page, size);

    Pageable pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());
    return repository.findAll(pageable);
  }
}
