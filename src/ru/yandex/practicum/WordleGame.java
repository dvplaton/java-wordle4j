package ru.yandex.practicum;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WordleGame {

    private static final int MAX_ATTEMPTS = 6;
    private static final int WORD_LENGTH = 5;

    public enum LetterMatch {
        CORRECT, PRESENT, ABSENT
    }

    private final String answer;
    private int steps;
    private final WordleDictionary dictionary;
    private final PrintWriter logWriter;

    // история
    private final List<String> guesses;
    private final List<LetterMatch[]> results;

    // накопленные знания
    private final Set<Character> absentLetters;
    private final char[] correctLetters;                    // '?' если неизвестно
    @SuppressWarnings("unchecked")
    private final Set<Character>[] presentNotAt = new HashSet[WORD_LENGTH];
    private final Set<Character> presentLetters;

    // уже выданные подсказки
    private final Set<String> givenHints;

    public WordleGame(WordleDictionary dictionary, PrintWriter logWriter) {
        if (dictionary == null || dictionary.isEmpty()) {
            throw new RuntimeException("Словарь не может быть null или пустым");
        }

        this.dictionary = dictionary;
        this.logWriter = logWriter;
        this.answer = dictionary.getRandomWord();
        this.steps = 0;

        this.guesses = new ArrayList<>();
        this.results = new ArrayList<>();

        this.absentLetters = new HashSet<>();
        this.correctLetters = new char[WORD_LENGTH];
        this.presentLetters = new HashSet<>();
        this.givenHints = new HashSet<>();

        for (int i = 0; i < WORD_LENGTH; i++) {
            correctLetters[i] = '?';
            presentNotAt[i] = new HashSet<>();
        }

        log("Игра создана. Ответ: " + answer);
        logState();
    }

    // сравнение
    public LetterMatch[] compareWords(String guess, String secret) {
        if (guess == null || secret == null) {
            throw new RuntimeException("compareWords: аргументы не могут быть null");
        }
        if (guess.length() != secret.length()) {
            throw new RuntimeException("compareWords: длины слов не совпадают: " + guess.length() + " vs " + secret.length());
        }

        int length = secret.length();
        LetterMatch[] result = new LetterMatch[length];

        char[] guessChars = guess.toLowerCase().toCharArray();
        char[] secretChars = secret.toLowerCase().toCharArray();

        int[] remainingCount = new int[Character.MAX_VALUE];

        for (int i = 0; i < length; i++) {
            if (guessChars[i] == secretChars[i]) {
                result[i] = LetterMatch.CORRECT;
            } else {
                remainingCount[secretChars[i]]++;
            }
        }

        for (int i = 0; i < length; i++) {
            if (result[i] == LetterMatch.CORRECT) {
                continue;
            }
            if (remainingCount[guessChars[i]] > 0) {
                result[i] = LetterMatch.PRESENT;
                remainingCount[guessChars[i]]--;
            } else {
                result[i] = LetterMatch.ABSENT;
            }
        }

        log("Сравнение: " + guess + " vs " + secret + " -> " + matchesToString(result));
        return result;
    }

    public boolean isExactMatch(String guess, String secret) {
        return guess.equalsIgnoreCase(secret);
    }

    public String matchesToString(LetterMatch[] matches) {
        StringBuilder sb = new StringBuilder(matches.length);
        for (LetterMatch match : matches) {
            switch (match) {
                case CORRECT:
                    sb.append("🟩");
                    break;
                case PRESENT:
                    sb.append("🟨");
                    break;
                case ABSENT:
                    sb.append("⬜");
                    break;
            }
        }
        return sb.toString();
    }

    public String formatGuessResult(String guess, LetterMatch[] matches) {
        StringBuilder sb = new StringBuilder(guess.length() * 6);
        for (int i = 0; i < guess.length(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(guess.charAt(i)));
            switch (matches[i]) {
                case CORRECT:
                    sb.append("[🟩]");
                    break;
                case PRESENT:
                    sb.append("[🟨]");
                    break;
                case ABSENT:
                    sb.append("[⬜]");
                    break;
            }
        }
        return sb.toString();
    }


    // trim + lowercase + ё=е
    public static String normalize(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase().replace('ё', 'е');
    }

    // только русские буквы
    public static boolean isRussianLettersOnly(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (!((c >= 'а' && c <= 'я'))) {
                return false;
            }
        }
        return true;
    }

    public void validateGuess(String normalized) throws WordleException {
        if (normalized.isEmpty()) {
            throw new EmptyInputException();
        }
        if (!isRussianLettersOnly(normalized)) {
            throw new InvalidWordContentException(normalized);
        }
        if (normalized.length() != WORD_LENGTH) {
            throw new InvalidWordLengthException(WORD_LENGTH, normalized.length());
        }
        if (!dictionary.contains(normalized)) {
            throw new WordNotFoundInDictionaryException(normalized);
        }
    }

    // основной метод
    public String makeGuess(String input) throws WordleException {
        if (isGameOver()) {
            throw new GameOverException();
        }

        String normalized = normalize(input);
        validateGuess(normalized);

        // проверка на точное совпадение
        boolean exact = isExactMatch(normalized, answer);

        LetterMatch[] matchResult = compareWords(normalized, answer);

        guesses.add(normalized);
        results.add(matchResult);
        steps++;

        updateKnowledge(normalized, matchResult);

        String formatted = formatGuessResult(normalized, matchResult);
        log("Попытка " + steps + ": " + normalized + " -> " + formatted + (exact ? " [ПОБЕДА]" : ""));
        logState();

        return formatted;
    }

    private void updateKnowledge(String guess, LetterMatch[] matchResult) {
        for (int i = 0; i < WORD_LENGTH; i++) {
            char letter = guess.charAt(i);

            switch (matchResult[i]) {
                case CORRECT:
                    correctLetters[i] = letter;
                    presentLetters.add(letter);
                    absentLetters.remove(letter);
                    break;

                case PRESENT:
                    presentLetters.add(letter);
                    presentNotAt[i].add(letter);
                    absentLetters.remove(letter);
                    break;

                case ABSENT:
                    // Только если буква не встречается как CORRECT/PRESENT
                    if (!presentLetters.contains(letter)) {
                        absentLetters.add(letter);
                    }
                    break;
            }
        }
    }

    // подсказки
    public String getHint() throws GameOverException {
        if (isGameOver()) {
            throw new GameOverException();
        }

        List<String> candidates = filterCandidates();
        log("Кандидатов для подсказки: " + candidates.size());

        if (candidates.isEmpty()) {
            // Не должно происходить — ответ всегда должен быть среди кандидатов
            throw new RuntimeException("Алгоритм потерял решение! Ответ: " + answer
                    + ", absent: " + absentLetters
                    + ", present: " + presentLetters
                    + ", correct: " + new String(correctLetters));
        }

        String hint = selectBestCandidate(candidates);
        givenHints.add(hint);

        log("Подсказка: " + hint);
        return hint;
    }

    // фильтр
    List<String> filterCandidates() {
        List<String> candidates = new ArrayList<>();

        for (String word : dictionary.getWords()) {
            if (isWordCompatible(word)) {
                candidates.add(word);
            }
        }

        return candidates;
    }

    boolean isWordCompatible(String word) {
        // не использованные и не подсказанные
        if (guesses.contains(word) || givenHints.contains(word)) {
            return false;
        }

        for (int i = 0; i < WORD_LENGTH; i++) {
            char letter = word.charAt(i);

            // буква точно отсутствует
            if (absentLetters.contains(letter)) {
                return false;
            }

            // на этой позиции должна быть конкретная буква
            if (correctLetters[i] != '?' && correctLetters[i] != letter) {
                return false;
            }

            // буква точно НЕ на этой позиции
            if (presentNotAt[i].contains(letter)) {
                return false;
            }
        }

        // все present-буквы должны присутствовать
        for (char p : presentLetters) {
            if (word.indexOf(p) < 0) {
                return false;
            }
        }

        return true;
    }

    private String selectBestCandidate(List<String> candidates) {
        Set<Character> known = new HashSet<>(absentLetters);
        known.addAll(presentLetters);

        String best = candidates.getFirst();
        int bestScore = -1;

        for (String candidate : candidates) {
            int score = 0;
            Set<Character> seen = new HashSet<>();
            for (int i = 0; i < candidate.length(); i++) {
                char c = candidate.charAt(i);
                if (seen.add(c)) score++;           // уникальная буква
                if (!known.contains(c)) score += 2; // новая буква
            }
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    // состояние
    public boolean isGameOver() {
        return isWon() || steps >= MAX_ATTEMPTS;
    }

    public boolean isWon() {
        if (guesses.isEmpty()) return false;
        return isExactMatch(guesses.getLast(), answer);
    }

    public int getCurrentAttempt() {
        return steps;
    }

    public String getSecretWord() {
        return answer;
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    public List<String> getGuessHistory() {
        return Collections.unmodifiableList(guesses);
    }

    public List<LetterMatch[]> getResults() {
        return Collections.unmodifiableList(results);
    }

    public Set<Character> getAbsentLetters() {
        return Collections.unmodifiableSet(absentLetters);
    }

    public Set<Character> getPresentLetters() {
        return Collections.unmodifiableSet(presentLetters);
    }

    public String getStateAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Wordle [").append(steps).append('/').append(MAX_ATTEMPTS).append("] ---\n");

        for (int i = 0; i < guesses.size(); i++) {
            sb.append("  ").append(i + 1).append(". ");
            sb.append(formatGuessResult(guesses.get(i), results.get(i)));
            sb.append('\n');
        }
        for (int i = guesses.size(); i < MAX_ATTEMPTS; i++) {
            sb.append("  ").append(i + 1).append(". _ _ _ _ _\n");
        }

        sb.append("  Исключены: ").append(formatSet(absentLetters)).append('\n');
        sb.append("  Найдены:   ").append(formatSet(presentLetters)).append('\n');
        sb.append("----------------------------");

        return sb.toString();
    }

    public String getResultAsString() {
        StringBuilder sb = new StringBuilder();
        if (isWon()) {
            sb.append("Поздравляем! Вы угадали слово \"").append(answer).append("\" за ").append(steps).append(" попыток!");
        } else if (steps >= MAX_ATTEMPTS) {
            sb.append("Вы проиграли. Загаданное слово: ").append(answer);
        } else {
            sb.append("Игра в процессе...");
        }
        return sb.toString();
    }

    private String formatSet(Set<Character> set) {
        if (set.isEmpty()) return "—";
        List<Character> sorted = new ArrayList<>(set);
        sorted.sort(Character::compareTo);
        StringBuilder sb = new StringBuilder();
        for (char c : sorted) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(c);
        }
        return sb.toString();
    }

    private void logState() {
        log("Состояние: steps=" + steps
                + ", absent=" + absentLetters
                + ", present=" + presentLetters
                + ", correct=" + new String(correctLetters)
                + ", guesses=" + guesses);
    }

    private void log(String message) {
        if (logWriter != null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logWriter.println("[" + timestamp + "] [Game] " + message);
            logWriter.flush();
        }
    }
}