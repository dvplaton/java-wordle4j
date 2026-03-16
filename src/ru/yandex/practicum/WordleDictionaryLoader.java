package ru.yandex.practicum;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WordleDictionaryLoader {

    private static final int EXPECTED_WORD_LENGTH = 5;
    private final PrintWriter logWriter;

    public WordleDictionaryLoader(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    public WordleDictionary load(String fileName) throws IOException {
        log("Начинаю загрузку словаря из файла: " + fileName);

        Path filePath = resolveFilePath(fileName);
        log("Путь к файлу: " + filePath.toAbsolutePath());

        Charset charset = detectCharset(filePath);
        log("Кодировка: " + charset.displayName());

        List<String> words = readAndFilterWords(filePath, charset);
        log("Загружено слов (длина " + EXPECTED_WORD_LENGTH + "): " + words.size());

        if (words.isEmpty()) {
            throw new IOException("Словарь не содержит подходящих слов длиной " + EXPECTED_WORD_LENGTH);
        }

        return new WordleDictionary(words, logWriter);
    }

    private Path resolveFilePath(String fileName) throws FileNotFoundException {
        log("Рабочая директория: " + Paths.get("").toAbsolutePath());

        Path[] candidates = {
                Paths.get(fileName),
                Paths.get("src", fileName),
                Paths.get("src", "main", "resources", fileName)
        };

        for (Path path : candidates) {
            log("Проверяю: " + path.toAbsolutePath());
            if (Files.exists(path)) {
                return path;
            }
        }

        // classpath
        var resource = getClass().getClassLoader().getResource(fileName);
        if (resource != null) {
            log("Найден в classpath: " + resource);
            try {
                return Paths.get(resource.toURI());
            } catch (Exception e) {
                throw new FileNotFoundException("Не удалось преобразовать URL ресурса: " + resource);
            }
        }

        throw new FileNotFoundException(
                "Файл \"" + fileName + "\" не найден. Рабочая директория: "
                        + Paths.get("").toAbsolutePath()
        );
    }

    private Charset detectCharset(Path filePath) {
        Charset[] charsets = {
                StandardCharsets.UTF_8,
                Charset.forName("Windows-1251"),
                StandardCharsets.ISO_8859_1
        };

        for (Charset charset : charsets) {
            if (isValidCharset(filePath, charset)) {
                return charset;
            }
        }

        return StandardCharsets.UTF_8;
    }

    private boolean isValidCharset(Path filePath, Charset charset) {
        try (BufferedReader reader = Files.newBufferedReader(filePath, charset)) {
            String line;
            int checkedLines = 0;
            while ((line = reader.readLine()) != null && checkedLines < 20) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    for (char c : trimmed.toCharArray()) {
                        if (!Character.isLetter(c)) {
                            return false;
                        }
                    }
                    checkedLines++;
                }
            }
            return checkedLines > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private List<String> readAndFilterWords(Path filePath, Charset charset) throws IOException {
        Set<String> wordSet = new LinkedHashSet<>();
        int totalLines = 0;
        int skippedLines = 0;

        try (BufferedReader reader = Files.newBufferedReader(filePath, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                String word = line.trim().toLowerCase();

                if (word.length() != EXPECTED_WORD_LENGTH || !isAllLetters(word)) {
                    skippedLines++;
                    continue;
                }

                wordSet.add(word);
            }
        }

        log("Строк: " + totalLines + ", пропущено: " + skippedLines + ", уникальных: " + wordSet.size());
        return new ArrayList<>(wordSet);
    }

    private boolean isAllLetters(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (!Character.isLetter(word.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void log(String message) {
        if (logWriter != null) {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logWriter.println("[" + timestamp + "] [Loader] " + message);
            logWriter.flush();
        }
    }
}