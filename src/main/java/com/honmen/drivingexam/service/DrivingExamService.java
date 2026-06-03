package com.honmen.drivingexam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honmen.drivingexam.dto.AuthDtos.AuthResponse;
import com.honmen.drivingexam.dto.BusinessDtos.ErrorQuestion;
import com.honmen.drivingexam.dto.BusinessDtos.ExamSubmitRequest;
import com.honmen.drivingexam.dto.BusinessDtos.FavoriteQuestion;
import com.honmen.drivingexam.dto.BusinessDtos.PracticeQuestionStatus;
import com.honmen.drivingexam.dto.BusinessDtos.ProgressRequest;
import com.honmen.drivingexam.dto.BusinessDtos.StatsOverview;
import com.honmen.drivingexam.dto.PageResult;
import com.honmen.drivingexam.exception.ApiException;
import com.honmen.drivingexam.model.ExamHistory;
import com.honmen.drivingexam.model.PracticeProgress;
import com.honmen.drivingexam.model.PracticeRecord;
import com.honmen.drivingexam.model.Question;
import com.honmen.drivingexam.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static com.honmen.drivingexam.dto.BusinessDtos.SubjectOverview;

@Service
public class DrivingExamService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, Long> tokenToUserId = new ConcurrentHashMap<>();

    private final RowMapper<Question> questionMapper = (rs, rowNum) -> new Question(
        rs.getLong("id"),
        rs.getInt("subject"),
        rs.getString("type"),
        rs.getString("title"),
        rs.getString("option_a"),
        rs.getString("option_b"),
        rs.getString("option_c"),
        rs.getString("option_d"),
        rs.getString("answer"),
        rs.getString("description"),
        rs.getString("image"),
        rs.getString("video")
    );

    public DrivingExamService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        ensureDefaultUser();
        importQuestionsIfEmpty();
    }

    public AuthResponse register(String username, String password) {
        String nickname = "Mock User";
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO `user` (username, password_hash, nickname) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                statement.setString(1, username);
                statement.setString(2, password);
                statement.setString(3, nickname);
                return statement;
            }, keyHolder);
            long userId = Optional.ofNullable(keyHolder.getKey()).map(Number::longValue)
                .orElseGet(() -> getUserByUsername(username).id());
            return issueToken(new User(userId, username, password, nickname));
        } catch (DuplicateKeyException ex) {
            throw new ApiException(400, "Username already exists");
        }
    }

    public AuthResponse login(String username, String password) {
        Optional<User> matched = findUserByUsername(username)
            .filter(user -> user.password().equals(password));
        return issueToken(matched.orElseGet(this::getDefaultUser));
    }

    public long requireUserId(String authorization) {
        String token = Optional.ofNullable(authorization)
            .filter(value -> value.startsWith("Bearer "))
            .map(value -> value.substring("Bearer ".length()))
            .orElse("");
        if (token.isBlank() || "mock-token-abc".equals(token)) {
            return getDefaultUser().id();
        }
        return Optional.ofNullable(tokenToUserId.get(token)).orElseGet(() -> getDefaultUser().id());
    }

    public PageResult<Question> listQuestions(int subject, String type, int page, int limit) {
        validateSubject(subject);
        int safePage = Math.max(page, 1);
        int safeLimit = normalizeLimit(limit);
        int offset = (safePage - 1) * safeLimit;

        String countSql = "SELECT COUNT(*) FROM question WHERE subject = ?";
        String listSql = "SELECT * FROM question WHERE subject = ? ORDER BY id LIMIT ? OFFSET ?";
        Object[] countArgs = {subject};
        Object[] listArgs = {subject, safeLimit, offset};

        if (type != null && !type.isBlank()) {
            countSql = "SELECT COUNT(*) FROM question WHERE subject = ? AND type = ?";
            listSql = "SELECT * FROM question WHERE subject = ? AND type = ? ORDER BY id LIMIT ? OFFSET ?";
            countArgs = new Object[] {subject, type};
            listArgs = new Object[] {subject, type, safeLimit, offset};
        }

        long total = Optional.ofNullable(jdbc.queryForObject(countSql, Long.class, countArgs)).orElse(0L);
        List<Question> list = jdbc.query(listSql, questionMapper, listArgs);
        return new PageResult<>(total, safePage, safeLimit, list);
    }

    public Question getQuestion(long id) {
        return findQuestion(id).orElseThrow(() -> new ApiException(400, "Question not found"));
    }

    public List<Question> randomQuestions(int subject, int limit) {
        validateSubject(subject);
        return jdbc.query(
            "SELECT * FROM question WHERE subject = ? ORDER BY RAND() LIMIT ?",
            questionMapper,
            subject,
            normalizeLimit(limit)
        );
    }

    public List<Question> batchQuestions(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        List<Question> list = new ArrayList<>();
        for (String item : ids.split(",")) {
            if (!item.isBlank()) {
                findQuestion(Long.parseLong(item.trim())).ifPresent(list::add);
            }
        }
        return list;
    }

    public PageResult<ErrorQuestion> listErrors(long userId, int subject, Integer isMastered, int page, int limit) {
        validateSubject(subject);
        int safePage = Math.max(page, 1);
        int safeLimit = normalizeLimit(limit);
        int offset = (safePage - 1) * safeLimit;

        String countSql = "SELECT COUNT(*) FROM user_error_book WHERE user_id = ? AND subject = ?";
        String listSql = """
            SELECT q.*, e.error_count, e.latest_wrong_answer, e.is_mastered, e.updated_at
            FROM user_error_book e
            JOIN question q ON q.id = e.question_id
            WHERE e.user_id = ? AND e.subject = ?
            ORDER BY e.updated_at DESC
            LIMIT ? OFFSET ?
            """;
        Object[] countArgs = {userId, subject};
        Object[] listArgs = {userId, subject, safeLimit, offset};

        if (isMastered != null) {
            countSql += " AND is_mastered = ?";
            listSql = """
                SELECT q.*, e.error_count, e.latest_wrong_answer, e.is_mastered, e.updated_at
                FROM user_error_book e
                JOIN question q ON q.id = e.question_id
                WHERE e.user_id = ? AND e.subject = ? AND e.is_mastered = ?
                ORDER BY e.updated_at DESC
                LIMIT ? OFFSET ?
                """;
            countArgs = new Object[] {userId, subject, isMastered};
            listArgs = new Object[] {userId, subject, isMastered, safeLimit, offset};
        }

        long total = Optional.ofNullable(jdbc.queryForObject(countSql, Long.class, countArgs)).orElse(0L);
        List<ErrorQuestion> list = jdbc.query(listSql, this::mapErrorQuestion, listArgs);
        return new PageResult<>(total, safePage, safeLimit, list);
    }

    public ErrorQuestion recordError(long userId, long questionId, int subject, String wrongAnswer) {
        validateQuestionSubject(questionId, subject);
        jdbc.update("""
            INSERT INTO user_error_book (user_id, question_id, subject, latest_wrong_answer, error_count, is_mastered)
            VALUES (?, ?, ?, ?, 1, 0)
            ON DUPLICATE KEY UPDATE
                latest_wrong_answer = VALUES(latest_wrong_answer),
                error_count = error_count + 1,
                is_mastered = 0
            """, userId, questionId, subject, wrongAnswer);
        return getErrorQuestion(userId, questionId);
    }

    public ErrorQuestion markErrorMastered(long userId, long questionId) {
        int updated = jdbc.update(
            "UPDATE user_error_book SET is_mastered = 1 WHERE user_id = ? AND question_id = ?",
            userId,
            questionId
        );
        if (updated == 0) {
            throw new ApiException(400, "Error question not found");
        }
        return getErrorQuestion(userId, questionId);
    }

    public FavoriteQuestion addFavorite(long userId, long questionId, int subject) {
        validateQuestionSubject(questionId, subject);
        jdbc.update("""
            INSERT INTO user_favorite (user_id, question_id, subject)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE subject = VALUES(subject)
            """, userId, questionId, subject);
        return getFavoriteQuestion(userId, questionId);
    }

    public void removeFavorite(long userId, long questionId) {
        jdbc.update("DELETE FROM user_favorite WHERE user_id = ? AND question_id = ?", userId, questionId);
    }

    public PageResult<FavoriteQuestion> listFavorites(long userId, int subject, int page, int limit) {
        validateSubject(subject);
        int safePage = Math.max(page, 1);
        int safeLimit = normalizeLimit(limit);
        int offset = (safePage - 1) * safeLimit;
        long total = Optional.ofNullable(jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_favorite WHERE user_id = ? AND subject = ?",
            Long.class,
            userId,
            subject
        )).orElse(0L);
        List<FavoriteQuestion> list = jdbc.query("""
            SELECT q.*, f.created_at
            FROM user_favorite f
            JOIN question q ON q.id = f.question_id
            WHERE f.user_id = ? AND f.subject = ?
            ORDER BY f.created_at DESC
            LIMIT ? OFFSET ?
            """, this::mapFavoriteQuestion, userId, subject, safeLimit, offset);
        return new PageResult<>(total, safePage, safeLimit, list);
    }

    public List<Question> examPaper(int subject) {
        int limit = subject == 1 ? 100 : 50;
        return randomQuestions(subject, limit);
    }

    public ExamHistory submitExam(long userId, ExamSubmitRequest request) {
        validateSubject(request.subject());
        List<Long> wrongIds = request.wrongQuestionIds() == null ? List.of() : List.copyOf(request.wrongQuestionIds());
        wrongIds.stream()
            .filter(this::questionExists)
            .forEach(questionId -> recordError(userId, questionId, request.subject(), ""));
        String wrongIdsJson = toJson(wrongIds);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO mock_exam_history (user_id, subject, score, time_used, is_passed, wrong_question_ids)
                VALUES (?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setInt(2, request.subject());
            statement.setInt(3, request.score());
            statement.setInt(4, request.timeUsed());
            statement.setInt(5, request.isPassed());
            statement.setString(6, wrongIdsJson);
            return statement;
        }, keyHolder);
        long id = Optional.ofNullable(keyHolder.getKey()).map(Number::longValue).orElse(0L);
        return getExamHistory(id);
    }

    public List<ExamHistory> examHistory(long userId, int subject) {
        validateSubject(subject);
        return jdbc.query("""
            SELECT * FROM mock_exam_history
            WHERE user_id = ? AND subject = ?
            ORDER BY created_at DESC
            """, this::mapExamHistory, userId, subject);
    }

    @Transactional
    public PracticeProgress syncProgress(long userId, ProgressRequest request) {
        int subject = request.subject();
        long lastQuestionId = request.lastQuestionId();
        int answeredDelta = request.answeredDelta();
        int correctDelta = request.correctDelta();
        int wrongDelta = request.wrongDelta();

        validateSubject(subject);
        Optional<PracticeRecordUpsert> practiceRecordUpdate = resolvePracticeRecordUpdate(request);
        practiceRecordUpdate.ifPresent(update -> validateQuestionSubject(update.questionId(), subject));

        jdbc.update("""
            INSERT INTO user_practice_progress
                (user_id, subject, last_question_id, total_answered, total_correct, total_wrong)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                last_question_id = VALUES(last_question_id),
                total_answered = total_answered + VALUES(total_answered),
                total_correct = total_correct + VALUES(total_correct),
                total_wrong = total_wrong + VALUES(total_wrong)
            """,
            userId,
            subject,
            lastQuestionId,
            Math.max(answeredDelta, 0),
            Math.max(correctDelta, 0),
            Math.max(wrongDelta, 0)
        );

        practiceRecordUpdate.ifPresent(update ->
            upsertPracticeRecord(userId, update.questionId(), subject, request.latestAnswer(), update.latestResult())
        );
        return buildPracticeProgress(userId, subject);
    }

    public StatsOverview statsOverview(long userId) {
        return new StatsOverview(subjectOverview(userId, 1), subjectOverview(userId, 4));
    }

    public List<PracticeQuestionStatus> listPracticeStatuses(long userId, int subject, String questionIds) {
        validateSubject(subject);
        List<Long> parsedQuestionIds = parseQuestionIds(questionIds);
        if (parsedQuestionIds.isEmpty()) {
            return jdbc.query("""
                SELECT question_id, latest_result
                FROM user_practice_record
                WHERE user_id = ? AND subject = ?
                ORDER BY question_id
                """, (rs, rowNum) -> new PracticeQuestionStatus(
                rs.getLong("question_id"),
                true,
                rs.getInt("latest_result") == 1
            ), userId, subject);
        }

        Map<Long, PracticeRecord> recordByQuestionId = listPracticeRecords(userId, subject, parsedQuestionIds).stream()
            .collect(Collectors.toMap(PracticeRecord::getQuestionId, record -> record));

        List<PracticeQuestionStatus> statuses = new ArrayList<>(parsedQuestionIds.size());
        for (Long questionId : parsedQuestionIds) {
            PracticeRecord record = recordByQuestionId.get(questionId);
            statuses.add(new PracticeQuestionStatus(
                questionId,
                record != null,
                record != null && record.getLatestResult() == 1
            ));
        }
        return statuses;
    }

    private AuthResponse issueToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenToUserId.put(token, user.id());
        return new AuthResponse(token, user.id(), user.nickname());
    }

    private void ensureDefaultUser() {
        if (findUserByUsername("13800000000").isEmpty()) {
            register("13800000000", "123456");
        }
    }

    private User getDefaultUser() {
        return findUserByUsername("13800000000")
            .or(() -> firstUser())
            .orElseThrow(() -> new ApiException(401, "Missing token"));
    }

    private Optional<User> firstUser() {
        return queryOne("SELECT * FROM `user` ORDER BY id LIMIT 1", this::mapUser);
    }

    private User getUserByUsername(String username) {
        return findUserByUsername(username).orElseThrow(() -> new ApiException(400, "User not found"));
    }

    private Optional<User> findUserByUsername(String username) {
        return queryOne("SELECT * FROM `user` WHERE username = ?", this::mapUser, username);
    }

    private User mapUser(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("nickname")
        );
    }

    private SubjectOverview subjectOverview(long userId, int subject) {
        PracticeProgress progress = buildPracticeProgress(userId, subject);
        String accuracy = progress.getTotalAnswered() == 0
            ? "0.0%"
            : String.format("%.1f%%", progress.getTotalCorrect() * 100.0 / progress.getTotalAnswered());
        return new SubjectOverview(
            progress.getTotalAnswered(),
            progress.getTotalCorrect(),
            progress.getTotalWrong(),
            accuracy,
            progress.getLastQuestionId()
        );
    }

    private PracticeProgress buildPracticeProgress(long userId, int subject) {
        PracticeProgress storedProgress = getStoredPracticeProgress(userId, subject);
        PracticeRecordStats recordStats = getPracticeRecordStats(userId, subject);
        if (recordStats.totalAnswered() == 0) {
            return storedProgress;
        }

        PracticeProgress progress = new PracticeProgress(userId, subject);
        progress.sync(
            resolveResumeQuestionId(userId, subject, storedProgress.getLastQuestionId()),
            recordStats.totalAnswered(),
            recordStats.totalCorrect(),
            recordStats.totalWrong()
        );
        return progress;
    }

    private PracticeProgress getStoredPracticeProgress(long userId, int subject) {
        return queryOne(
            "SELECT * FROM user_practice_progress WHERE user_id = ? AND subject = ?",
            (rs, rowNum) -> {
                PracticeProgress progress = new PracticeProgress(userId, subject);
                progress.sync(
                    rs.getLong("last_question_id"),
                    rs.getInt("total_answered"),
                    rs.getInt("total_correct"),
                    rs.getInt("total_wrong")
                );
                return progress;
            },
            userId,
            subject
        ).orElseGet(() -> new PracticeProgress(userId, subject));
    }

    private Optional<PracticeRecord> findPracticeRecord(long userId, long questionId) {
        return queryOne("""
            SELECT * FROM user_practice_record
            WHERE user_id = ? AND question_id = ?
            """, this::mapPracticeRecord, userId, questionId);
    }

    private List<PracticeRecord> listPracticeRecords(long userId, int subject, List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(", ", Collections.nCopies(questionIds.size(), "?"));
        List<Object> args = new ArrayList<>(questionIds.size() + 2);
        args.add(userId);
        args.add(subject);
        args.addAll(questionIds);

        return jdbc.query("""
            SELECT * FROM user_practice_record
            WHERE user_id = ? AND subject = ? AND question_id IN (%s)
            """.formatted(placeholders), this::mapPracticeRecord, args.toArray());
    }

    private PracticeRecordStats getPracticeRecordStats(long userId, int subject) {
        return queryOne("""
            SELECT
                COUNT(*) AS total_answered,
                COALESCE(SUM(CASE WHEN latest_result = 1 THEN 1 ELSE 0 END), 0) AS total_correct,
                COALESCE(SUM(CASE WHEN latest_result = 0 THEN 1 ELSE 0 END), 0) AS total_wrong
            FROM user_practice_record
            WHERE user_id = ? AND subject = ?
            """, (rs, rowNum) -> new PracticeRecordStats(
            rs.getInt("total_answered"),
            rs.getInt("total_correct"),
            rs.getInt("total_wrong")
        ), userId, subject).orElseGet(() -> new PracticeRecordStats(0, 0, 0));
    }

    private long resolveResumeQuestionId(long userId, int subject, long fallbackQuestionId) {
        Optional<Long> firstUnanswered = queryOne("""
            SELECT q.id
            FROM question q
            LEFT JOIN user_practice_record r
                ON r.question_id = q.id AND r.user_id = ?
            WHERE q.subject = ? AND r.id IS NULL
            ORDER BY q.id
            LIMIT 1
            """, (rs, rowNum) -> rs.getLong("id"), userId, subject);
        if (firstUnanswered.isPresent()) {
            return firstUnanswered.get();
        }

        return queryOne("""
            SELECT question_id
            FROM user_practice_record
            WHERE user_id = ? AND subject = ?
            ORDER BY answered_at DESC, updated_at DESC, question_id DESC
            LIMIT 1
            """, (rs, rowNum) -> rs.getLong("question_id"), userId, subject)
            .orElse(fallbackQuestionId);
    }

    private Optional<PracticeRecordUpsert> resolvePracticeRecordUpdate(ProgressRequest request) {
        Integer latestResult = normalizeLatestResult(request.latestResult(), request.answeredDelta(), request.correctDelta(), request.wrongDelta());
        if (latestResult == null) {
            return Optional.empty();
        }

        long questionId = Optional.ofNullable(request.questionId()).orElse(request.lastQuestionId());
        if (questionId <= 0) {
            return Optional.empty();
        }
        return Optional.of(new PracticeRecordUpsert(questionId, latestResult));
    }

    private Integer normalizeLatestResult(Integer latestResult, int answeredDelta, int correctDelta, int wrongDelta) {
        if (latestResult != null) {
            if (latestResult != 0 && latestResult != 1) {
                throw new ApiException(400, "latestResult must be 0 or 1");
            }
            return latestResult;
        }
        if (answeredDelta <= 0) {
            return null;
        }
        if (correctDelta > 0 && wrongDelta == 0) {
            return 1;
        }
        if (wrongDelta > 0 && correctDelta == 0) {
            return 0;
        }
        return null;
    }

    private void upsertPracticeRecord(long userId, long questionId, int subject, String latestAnswer, int latestResult) {
        jdbc.update("""
            INSERT INTO user_practice_record
                (user_id, question_id, subject, latest_answer, latest_result, answered_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                subject = VALUES(subject),
                latest_answer = COALESCE(VALUES(latest_answer), latest_answer),
                latest_result = VALUES(latest_result),
                answered_at = VALUES(answered_at)
            """, userId, questionId, subject, latestAnswer, latestResult);
    }

    private PracticeRecord mapPracticeRecord(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new PracticeRecord(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("question_id"),
            rs.getInt("subject"),
            rs.getString("latest_answer"),
            rs.getInt("latest_result"),
            toLocalDateTime(rs.getTimestamp("answered_at")),
            toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private ErrorQuestion getErrorQuestion(long userId, long questionId) {
        return queryOne("""
            SELECT q.*, e.error_count, e.latest_wrong_answer, e.is_mastered, e.updated_at
            FROM user_error_book e
            JOIN question q ON q.id = e.question_id
            WHERE e.user_id = ? AND e.question_id = ?
            """, this::mapErrorQuestion, userId, questionId)
            .orElseThrow(() -> new ApiException(400, "Error question not found"));
    }

    private ErrorQuestion mapErrorQuestion(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ErrorQuestion(
            questionMapper.mapRow(rs, rowNum),
            rs.getInt("error_count"),
            rs.getString("latest_wrong_answer"),
            rs.getInt("is_mastered"),
            toLocalDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private FavoriteQuestion getFavoriteQuestion(long userId, long questionId) {
        return queryOne("""
            SELECT q.*, f.created_at
            FROM user_favorite f
            JOIN question q ON q.id = f.question_id
            WHERE f.user_id = ? AND f.question_id = ?
            """, this::mapFavoriteQuestion, userId, questionId)
            .orElseThrow(() -> new ApiException(400, "Favorite question not found"));
    }

    private FavoriteQuestion mapFavoriteQuestion(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new FavoriteQuestion(
            questionMapper.mapRow(rs, rowNum),
            toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private ExamHistory getExamHistory(long id) {
        return queryOne("SELECT * FROM mock_exam_history WHERE id = ?", this::mapExamHistory, id)
            .orElseThrow(() -> new ApiException(400, "Exam history not found"));
    }

    private ExamHistory mapExamHistory(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ExamHistory(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getInt("subject"),
            rs.getInt("score"),
            rs.getInt("time_used"),
            rs.getInt("is_passed"),
            parseLongList(rs.getString("wrong_question_ids")),
            toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private Optional<Question> findQuestion(long id) {
        return queryOne("SELECT * FROM question WHERE id = ?", questionMapper, id);
    }

    private boolean questionExists(long id) {
        return Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM question WHERE id = ?", Long.class, id)).orElse(0L) > 0;
    }

    private List<Long> parseQuestionIds(String questionIds) {
        if (questionIds == null || questionIds.isBlank()) {
            return List.of();
        }

        LinkedHashSet<Long> parsed = new LinkedHashSet<>();
        for (String item : questionIds.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                long questionId = Long.parseLong(trimmed);
                if (questionId <= 0) {
                    throw new ApiException(400, "questionIds must contain positive integers");
                }
                parsed.add(questionId);
            } catch (NumberFormatException ex) {
                throw new ApiException(400, "questionIds must be a comma-separated list of integers");
            }
        }
        return List.copyOf(parsed);
    }

    private int normalizeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 200);
    }

    private void validateSubject(int subject) {
        if (subject != 1 && subject != 4) {
            throw new ApiException(400, "subject must be 1 or 4");
        }
    }

    private void validateQuestionSubject(long questionId, int subject) {
        Question question = getQuestion(questionId);
        if (question.subject() != subject) {
            throw new ApiException(400, "Question subject mismatch");
        }
    }

    private void importQuestionsIfEmpty() {
        long count = Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM question", Long.class)).orElse(0L);
        if (count > 0) {
            return;
        }
        Path dataRoot = resolveDataRoot();
        try {
            long nextId = 1;
            nextId = importSubject(dataRoot.resolve("subject1").resolve("questions.json"), nextId);
            importSubject(dataRoot.resolve("subject4").resolve("questions.json"), nextId);
        } catch (IOException ex) {
            throw new ApiException(500, "Failed to import mock question data: " + ex.getMessage());
        }
    }

    private long importSubject(Path file, long nextId) throws IOException {
        JsonNode root = objectMapper.readTree(file.toFile());
        JsonNode questionNode = root.has("questions") ? root.get("questions") : root;
        List<Question> loaded = objectMapper.convertValue(questionNode, new TypeReference<List<Question>>() {});
        for (Question question : loaded) {
            long id = question.id() > 0 ? question.id() : nextId;
            jdbc.update("""
                INSERT INTO question
                    (id, subject, type, title, option_a, option_b, option_c, option_d, answer, description, image, video)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                question.subject(),
                question.type(),
                question.title(),
                question.optionA(),
                question.optionB(),
                question.optionC(),
                question.optionD(),
                question.answer(),
                question.description(),
                question.image(),
                question.video()
            );
            nextId = Math.max(nextId, id + 1);
        }
        return nextId;
    }

    private Path resolveDataRoot() {
        Path current = Paths.get("").toAbsolutePath();
        for (Path candidate : new Path[] {current, current.getParent()}) {
            if (candidate != null
                && Files.exists(candidate.resolve("subject1").resolve("questions.json"))
                && Files.exists(candidate.resolve("subject4").resolve("questions.json"))) {
                return candidate;
            }
        }
        throw new ApiException(500, "Cannot find subject1/subject4 mock data directories");
    }

    private String toJson(List<Long> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new ApiException(500, "Failed to serialize exam history");
        }
    }

    private List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... args) {
        List<T> list = jdbc.query(sql, mapper, args);
        return list.stream().findFirst();
    }

    private record PracticeRecordStats(int totalAnswered, int totalCorrect, int totalWrong) {
    }

    private record PracticeRecordUpsert(long questionId, int latestResult) {
    }
}
