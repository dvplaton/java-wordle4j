package ru.yandex.practicum;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class Wordle {

    private static final String DICTIONARY_FILE = "words_ru.txt";
    private static final String LOG_FILE = "wordle_log.txt";

    public static void main(String[] args) {
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(new File(LOG_FILE), true)); Scanner scanner = new Scanner(System.in)) {

            log(logWriter, "=== Игра Wordle запущена ===");

            WordleDictionaryLoader loader = new WordleDictionaryLoader(logWriter);
            WordleDictionary dictionary = loader.load(DICTIONARY_FILE);
            log(logWriter, "Словарь загружен: " + dictionary.size() + " слов");

            WordleGame game = new WordleGame(dictionary, logWriter);

            System.out.println("=== WORDLE ===");
            System.out.println("Угадайте слово из 5 русских букв. У вас 6 попыток.");
            System.out.println("Команды: 'hint' — подсказка, 'auto' — автоигра, 'quit' — выход.");
            System.out.println();

            while (!game.isGameOver()) {
                System.out.printf("Попытка %d/%d: ", game.getCurrentAttempt() + 1, game.getMaxAttempts());
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                if (input.equalsIgnoreCase("quit")) {
                    System.out.println("Вы вышли. Слово было: " + game.getSecretWord());
                    log(logWriter, "Игрок вышел");
                    return;
                }

                if (input.equalsIgnoreCase("hint")) {
                    handleHint(game);
                    continue;
                }

                if (input.equalsIgnoreCase("auto")) {
                    autoPlay(game, scanner);
                    break;
                }

                handleGuess(game, input);
            }

            System.out.println();
            System.out.println(game.getResultAsString());
            log(logWriter, "Итог: " + game.getResultAsString());
            log(logWriter, "=== Игра завершена ===");

        } catch (IOException e) {
            System.err.println("Ошибка файла: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Внутренняя ошибка: " + e.getMessage());
            e.printStackTrace();
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

    private static void handleHint(WordleGame game) {
        try {
            String hint = game.getHint();
            System.out.println("  Подсказка: попробуйте \"" + hint + "\"");
        } catch (GameOverException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private static void autoPlay(WordleGame game, Scanner scanner) {
        System.out.println("  Автоигра! Нажимайте Enter для каждого хода.");
        System.out.println();

        while (!game.isGameOver()) {
            try {
                String hint = game.getHint();
                System.out.printf("  Попытка %d/%d — пробую \"%s\"... (Enter)", game.getCurrentAttempt() + 1, game.getMaxAttempts(), hint);
                scanner.nextLine();

                String result = game.makeGuess(hint);
                System.out.println("  " + result);
                System.out.println();
                System.out.println(game.getStateAsString());
                System.out.println();

            } catch (GameOverException e) {
                break;
            } catch (WordleException e) {
                System.out.println("  Ошибка автоигры: " + e.getMessage());
                break;
            }
        }
    }

    private static void log(PrintWriter logWriter, String message) {
        logWriter.println("[" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] [Main] " + message);
        logWriter.flush();
    }
}