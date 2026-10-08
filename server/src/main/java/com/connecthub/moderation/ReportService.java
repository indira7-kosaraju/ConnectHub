package com.connecthub.moderation;

import com.connecthub.common.TextSanitizer;
import com.connecthub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Records reports. Reporting the same target twice is a no-op (and not an error) for the reporter. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reports;
    private final UserRepository users;

    @Transactional
    public void report(UUID reporterId, ReportTarget type, UUID targetId, ReportRequest req) {
        if (reports.existsByReporterIdAndTargetTypeAndTargetId(reporterId, type, targetId)) {
            return;
        }
        Report r = new Report();
        r.setReporter(users.getReferenceById(reporterId));
        r.setTargetType(type);
        r.setTargetId(targetId);
        r.setReason(req.reason());
        r.setDetails(TextSanitizer.plain(req.details()));
        reports.save(r);
        log.info("Report filed: {} {} for {}", type, targetId, req.reason());
    }
}
