package com.pacgram.moderation.repository;

import com.pacgram.moderation.entity.ReportEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportEvidenceRepository extends JpaRepository<ReportEvidence, Long> {
    @Query("select e from ReportEvidence e where e.report.id in :reportIds order by e.createdAt")
    List<ReportEvidence> findAllByReportIds(@Param("reportIds") List<Long> reportIds);

    Optional<ReportEvidence> findByIdAndReportId(Long id, Long reportId);
}
