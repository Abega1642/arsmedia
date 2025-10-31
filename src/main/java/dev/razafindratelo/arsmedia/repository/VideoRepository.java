package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.JVideo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<JVideo, String> {
  Optional<JVideo> findByBucketKey(String bucketKey);
}
