package com.pacgram.admin.service;

import com.pacgram.admin.dto.AdminAuditDto;
import com.pacgram.admin.dto.AdminBlockDto;
import com.pacgram.admin.dto.AdminDashboardDto;
import com.pacgram.admin.dto.AdminPageDto;
import com.pacgram.admin.dto.AdminPostDto;
import com.pacgram.admin.dto.AdminReportDto;
import com.pacgram.admin.dto.AdminUserDetailDto;
import com.pacgram.admin.dto.AdminUserDto;
import com.pacgram.admin.dto.UpdateReportRequest;
import com.pacgram.common.exception.NotFoundException;
import com.pacgram.moderation.dto.ReportEvidenceDto;
import com.pacgram.moderation.repository.ReportEvidenceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class AdminManagementService {
    private static final int MAX_PAGE_SIZE = 100;
    private final JdbcTemplate jdbcTemplate;
    private final ReportEvidenceRepository evidenceRepository;

    public AdminManagementService(JdbcTemplate jdbcTemplate, ReportEvidenceRepository evidenceRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.evidenceRepository = evidenceRepository;
    }

    public AdminDashboardDto overview() {
        long total = count("SELECT COUNT_BIG(*) FROM users WHERE account_role = 'USER'");
        long online = count("""
                SELECT COUNT_BIG(*) FROM users WHERE account_role = 'USER' AND account_status = 'ACTIVE'
                  AND activity_status = 1
                  AND last_seen_at >= DATEADD(SECOND, -90, SYSUTCDATETIME())
                """);
        long suspended = count(
                "SELECT COUNT_BIG(*) FROM users WHERE account_role = 'USER' AND account_status = 'SUSPENDED'");
        long pendingReports = count("SELECT COUNT_BIG(*) FROM user_reports WHERE status = 'PENDING'");
        long inReviewReports = count("SELECT COUNT_BIG(*) FROM user_reports WHERE status = 'IN_REVIEW'");
        long resolvedReports = count("SELECT COUNT_BIG(*) FROM user_reports WHERE status = 'RESOLVED'");
        long dismissedReports = count("SELECT COUNT_BIG(*) FROM user_reports WHERE status = 'DISMISSED'");
        return new AdminDashboardDto(total, online, Math.max(0, total - online - suspended), suspended,
                count("SELECT COUNT_BIG(DISTINCT reported_user_id) FROM user_reports"),
                count("SELECT COUNT_BIG(*) FROM user_blocks"),
                pendingReports, inReviewReports, resolvedReports, dismissedReports,
                pendingReports + inReviewReports + resolvedReports + dismissedReports,
                count("""
                        SELECT COUNT_BIG(*) FROM users
                        WHERE account_role = 'USER'
                          AND created_at >= DATEADD(DAY, -7, SYSUTCDATETIME())
                        """));
    }

    public AdminPageDto<AdminUserDto> users(int requestedPage, int requestedSize, String query) {
        int page = Math.max(0, requestedPage);
        int size = pageSize(requestedSize);
        int offset = page * size;
        String term = query == null || query.isBlank() ? null : "%" + query.trim() + "%";
        String filter = term == null ? "WHERE u.account_role = 'USER'"
                : "WHERE u.account_role = 'USER' AND (u.username LIKE ? OR u.display_name LIKE ? OR u.email LIKE ?)";
        Object[] args = term == null ? new Object[0] : new Object[]{term, term, term};
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT_BIG(*) FROM users u " + filter, Long.class, args);
        String sql = """
                SELECT u.id, u.display_name, u.username, u.email, u.account_status,
                    u.last_seen_at, u.created_at,
                    (SELECT COUNT_BIG(*) FROM posts p WHERE p.author_id = u.id) AS posts_count
                FROM users u
                """ + filter + """
                ORDER BY u.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        List<AdminUserDto> result = jdbcTemplate.query(sql, (row, index) -> new AdminUserDto(
                row.getLong("id"), row.getString("display_name"), row.getString("username"),
                row.getString("email"), row.getString("account_status"),
                row.getTimestamp("last_seen_at") == null ? null : row.getTimestamp("last_seen_at").toInstant(),
                row.getTimestamp("created_at").toInstant(), row.getLong("posts_count")),
                append(args, offset, size));
        return AdminPageDto.of(result, page, size, total);
    }

    public AdminUserDetailDto userDetail(long userId) {
        List<AdminUserDetailDto> result = jdbcTemplate.query("""
                SELECT u.id, u.display_name, u.username, u.email, u.account_status, u.last_seen_at,
                    u.created_at, u.bio, u.avatar_url, u.cover_url, u.private_account, u.activity_status,
                    (SELECT COUNT_BIG(*) FROM posts p WHERE p.author_id = u.id) AS posts_count,
                    (SELECT COUNT_BIG(*) FROM user_follows f WHERE f.followed_id = u.id) AS followers_count,
                    (SELECT COUNT_BIG(*) FROM user_follows f WHERE f.follower_id = u.id) AS following_count,
                    (SELECT COUNT_BIG(*) FROM user_reports r WHERE r.reporter_id = u.id) AS reports_made_count,
                    (SELECT COUNT_BIG(*) FROM user_reports r WHERE r.reported_user_id = u.id) AS reports_received_count,
                    (SELECT COUNT_BIG(*) FROM user_blocks b WHERE b.blocked_id = u.id) AS blocked_by_count,
                    (SELECT COUNT_BIG(*) FROM user_blocks b WHERE b.blocker_id = u.id) AS blocking_count
                FROM users u
                WHERE u.id = ? AND u.account_role = 'USER'
                """, (row, index) -> {
            AdminUserDto account = new AdminUserDto(
                    row.getLong("id"), row.getString("display_name"), row.getString("username"),
                    row.getString("email"), row.getString("account_status"),
                    row.getTimestamp("last_seen_at") == null ? null : row.getTimestamp("last_seen_at").toInstant(),
                    row.getTimestamp("created_at").toInstant(), row.getLong("posts_count"));
            return new AdminUserDetailDto(account, row.getString("bio"), row.getString("avatar_url"),
                    row.getString("cover_url"), row.getBoolean("private_account"),
                    row.getBoolean("activity_status"), row.getLong("followers_count"),
                    row.getLong("following_count"), row.getLong("reports_made_count"),
                    row.getLong("reports_received_count"), row.getLong("blocked_by_count"),
                    row.getLong("blocking_count"));
        }, userId);
        if (result.isEmpty()) throw new NotFoundException("User was not found");
        return result.get(0);
    }

    public AdminPageDto<AdminPostDto> userPosts(long userId, int requestedPage, int requestedSize) {
        requireUser(userId);
        int page = Math.max(0, requestedPage);
        int size = pageSize(requestedSize);
        int offset = page * size;
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT_BIG(*) FROM posts WHERE author_id = ?", Long.class, userId);
        List<AdminPostDto> result = jdbcTemplate.query("""
                SELECT p.id, p.caption, p.image_url, p.created_at,
                    (SELECT COUNT_BIG(*) FROM post_likes l WHERE l.post_id = p.id) AS likes_count,
                    (SELECT COUNT_BIG(*) FROM post_comments c WHERE c.post_id = p.id) AS comments_count
                FROM posts p
                WHERE p.author_id = ?
                ORDER BY p.created_at DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (row, index) -> new AdminPostDto(row.getLong("id"), row.getString("caption"),
                row.getString("image_url"), row.getTimestamp("created_at").toInstant(),
                row.getLong("likes_count"), row.getLong("comments_count")), userId, offset, size);
        return AdminPageDto.of(result, page, size, total);
    }

    public AdminPageDto<AdminBlockDto> userBlocks(long userId, int requestedPage, int requestedSize) {
        requireUser(userId);
        int page = Math.max(0, requestedPage);
        int size = pageSize(requestedSize);
        int offset = page * size;
        long total = jdbcTemplate.queryForObject("""
                SELECT COUNT_BIG(*) FROM user_blocks WHERE blocker_id = ? OR blocked_id = ?
                """, Long.class, userId, userId);
        List<AdminBlockDto> result = jdbcTemplate.query("""
                SELECT b.id, b.blocker_id, blocker.username AS blocker_username,
                    b.blocked_id, blocked.username AS blocked_username, b.created_at
                FROM user_blocks b
                JOIN users blocker ON blocker.id = b.blocker_id
                JOIN users blocked ON blocked.id = b.blocked_id
                WHERE b.blocker_id = ? OR b.blocked_id = ?
                ORDER BY b.created_at DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (row, index) -> new AdminBlockDto(row.getLong("id"), row.getLong("blocker_id"),
                row.getString("blocker_username"), row.getLong("blocked_id"),
                row.getString("blocked_username"), row.getTimestamp("created_at").toInstant()),
                userId, userId, offset, size);
        return AdminPageDto.of(result, page, size, total);
    }

    public AdminPageDto<AdminReportDto> reports(int requestedPage, int requestedSize, String status) {
        return reportPage(requestedPage, requestedSize, status, null);
    }

    public AdminPageDto<AdminReportDto> userReports(long userId, int requestedPage, int requestedSize) {
        requireUser(userId);
        return reportPage(requestedPage, requestedSize, null, userId);
    }

    private AdminPageDto<AdminReportDto> reportPage(
            int requestedPage, int requestedSize, String status, Long userId) {
        int page = Math.max(0, requestedPage);
        int size = pageSize(requestedSize);
        int offset = page * size;
        String normalized = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        List<Object> filters = new java.util.ArrayList<>();
        List<String> conditions = new java.util.ArrayList<>();
        if (normalized != null) {
            conditions.add("r.status = ?");
            filters.add(normalized);
        }
        if (userId != null) {
            conditions.add("(r.reporter_id = ? OR r.reported_user_id = ?)");
            filters.add(userId);
            filters.add(userId);
        }
        String filter = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT_BIG(*) FROM user_reports r " + filter, Long.class, filters.toArray());
        String sql = """
                SELECT r.id, r.reporter_id, reporter.username reporter_username,
                    reporter.display_name reporter_name, r.reported_user_id,
                    target.username reported_username, target.display_name reported_name,
                    r.reason, r.details, r.content_type, r.content_id, r.content_snapshot,
                    r.status, r.resolution_note, r.created_at, r.updated_at
                FROM user_reports r
                JOIN users reporter ON reporter.id = r.reporter_id
                JOIN users target ON target.id = r.reported_user_id
                """ + filter + """
                ORDER BY r.created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        List<AdminReportDto> rows = jdbcTemplate.query(sql, (row, index) -> new AdminReportDto(
                row.getLong("id"), row.getLong("reporter_id"), row.getString("reporter_username"),
                row.getString("reporter_name"), row.getLong("reported_user_id"),
                row.getString("reported_username"), row.getString("reported_name"),
                row.getString("reason"), row.getString("details"), row.getString("content_type"),
                row.getObject("content_id", Long.class), row.getString("content_snapshot"),
                row.getString("status"), row.getString("resolution_note"),
                row.getTimestamp("created_at").toInstant(), row.getTimestamp("updated_at").toInstant(), List.of()),
                append(filters.toArray(), offset, size));
        List<Long> reportIds = rows.stream().map(AdminReportDto::id).toList();
        var evidenceByReport = (reportIds.isEmpty() ? List.<com.pacgram.moderation.entity.ReportEvidence>of()
                : evidenceRepository.findAllByReportIds(reportIds)).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        evidence -> evidence.getReport().getId(),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.mapping(
                                evidence -> new ReportEvidenceDto(evidence.getId(), evidence.getOriginalName(),
                                        evidence.getContentType(), evidence.getFileSize()),
                                java.util.stream.Collectors.toList())));
        List<AdminReportDto> result = rows.stream().map(report -> new AdminReportDto(
                report.id(), report.reporterId(), report.reporterUsername(), report.reporterName(),
                report.reportedUserId(), report.reportedUsername(), report.reportedName(),
                report.reason(), report.details(), report.contentType(), report.contentId(),
                report.contentSnapshot(), report.status(), report.resolutionNote(),
                report.createdAt(), report.updatedAt(),
                evidenceByReport.getOrDefault(report.id(), List.of()))).toList();
        return AdminPageDto.of(result, page, size, total);
    }

    private void requireUser(long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT_BIG(*) FROM users WHERE id = ? AND account_role = 'USER'", Long.class, userId);
        if (count == null || count == 0) throw new NotFoundException("User was not found");
    }

    @Transactional
    public void setUserStatus(String adminUsername, long userId, String status) {
        int changed = jdbcTemplate.update(
                "UPDATE users SET account_status = ? WHERE id = ? AND account_role = 'USER'", status, userId);
        if (changed == 0) throw new NotFoundException("User was not found");
        audit(adminUsername, "UPDATE_USER_STATUS", "USER", userId, "Changed status to " + status);
    }

    @Transactional
    public void updateReport(String adminUsername, long id, UpdateReportRequest request) {
        int changed = jdbcTemplate.update("""
                UPDATE user_reports SET status = ?, resolution_note = ?, updated_at = SYSUTCDATETIME()
                WHERE id = ?
                """, request.status(), blankToNull(request.resolutionNote()), id);
        if (changed == 0) throw new NotFoundException("Report was not found");
        audit(adminUsername, "UPDATE_REPORT", "REPORT", id, "Changed report status to " + request.status());
    }

    public AdminPageDto<AdminAuditDto> audit(int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = pageSize(requestedSize);
        int offset = page * size;
        long total = count("SELECT COUNT_BIG(*) FROM admin_audit_log");
        List<AdminAuditDto> result = jdbcTemplate.query("""
                SELECT id, admin_username, action, target_type, target_id, description, created_at
                FROM admin_audit_log ORDER BY created_at DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """, (row, index) -> new AdminAuditDto(row.getLong("id"), row.getString("admin_username"),
                row.getString("action"), row.getString("target_type"),
                row.getObject("target_id", Long.class), row.getString("description"),
                row.getTimestamp("created_at").toInstant()), offset, size);
        return AdminPageDto.of(result, page, size, total);
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0 : result;
    }

    private void audit(String admin, String action, String type, long id, String description) {
        jdbcTemplate.update("""
                INSERT INTO admin_audit_log (admin_username, action, target_type, target_id, description, created_at)
                VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME())
                """, admin, action, type, id, description);
    }

    private int pageSize(int requested) {
        return Math.max(1, Math.min(requested, MAX_PAGE_SIZE));
    }

    private Object[] append(Object[] original, Object... values) {
        Object[] result = Arrays.copyOf(original, original.length + values.length);
        System.arraycopy(values, 0, result, original.length, values.length);
        return result;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
