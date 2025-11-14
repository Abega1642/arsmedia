package dev.razafindratelo.arsmedia.repository;

import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.repository.model.VideoCompressionJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoCompressionJobRepository extends JpaRepository<VideoCompressionJob, String> {

  @Modifying
  @Query("UPDATE VideoCompressionJob cmp SET cmp.status = :process_status WHERE cmp.id = :id")
  int updateCompressedVideoStatus(
      @Param("process_status") ProcessStatus processStatus, @Param("id") String id);

  @Query(
      """
      SELECT cmp
      FROM VideoCompressionJob cmp
      WHERE cmp.status = dev.razafindratelo.arsmedia.model.classifier.ProcessStatus.COMPLETED
        AND cmp.parent.owner.email = :email
      """)
  List<VideoCompressionJob> findAllCompletedByOwnerEmail(@Param("email") String email);

  List<VideoCompressionJob> findByParentId(String parentId);
}
