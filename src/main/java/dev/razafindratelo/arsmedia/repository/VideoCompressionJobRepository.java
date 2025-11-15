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
      "SELECT j FROM VideoCompressionJob j WHERE j.status = :status AND j.parent.owner.email ="
          + " :email")
  List<VideoCompressionJob> findByStatusAndOwnerEmail(
      @Param("status") ProcessStatus status, @Param("email") String email);

  default List<VideoCompressionJob> findAllCompletedByOwnerEmail(String email) {
    return findByStatusAndOwnerEmail(ProcessStatus.COMPLETED, email);
  }

  List<VideoCompressionJob> findByParentId(String parentId);
}
