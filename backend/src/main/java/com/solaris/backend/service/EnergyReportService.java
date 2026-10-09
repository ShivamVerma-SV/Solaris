package com.solaris.backend.service;

import com.solaris.backend.dto.report.DailyEnergyReportResponse;
import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.exception.ReportQueryException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.EnergyReportJdbcRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnergyReportService {
    private final EnergyReportJdbcRepository reportRepository;
    private final SolarSiteRepository siteRepository;

    @Transactional(readOnly = true)
    public List<DailyEnergyReportResponse> dailyReport(
            Long ownerId, Long siteId, LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to == null ? LocalDate.now(ZoneOffset.UTC) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
        if (ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) > 365) {
            throw new BadRequestException("Report date range cannot exceed 366 days");
        }
        if (siteId != null && siteRepository.findByIdAndOwnerId(siteId, ownerId).isEmpty()) {
            // A non-owned site is reported as missing to avoid revealing that another user's site exists.
            throw new ResourceNotFoundException("Solar site not found");
        }
        try {
            // Convert the inclusive date range into a half-open UTC interval for unambiguous SQL boundaries.
            return reportRepository.dailyReport(
                    ownerId,
                    siteId,
                    resolvedFrom.atStartOfDay().toInstant(ZoneOffset.UTC),
                    resolvedTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            );
        } catch (DataAccessException exception) {
            throw new ReportQueryException(exception);
        }
    }
}
