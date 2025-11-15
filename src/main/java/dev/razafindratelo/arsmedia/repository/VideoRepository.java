package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.repository.model.JVideo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<JVideo, String> {
  Optional<JVideo> findByBucketKey(String bucketKey);

  @Query("select v from JVideo v join fetch v.owner")
  List<JVideo> findAllWithOwner();
}
