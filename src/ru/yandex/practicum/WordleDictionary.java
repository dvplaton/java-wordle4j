package ru.yandex.practicum;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WordleDictionary {

    public enum LetterMatch {
        CORRECT,   // 🟩 буква на правильном месте
        PRESENT,   // 🟨 буква есть, но не здесь
        ABSENT     // ⬜ буквы нет в слове
    }

    private final List<String> words;
    private final Set<String> wordSet; // для O(1) поиска
    private final PrintWriter logWriter;
    private final Random random;

    public WordleDictionary(List<String> words, PrintWriter logWriter) {
        this.words = new ArrayList<>(words);
        this.wordSet = new HashSet<>(words);
        this.logWriter = logWriter;
        this.random = new Random();
        log("Словарь создан, количество слов: " + words.size());
    }

    // проверка наличия слова
    public boolean contains(String word) {
        return wordSet.contains(word.toLowerCase());
    }

    public int size() {
        return words.size();
    }

    public boolean isEmpty() {
        return words.isEmpty();
    }

    public String get(int index) {
        return words.get(index);
    }

    public List<String> getWords() {
        return Collections.unmodifiableList(words);
    }

    public String getRandomWord() {
        if (words.isEmpty()) {
            throw new RuntimeException("Словарь пуст — невозможно выбрать слово");
        }
        String word = words.get(random.nextInt(words.size()));
        log("Выбрано случайное слово: " + word);
        return word;
    }

    // сравнение
    public LetterMatch[] compareWords(String guess, String secret) {
        if (guess == null || secret == null) {
            throw new RuntimeException("compareWords: аргументы не могут быть null");
        }
        if (guess.length() != secret.length()) {
            throw new RuntimeException("compareWords: длины слов не совпадают: "
                    + guess.length() + " vs " + secret.length());
        }

        int length = secret.length();
        LetterMatch[] result = new LetterMatch[length];

        char[] guessChars = guess.toLowerCase().toCharArray();
        char[] secretChars = secret.toLowerCase().toCharArray();

        // подсчёт оставшихся букв
        int[] remainingCount = new int[Character.MAX_VALUE];

        // точные совпадения
        for (int i = 0; i < length; i++) {
            if (guessChars[i] == secretChars[i]) {
                result[i] = LetterMatch.CORRECT;
            } else {
                remainingCount[secretChars[i]]++;
            }
        }

        // PRESENT или ABSENT
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

    private void log(String message) {
        if (logWriter != null) {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logWriter.println("[" + timestamp + "] [Dictionary] " + message);
            logWriter.flush();
        }
    }
}