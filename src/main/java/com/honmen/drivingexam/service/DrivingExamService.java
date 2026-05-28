package com.honmen.drivingexam.service;

import com.honmen.drivingexam.dto.AuthDtos.AuthResponse;
import com.honmen.drivingexam.dto.BusinessDtos.ErrorQuestion;
import com.honmen.drivingexam.dto.BusinessDtos.ExamSubmitRequest;
import com.honmen.drivingexam.dto.BusinessDtos.FavoriteQuestion;
import com.honmen.drivingexam.dto.BusinessDtos.StatsOverview;
import com.honmen.drivingexam.dto.PageResult;
import com.honmen.drivingexam.exception.ApiException;
import com.honmen.drivingexam.model.ErrorEntry;
import com.honmen.drivingexam.model.ExamHistory;
import com.honmen.drivingexam.model.FavoriteEntry;
import com.honmen.drivingexam.model.PracticeProgress;
import com.honmen.drivingexam.model.Question;
import com.honmen.drivingexam.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.honmen.drivingexam.dto.BusinessDtos.SubjectOverview;

@Service
public class DrivingExamService {
    private final AtomicLong userIdGenerator = new AtomicLong(1);
    private final AtomicLong historyIdGenerator = new AtomicLong(1);
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();
    private final Map<Long, Question> questions = new ConcurrentHashMap<>();
    private final Map<String, ErrorEntry> errors = new ConcurrentHashMap<>();
    private final Map<String, FavoriteEntry> favorites = new ConcurrentHashMap<>();
    private final Map<Long, ExamHistory> histories = new ConcurrentHashMap<>();
    private final Map<String, PracticeProgress> progresses = new ConcurrentHashMap<>();

    public DrivingExamService() {
        register("13800000000", "123456");
        seedQuestions();
    }

    public AuthResponse register(String username, String password) {
        if (usersByUsername.containsKey(username)) {
            throw new ApiException(400, "Username already exists");
        }
        long userId = userIdGenerator.getAndIncrement();
        User user = new User(userId, username, password, "驾考新星" + userId);
        usersByUsername.put(username, user);
        return issueToken(user);
    }

    public AuthResponse login(String username, String password) {
        User user = Optional.ofNullable(usersByUsername.get(username))
            .filter(value -> Objects.equals(value.password(), password))
            .orElseThrow(() -> new ApiException(400, "Invalid username or password"));
        return issueToken(user);
    }

    public long requireUserId(String authorization) {
        String token = Optional.ofNullable(authorization)
            .filter(value -> value.startsWith("Bearer "))
            .map(value -> value.substring("Bearer ".length()))
            .orElseThrow(() -> new ApiException(401, "Missing token"));
        return Optional.ofNullable(tokenToUserId.get(token))
            .orElseThrow(() -> new ApiException(401, "Invalid token"));
    }

    public PageResult<Question> listQuestions(int subject, String type, int page, int limit) {
        validateSubject(subject);
        List<Question> filtered = questions.values().stream()
            .filter(question -> question.subject() == subject)
            .filter(question -> type == null || type.isBlank() || question.type().equalsIgnoreCase(type))
            .sorted(Comparator.comparingLong(Question::id))
            .toList();
        return page(filtered, page, limit);
    }

    public Question getQuestion(long id) {
        return Optional.ofNullable(questions.get(id))
            .orElseThrow(() -> new ApiException(400, "Question not found"));
    }

    public List<Question> randomQuestions(int subject, int limit) {
        validateSubject(subject);
        List<Question> list = questions.values().stream()
            .filter(question -> question.subject() == subject)
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(list);
        return list.stream().limit(normalizeLimit(limit)).toList();
    }

    public List<Question> batchQuestions(String ids) {
        if (ids == null || ids.isBlank()) {
            throw new ApiException(400, "ids is required");
        }
        List<Question> list = new ArrayList<>();
        for (String item : ids.split(",")) {
            if (!item.isBlank()) {
                list.add(getQuestion(Long.parseLong(item.trim())));
            }
        }
        return list;
    }

    public PageResult<ErrorQuestion> listErrors(long userId, int subject, Integer isMastered, int page, int limit) {
        validateSubject(subject);
        List<ErrorQuestion> list = errors.values().stream()
            .filter(error -> error.getUserId() == userId)
            .filter(error -> error.getSubject() == subject)
            .filter(error -> isMastered == null || (error.isMastered() ? 1 : 0) == isMastered)
            .sorted(Comparator.comparing(ErrorEntry::getUpdatedAt).reversed())
            .map(error -> new ErrorQuestion(
                getQuestion(error.getQuestionId()),
                error.getErrorCount(),
                error.getLatestWrongAnswer(),
                error.isMastered() ? 1 : 0,
                error.getUpdatedAt()
            ))
            .toList();
        return page(list, page, limit);
    }

    public ErrorQuestion recordError(long userId, long questionId, int subject, String wrongAnswer) {
        validateQuestionSubject(questionId, subject);
        ErrorEntry entry = errors.compute(errorKey(userId, questionId), (key, existing) -> {
            if (existing == null) {
                return new ErrorEntry(userId, questionId, subject, wrongAnswer);
            }
            existing.recordWrongAnswer(wrongAnswer);
            return existing;
        });
        return new ErrorQuestion(getQuestion(questionId), entry.getErrorCount(), entry.getLatestWrongAnswer(), 0, entry.getUpdatedAt());
    }

    public ErrorQuestion markErrorMastered(long userId, long questionId) {
        ErrorEntry entry = Optional.ofNullable(errors.get(errorKey(userId, questionId)))
            .orElseThrow(() -> new ApiException(400, "Error question not found"));
        entry.markMastered();
        return new ErrorQuestion(getQuestion(questionId), entry.getErrorCount(), entry.getLatestWrongAnswer(), 1, entry.getUpdatedAt());
    }

    public FavoriteQuestion addFavorite(long userId, long questionId, int subject) {
        validateQuestionSubject(questionId, subject);
        FavoriteEntry entry = favorites.computeIfAbsent(favoriteKey(userId, questionId),
            key -> new FavoriteEntry(userId, questionId, subject, LocalDateTime.now()));
        return new FavoriteQuestion(getQuestion(questionId), entry.createdAt());
    }

    public void removeFavorite(long userId, long questionId) {
        favorites.remove(favoriteKey(userId, questionId));
    }

    public PageResult<FavoriteQuestion> listFavorites(long userId, int subject, int page, int limit) {
        validateSubject(subject);
        List<FavoriteQuestion> list = favorites.values().stream()
            .filter(favorite -> favorite.userId() == userId)
            .filter(favorite -> favorite.subject() == subject)
            .sorted(Comparator.comparing(FavoriteEntry::createdAt).reversed())
            .map(favorite -> new FavoriteQuestion(getQuestion(favorite.questionId()), favorite.createdAt()))
            .toList();
        return page(list, page, limit);
    }

    public List<Question> examPaper(int subject) {
        int limit = subject == 1 ? 100 : 50;
        return randomQuestions(subject, limit);
    }

    public ExamHistory submitExam(long userId, ExamSubmitRequest request) {
        validateSubject(request.subject());
        List<Long> wrongIds = request.wrongQuestionIds() == null ? List.of() : List.copyOf(request.wrongQuestionIds());
        wrongIds.forEach(questionId -> {
            if (questions.containsKey(questionId)) {
                recordError(userId, questionId, request.subject(), "");
            }
        });
        ExamHistory history = new ExamHistory(
            historyIdGenerator.getAndIncrement(),
            userId,
            request.subject(),
            request.score(),
            request.timeUsed(),
            request.isPassed(),
            wrongIds,
            LocalDateTime.now()
        );
        histories.put(history.id(), history);
        return history;
    }

    public List<ExamHistory> examHistory(long userId, int subject) {
        validateSubject(subject);
        return histories.values().stream()
            .filter(history -> history.userId() == userId)
            .filter(history -> history.subject() == subject)
            .sorted(Comparator.comparing(ExamHistory::createdAt).reversed())
            .toList();
    }

    public PracticeProgress syncProgress(long userId, int subject, long lastQuestionId, int answeredDelta, int correctDelta, int wrongDelta) {
        validateSubject(subject);
        PracticeProgress progress = progresses.computeIfAbsent(progressKey(userId, subject), key -> new PracticeProgress(userId, subject));
        progress.sync(lastQuestionId, answeredDelta, correctDelta, wrongDelta);
        return progress;
    }

    public StatsOverview statsOverview(long userId) {
        return new StatsOverview(subjectOverview(userId, 1), subjectOverview(userId, 4));
    }

    private AuthResponse issueToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenToUserId.put(token, user.id());
        return new AuthResponse(token, user.id(), user.nickname());
    }

    private SubjectOverview subjectOverview(long userId, int subject) {
        PracticeProgress progress = progresses.getOrDefault(progressKey(userId, subject), new PracticeProgress(userId, subject));
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

    private <T> PageResult<T> page(List<T> source, int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = normalizeLimit(limit);
        int from = Math.min((safePage - 1) * safeLimit, source.size());
        int to = Math.min(from + safeLimit, source.size());
        return new PageResult<>(source.size(), safePage, safeLimit, source.subList(from, to));
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

    private String errorKey(long userId, long questionId) {
        return userId + ":" + questionId;
    }

    private String favoriteKey(long userId, long questionId) {
        return userId + ":" + questionId;
    }

    private String progressKey(long userId, int subject) {
        return userId + ":" + subject;
    }

    private void seedQuestions() {
        seedSubject(1, 1000, 120, "科目一");
        seedSubject(4, 4000, 80, "科目四");
    }

    private void seedSubject(int subject, int baseId, int count, String subjectName) {
        String[] types = {"single", "multiple", "judge"};
        for (int i = 1; i <= count; i++) {
            String type = types[(i - 1) % types.length];
            long id = baseId + i;
            questions.put(id, new Question(
                id,
                subject,
                type,
                subjectName + "第" + i + "题：驾驶机动车遇到道路交通情况时，应当如何安全处理？",
                "减速观察，确保安全后通行",
                "加速通过，避免影响后车",
                "随意变更车道",
                "紧急制动并停在路中",
                "A",
                "安全驾驶应先观察、减速并遵守交通规则。",
                i % 5 == 0 ? "question-" + id + ".jpg" : null,
                subject == 4 && i % 7 == 0 ? "question-" + id + ".mp4" : null
            ));
        }
    }
}
