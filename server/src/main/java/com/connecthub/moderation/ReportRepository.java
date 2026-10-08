package com.connecthub.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(UUID reporterId, ReportTarget targetType, UUID targetId);
}
