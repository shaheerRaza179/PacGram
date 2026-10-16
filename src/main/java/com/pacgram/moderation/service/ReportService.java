package com.pacgram.moderation.service;

import com.pacgram.auth.entity.UserAccount;
import com.pacgram.auth.repository.UserAccountRepository;
import com.pacgram.common.exception.ApiException;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.messaging.entity.DirectMessage;
import com.pacgram.messaging.repository.DirectMessageRepository;
import com.pacgram.moderation.dto.CreateReportRequest;
import com.pacgram.moderation.dto.ReportReceiptDto;
import com.pacgram.moderation.entity.UserReport;
import com.pacgram.moderation.entity.ReportEvidence;
import com.pacgram.moderation.dto.ReportEvidenceDto;
import com.pacgram.moderation.repository.ReportEvidenceRepository;
import com.pacgram.moderation.repository.UserReportRepository;
import com.pacgram.moderation.service.ReportEvidenceStorageService.StoredEvidence;
import com.pacgram.post.entity.Comment;
import com.pacgram.post.entity.Post;
import com.pacgram.post.repository.CommentRepository;
import com.pacgram.post.repository.PostRepository;
import com.pacgram.user.entity.UserBlock;
import com.pacgram.user.repository.UserBlockRepository;
import com.pacgram.user.repository.UserFollowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {
    private final UserAccountRepository userRepository;
    private final UserReportRepository reportRepository;
    private final DirectMessageRepository messageRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserBlockRepository blockRepository;
    private final UserFollowRepository followRepository;
    private final ReportEvidenceRepository evidenceRepository;
    private final ReportEvidenceStorageService evidenceStorage;

    public ReportService(
            UserAccountRepository userRepository,
            UserReportRepository reportRepository,
            DirectMessageRepository messageRepository,
            PostRepository postRepository,
            CommentRepository commentRepository,
            UserBlockRepository blockRepository,
            UserFollowRepository followRepository,
            ReportEvidenceRepository evidenceRepository,
            ReportEvidenceStorageService evidenceStorage) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.messageRepository = messageRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.blockRepository = blockRepository;
        this.followRepository = followRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceStorage = evidenceStorage;
    }

    @Transactional
    public ReportReceiptDto create(AccountPrincipal principal, CreateReportRequest request) {
        return create(principal, request, List.of());
    }

    @Transactional
    public ReportReceiptDto create(AccountPrincipal principal, CreateReportRequest request,
                                   List<MultipartFile> evidenceFiles) {
        if (request == null || request.reportedUserId() == null
                || request.reason() == null || request.reason().isBlank()
                || request.reason().trim().length() > 60
                || request.details() == null || request.details().trim().length() < 10
                || request.details().trim().length() > 1000
                || (request.contentType() != null && request.contentType().trim().length() > 30)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPORT",
                    "Provide a reason and an explanation between 10 and 1000 characters");
        }
        if (principal.userId().equals(request.reportedUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPORT", "You cannot report your own account");
        }
        UserAccount reporter = findUser(principal.userId());
        UserAccount reportedUser = findUser(request.reportedUserId());
        String type = request.contentType() == null ? null : request.contentType().trim().toUpperCase();
        Long contentId = request.contentId();
        String snapshot = resolveContentSnapshot(type, contentId, reporter, reportedUser);
        if ((type == null) != (contentId == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_CONTENT",
                    "Content type and content ID must be supplied together");
        }
        UserReport report = reportRepository.save(new UserReport(
                reporter, reportedUser, request.reason().trim(), blankToNull(request.details()),
                type, contentId, snapshot));
        if (!blockRepository.existsByBlockerIdAndBlockedId(reporter.getId(), reportedUser.getId())) {
            blockRepository.save(new UserBlock(reporter, reportedUser));
        }
        followRepository.deleteByFollowerIdAndFollowedId(reporter.getId(), reportedUser.getId());
        followRepository.deleteByFollowerIdAndFollowedId(reportedUser.getId(), reporter.getId());
        if (evidenceFiles.size() > 3 || evidenceFiles.stream().anyMatch(MultipartFile::isEmpty)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_EVIDENCE",
                    "Attach up to 3 non-empty evidence files");
        }
        List<StoredEvidence> storedFiles = new ArrayList<>();
        try {
            for (MultipartFile file : evidenceFiles) {
                StoredEvidence stored = evidenceStorage.store(file);
                storedFiles.add(stored);
                evidenceRepository.save(new ReportEvidence(report, stored.fileName(), stored.originalName(),
                        stored.contentType(), stored.fileSize()));
            }
        } catch (RuntimeException exception) {
            storedFiles.forEach(file -> evidenceStorage.delete(file.fileName()));
            throw exception;
        }
        return new ReportReceiptDto(report.getId(), report.getStatus(), report.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ReportEvidenceDto> evidence(Long reportId, AccountPrincipal principal) {
        UserReport report = findReport(reportId);
        ensureEvidenceAccess(report, principal);
        return evidenceRepository.findAllByReportIds(List.of(reportId)).stream()
                .map(item -> new ReportEvidenceDto(item.getId(), item.getOriginalName(),
                        item.getContentType(), item.getFileSize()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource evidenceFile(Long reportId, Long evidenceId, AccountPrincipal principal) {
        UserReport report = findReport(reportId);
        ensureEvidenceAccess(report, principal);
        ReportEvidence evidence = evidenceRepository.findByIdAndReportId(evidenceId, reportId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "EVIDENCE_NOT_FOUND", "Report evidence was not found"));
        return evidenceStorage.load(evidence.getFileName());
    }

    @Transactional(readOnly = true)
    public List<ReportEvidenceDto> evidenceForReports(List<Long> reportIds) {
        if (reportIds.isEmpty()) return List.of();
        return evidenceRepository.findAllByReportIds(reportIds).stream()
                .map(item -> new ReportEvidenceDto(item.getId(), item.getOriginalName(),
                        item.getContentType(), item.getFileSize()))
                .toList();
    }

    private String resolveContentSnapshot(
            String type, Long contentId, UserAccount reporter, UserAccount reportedUser) {
        if (type == null || contentId == null) {
            return null;
        }
        return switch (type) {
            case "MESSAGE" -> messageRepository.findById(contentId)
                    .filter(message -> message.getConversation().hasParticipant(reporter.getId()))
                    .filter(message -> message.getSender().getId().equals(reportedUser.getId()))
                    .map(DirectMessage::getText)
                    .orElseThrow(() -> invalidContent());
            case "POST" -> postRepository.findById(contentId)
                    .filter(post -> post.getAuthor().getId().equals(reportedUser.getId()))
                    .map(Post::getCaption)
                    .orElseThrow(() -> invalidContent());
            case "COMMENT" -> commentRepository.findById(contentId)
                    .filter(comment -> comment.getAuthor().getId().equals(reportedUser.getId()))
                    .map(Comment::getText)
                    .orElseThrow(() -> invalidContent());
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_CONTENT",
                    "Content type must be MESSAGE, POST, or COMMENT");
        };
    }

    private UserAccount findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
    }

    private UserReport findReport(Long id) {
        return reportRepository.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Report was not found"));
    }

    private void ensureEvidenceAccess(UserReport report, AccountPrincipal principal) {
        if (!principal.administrator() && !report.getReporter().getId().equals(principal.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Only the report creator and administrators can access report evidence");
        }
    }

    private ApiException invalidContent() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_CONTENT",
                "The reported content does not belong to the selected account or is unavailable");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
