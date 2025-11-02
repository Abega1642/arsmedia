package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.JMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends JpaRepository<JMedia, String> {}
