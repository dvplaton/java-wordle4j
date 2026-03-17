package ru.yandex.practicum;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WordleDictionary {

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

    private void log(String message) {
        if (logWriter != null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logWriter.println("[" + timestamp + "] [Dictionary] " + message);
            logWriter.flush();
        }
    }
}