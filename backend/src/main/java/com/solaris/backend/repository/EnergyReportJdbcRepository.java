package com.solaris.backend.repository;

import com.solaris.backend.dto.report.DailyEnergyReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EnergyReportJdbcRepository {
    private static final String BASE_SQL = """
            select cast(er.recorded_at as date) as report_date,
                   s.id as site_id,
                   s.name as site_name,
                   coalesce(sum(er.production_kwh), 0) as production_kwh,
                   coalesce(sum(er.consumption_kwh), 0) as consumption_kwh,
                   coalesce(sum(er.grid_import_kwh), 0) as grid_import_kwh,
                   coalesce(sum(er.grid_export_kwh), 0) as grid_export_kwh,
                   count(*) as reading_count
              from energy_readings er
              join solar_sites s on s.id = er.site_id
             where s.owner_id = :ownerId
               and er.recorded_at >= :fromTimestamp
               and er.recorded_at < :toTimestamp
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<DailyEnergyReportResponse> dailyReport(
            Long ownerId, Long siteId, Instant fromInclusive, Instant toExclusive) {
        String sql = BASE_SQL
                + (siteId == null ? "" : " and s.id = :siteId\n")
                + " group by cast(er.recorded_at as date), s.id, s.name\n"
                + " order by report_date asc, s.name asc";
        var parameters = new MapSqlParameterSource()
                .addValue("ownerId", ownerId)
                .addValue("fromTimestamp", Timestamp.from(fromInclusive))
                .addValue("toTimestamp", Timestamp.from(toExclusive));
        if (siteId != null) {
            parameters.addValue("siteId", siteId);
        }
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new DailyEnergyReportResponse(
                resultSet.getObject("report_date", java.time.LocalDate.class),
                resultSet.getLong("site_id"),
                resultSet.getString("site_name"),
                resultSet.getBigDecimal("production_kwh"),
                resultSet.getBigDecimal("consumption_kwh"),
                resultSet.getBigDecimal("grid_import_kwh"),
                resultSet.getBigDecimal("grid_export_kwh"),
                resultSet.getLong("reading_count")
        ));
    }
}
