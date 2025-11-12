package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.repository.model.JCompressedVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompressedVideoRepository extends JpaRepository<JCompressedVideo, String> {
  @Modifying
  @Query("UPDATE JCompressedVideo cmp SET cmp.status = :process_status WHERE cmp.id = :id")
  int updateCompressedVideoStatus(
      @Param("process_status") ProcessStatus processStatus, @Param("id") String id);
}
