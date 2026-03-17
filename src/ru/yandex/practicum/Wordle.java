package ru.yandex.practicum;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Wordle {

    private static final String DICTIONARY_FILE = "words_ru.txt";
    private static final String LOG_FILE = "wordle_log.txt";

    public static void main(String[] args) {
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(new File(LOG_FILE), true)); Scanner scanner = new Scanner(System.in)) {

            run(logWriter, scanner);

        } catch (IOException e) {
            // невозможно открыть лог-файл
            System.err.println("Невозможно создать лог-файл: " + e.getMessage());
        }
    }

    private static void run(PrintWriter logWriter, Scanner scanner) {
        try {
            log(logWriter, "=== Игра Wordle запущена ===");

            WordleDictionaryLoader loader = new WordleDictionaryLoader(logWriter);
            WordleDictionary dictionary = loader.load(DICTIONARY_FILE);
            log(logWriter, "Словарь загружен: " + dictionary.size() + " слов");

            WordleGame game = new WordleGame(dictionary, logWriter);

            System.out.println("=== WORDLE ===");
            System.out.println("Угадайте слово из 5 русских букв. У вас 6 попыток.");
            System.out.println("Нажмите Enter на пустой строке для подсказки.");
            System.out.println("Введите 'quit' для выхода.");
            System.out.println();

            while (!game.isGameOver()) {
                System.out.printf("Попытка %d/%d: ", game.getCurrentAttempt() + 1, game.getMaxAttempts());
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    handleHint(game, logWriter);
                    continue;
                }

                if (input.equalsIgnoreCase("quit")) {
                    System.out.println("Вы вышли. Слово было: " + game.getSecretWord());
                    log(logWriter, "Игрок вышел");
                    return;
                }

                handleGuess(game, input);
            }

            System.out.println();
            System.out.println(game.getResultAsString());
            log(logWriter, "Итог: " + game.getResultAsString());
            log(logWriter, "=== Игра завершена ===");

        } catch (IOException e) {
            log(logWriter, "ОШИБКА IOException: " + e.getMessage());
            logStackTrace(logWriter, e);
        } catch (RuntimeException e) {
            log(logWriter, "ОШИБКА RuntimeException: " + e.getMessage());
            logStackTrace(logWriter, e);
        }
    }

    private static void handleGuess(WordleGame game, String input) {
        try {
            String result = game.makeGuess(input);
            System.out.println("  " + result);
            System.out.println();
            System.out.println(game.getStateAsString());
            System.out.println();
        } catch (EmptyInputException e) {
            System.out.println("  Введите слово.");
        } catch (InvalidWordContentException | InvalidWordLengthException | WordNotFoundInDictionaryException |
                 GameOverException e) {
            System.out.println("  " + e.getMessage());
        } catch (WordleException e) {
            System.out.println("  Ошибка: " + e.getMessage());
        }
    }

    private static void handleHint(WordleGame game, PrintWriter logWriter) {
        try {
            String hint = game.getHint();
            System.out.println("  Подсказка: " + hint);

            // делаем ход подсказанным словом
            String result = game.makeGuess(hint);
            System.out.println("  " + result);
            System.out.println();
            System.out.println(game.getStateAsString());
            System.out.println();

        } catch (GameOverException e) {
            System.out.println("  " + e.getMessage());
        } catch (WordleException e) {
            System.out.println("  Ошибка: " + e.getMessage());
        }
    }

    private static void log(PrintWriter logWriter, String message) {
        logWriter.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] [Main] " + message);
        logWriter.flush();
    }

    private static void logStackTrace(PrintWriter logWriter, Exception e) {
        logWriter.print("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] [Main] Стек вызовов: ");
        e.printStackTrace(logWriter);
        logWriter.flush();
    }
}