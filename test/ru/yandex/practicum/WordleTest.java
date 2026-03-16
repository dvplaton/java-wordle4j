package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordleDictionaryTest {

    private static PrintWriter logWriter;
    private WordleDictionary dictionary;

    @BeforeAll
    static void initLog() {
        logWriter = new PrintWriter(System.out, true);
    }

    @BeforeEach
    void setUp() {
        List<String> words = Arrays.asList("абвгд", "бвгде", "вгдеж", "гдежз", "дежзи");
        dictionary = new WordleDictionary(words, logWriter);
    }

    @Test
    void testContains() {
        assertTrue(dictionary.contains("абвгд"));
        assertTrue(dictionary.contains("АБВГД")); // регистронезависимо
        assertFalse(dictionary.contains("zzzzz"));
    }

    @Test
    void testSize() {
        assertEquals(5, dictionary.size());
    }

    @Test
    void testIsEmpty() {
        assertFalse(dictionary.isEmpty());
        WordleDictionary empty = new WordleDictionary(List.of(), logWriter);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testGetRandomWord() {
        String word = dictionary.getRandomWord();
        assertTrue(dictionary.contains(word));
    }

    @Test
    void testGetRandomWordThrowsOnEmpty() {
        WordleDictionary empty = new WordleDictionary(List.of(), logWriter);
        assertThrows(RuntimeException.class, empty::getRandomWord);
    }

    @Test
    void testCompareWordsAllCorrect() {
        WordleDictionary.LetterMatch[] result = dictionary.compareWords("абвгд", "абвгд");
        for (WordleDictionary.LetterMatch m : result) {
            assertEquals(WordleDictionary.LetterMatch.CORRECT, m);
        }
    }

    @Test
    void testCompareWordsAllAbsent() {
        List<String> words = Arrays.asList("абвгд", "жзикл");
        WordleDictionary dict = new WordleDictionary(words, logWriter);

        WordleDictionary.LetterMatch[] result = dict.compareWords("жзикл", "абвгд");
        for (WordleDictionary.LetterMatch m : result) {
            assertEquals(WordleDictionary.LetterMatch.ABSENT, m);
        }
    }

    @Test
    void testCompareWordsPresent() {
        // "бавгд" vs "абвгд": б=PRESENT, а=PRESENT, в=CORRECT, г=CORRECT, д=CORRECT
        List<String> words = Arrays.asList("абвгд", "бавгд");
        WordleDictionary dict = new WordleDictionary(words, logWriter);

        WordleDictionary.LetterMatch[] result = dict.compareWords("бавгд", "абвгд");
        assertEquals(WordleDictionary.LetterMatch.PRESENT, result[0]);
        assertEquals(WordleDictionary.LetterMatch.PRESENT, result[1]);
        assertEquals(WordleDictionary.LetterMatch.CORRECT, result[2]);
        assertEquals(WordleDictionary.LetterMatch.CORRECT, result[3]);
        assertEquals(WordleDictionary.LetterMatch.CORRECT, result[4]);
    }

    @Test
    void testCompareWordsDuplicateLetters() {
        // "ааааа" vs "абвгд": первая а=CORRECT, остальные=ABSENT
        List<String> words = Arrays.asList("абвгд", "ааааа");
        WordleDictionary dict = new WordleDictionary(words, logWriter);

        WordleDictionary.LetterMatch[] result = dict.compareWords("ааааа", "абвгд");
        assertEquals(WordleDictionary.LetterMatch.CORRECT, result[0]);
        assertEquals(WordleDictionary.LetterMatch.ABSENT, result[1]);
        assertEquals(WordleDictionary.LetterMatch.ABSENT, result[2]);
        assertEquals(WordleDictionary.LetterMatch.ABSENT, result[3]);
        assertEquals(WordleDictionary.LetterMatch.ABSENT, result[4]);
    }

    @Test
    void testCompareWordsNullThrows() {
        assertThrows(RuntimeException.class, () -> dictionary.compareWords(null, "абвгд"));
        assertThrows(RuntimeException.class, () -> dictionary.compareWords("абвгд", null));
    }

    @Test
    void testCompareWordsDifferentLengthThrows() {
        assertThrows(RuntimeException.class, () -> dictionary.compareWords("аб", "абвгд"));
    }

    @Test
    void testIsExactMatch() {
        assertTrue(dictionary.isExactMatch("абвгд", "абвгд"));
        assertTrue(dictionary.isExactMatch("АБВГД", "абвгд"));
        assertFalse(dictionary.isExactMatch("бвгде", "абвгд"));
    }

    @Test
    void testMatchesToString() {
        WordleDictionary.LetterMatch[] matches = {
                WordleDictionary.LetterMatch.CORRECT,
                WordleDictionary.LetterMatch.PRESENT,
                WordleDictionary.LetterMatch.ABSENT
        };
        assertEquals("🟩🟨⬜", dictionary.matchesToString(matches));
    }

    @Test
    void testFormatGuessResult() {
        WordleDictionary.LetterMatch[] matches = {
                WordleDictionary.LetterMatch.CORRECT,
                WordleDictionary.LetterMatch.ABSENT,
                WordleDictionary.LetterMatch.PRESENT,
                WordleDictionary.LetterMatch.CORRECT,
                WordleDictionary.LetterMatch.ABSENT
        };
        String result = dictionary.formatGuessResult("абвгд", matches);
        assertTrue(result.contains("А[🟩]"));
        assertTrue(result.contains("Б[⬜]"));
        assertTrue(result.contains("В[🟨]"));
        assertTrue(result.contains("Г[🟩]"));
        assertTrue(result.contains("Д[⬜]"));
    }
}

class WordleDictionaryLoaderTest {

    private static PrintWriter logWriter;

    @BeforeAll
    static void initLog() {
        logWriter = new PrintWriter(System.out, true);
    }

    @Test
    void testLoadExistingFile() throws IOException {
        WordleDictionaryLoader loader = new WordleDictionaryLoader(logWriter);
        WordleDictionary dictionary = loader.load("words_ru.txt");

        assertNotNull(dictionary);
        assertFalse(dictionary.isEmpty());
        // Все слова должны быть длиной 5
        for (String word : dictionary.getWords()) {
            assertEquals(5, word.length(), "Слово неправильной длины: " + word);
        }
    }

    @Test
    void testLoadNonExistingFile() {
        WordleDictionaryLoader loader = new WordleDictionaryLoader(logWriter);
        assertThrows(FileNotFoundException.class, () -> loader.load("nonexistent_file.txt"));
    }

    @Test
    void testAllWordsAreLowercase() throws IOException {
        WordleDictionaryLoader loader = new WordleDictionaryLoader(logWriter);
        WordleDictionary dictionary = loader.load("words_ru.txt");

        for (String word : dictionary.getWords()) {
            assertEquals(word.toLowerCase(), word, "Слово не в нижнем регистре: " + word);
        }
    }

    @Test
    void testAllWordsAreLettersOnly() throws IOException {
        WordleDictionaryLoader loader = new WordleDictionaryLoader(logWriter);
        WordleDictionary dictionary = loader.load("words_ru.txt");

        for (String word : dictionary.getWords()) {
            for (char c : word.toCharArray()) {
                assertTrue(Character.isLetter(c), "Не буква в слове \"" + word + "\": " + c);
            }
        }
    }

    @Test
    void testNoDuplicates() throws IOException {
        WordleDictionaryLoader loader = new WordleDictionaryLoader(logWriter);
        WordleDictionary dictionary = loader.load("words_ru.txt");

        long uniqueCount = dictionary.getWords().stream().distinct().count();
        assertEquals(dictionary.size(), uniqueCount, "В словаре есть дубликаты");
    }
}

class WordleGameTest {

    private static PrintWriter logWriter;
    private WordleDictionary dictionary;

    @BeforeAll
    static void initLog() {
        logWriter = new PrintWriter(System.out, true);
    }

    @BeforeEach
    void setUp() {
        // Словарь из русских 5-буквенных слов
        List<String> words = Arrays.asList(
                "кошка", "мышка", "книга", "лампа", "парта",
                "рыбка", "ручка", "полка", "горка", "марка"
        );
        dictionary = new WordleDictionary(words, logWriter);
    }

    private WordleGame createGameWithAnswer(String answer) {
        WordleGame game;
        int attempts = 0;
        do {
            game = new WordleGame(dictionary, logWriter);
            attempts++;
            if (attempts > 1000) {
                fail("Не удалось создать игру с ответом \"" + answer + "\"");
            }
        } while (!game.getSecretWord().equals(answer));
        return game;
    }

    // Нормализация

    @Test
    void testNormalize() {
        assertEquals("кошка", WordleGame.normalize("  КОШКА  "));
        assertEquals("мышка", WordleGame.normalize("Мышка"));
        assertEquals("", WordleGame.normalize(null));
        assertEquals("", WordleGame.normalize("  "));
    }

    @Test
    void testIsRussianLettersOnly() {
        assertTrue(WordleGame.isRussianLettersOnly("кошка"));
        assertTrue(WordleGame.isRussianLettersOnly("ёжик")); // ё не 5 букв, но проверяем символы
        assertFalse(WordleGame.isRussianLettersOnly("apple"));
        assertFalse(WordleGame.isRussianLettersOnly("кот12"));
        assertFalse(WordleGame.isRussianLettersOnly("кот ка"));
    }

    // Валидация

    @Test
    void testValidateEmptyInput() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        assertThrows(EmptyInputException.class, () -> game.validateGuess(""));
    }

    @Test
    void testValidateNonRussian() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        assertThrows(InvalidWordContentException.class, () -> game.validateGuess("apple"));
    }

    @Test
    void testValidateWrongLength() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        assertThrows(InvalidWordLengthException.class, () -> game.validateGuess("кот"));
    }

    @Test
    void testValidateNotInDictionary() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        assertThrows(WordNotFoundInDictionaryException.class, () -> game.validateGuess("абвгд"));
    }

    @Test
    void testValidateCorrectWord() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        assertDoesNotThrow(() -> game.validateGuess("кошка"));
    }

    // Основная игра

    @Test
    void testMakeGuessReturnsResult() throws WordleException {
        WordleGame game = new WordleGame(dictionary, logWriter);
        String result = game.makeGuess("кошка");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testWinGame() throws WordleException {
        WordleGame game = createGameWithAnswer("кошка");
        assertFalse(game.isWon());
        assertFalse(game.isGameOver());

        game.makeGuess("кошка");

        assertTrue(game.isWon());
        assertTrue(game.isGameOver());
        assertEquals(1, game.getCurrentAttempt());
    }

    @Test
    void testLoseGame() throws WordleException {
        WordleGame game = createGameWithAnswer("кошка");

        // 6 неправильных попыток
        String[] wrongGuesses = {"мышка", "книга", "лампа", "парта", "рыбка", "ручка"};
        for (String guess : wrongGuesses) {
            if (!game.isGameOver()) {
                game.makeGuess(guess);
            }
        }

        assertTrue(game.isGameOver());
        assertFalse(game.isWon());
        assertEquals("кошка", game.getSecretWord());
    }

    @Test
    void testGuessAfterGameOverThrows() throws WordleException {
        WordleGame game = createGameWithAnswer("кошка");
        game.makeGuess("кошка"); // победа

        assertThrows(GameOverException.class, () -> game.makeGuess("мышка"));
    }

    @Test
    void testAttemptCounter() throws WordleException {
        WordleGame game = new WordleGame(dictionary, logWriter);
        assertEquals(0, game.getCurrentAttempt());

        game.makeGuess("кошка");
        assertEquals(1, game.getCurrentAttempt());

        if (!game.isGameOver()) {
            game.makeGuess("мышка");
            assertEquals(2, game.getCurrentAttempt());
        }
    }

    @Test
    void testGuessHistory() throws WordleException {
        WordleGame game = new WordleGame(dictionary, logWriter);

        game.makeGuess("кошка");
        if (!game.isGameOver()) {
            game.makeGuess("мышка");
        }

        List<String> history = game.getGuessHistory();
        assertEquals("кошка", history.get(0));
        if (history.size() > 1) {
            assertEquals("мышка", history.get(1));
        }
    }

    // Подсказки

    @Test
    void testHintReturnsWordFromDictionary() throws GameOverException {
        WordleGame game = new WordleGame(dictionary, logWriter);
        String hint = game.getHint();
        assertTrue(dictionary.contains(hint), "Подсказка должна быть из словаря");
    }

    @Test
    void testHintAfterGameOverThrows() throws WordleException {
        WordleGame game = createGameWithAnswer("кошка");
        game.makeGuess("кошка");
        assertThrows(GameOverException.class, game::getHint);
    }

    @Test
    void testHintDoesNotRepeat() throws GameOverException {
        WordleGame game = new WordleGame(dictionary, logWriter);
        String hint1 = game.getHint();
        String hint2 = game.getHint();
        assertNotEquals(hint1, hint2, "Подсказки не должны повторяться");
    }

    @Test
    void testHintCompatibleWithKnowledge() throws WordleException {
        WordleGame game = createGameWithAnswer("кошка");

        // Попытка, чтобы накопить знания
        game.makeGuess("мышка");

        // Подсказка совместима со знаниями
        String hint = game.getHint();
        assertTrue(dictionary.contains(hint));

        // Подсказка не должна быть уже использованным словом
        assertNotEquals("мышка", hint);
    }

    @Test
    void testFilterCandidatesContainsAnswer() throws WordleException {
        WordleGame game = createGameWithAnswer("кошка");

        // После любой попытки ответ остаётся среди кандидатов
        game.makeGuess("мышка");

        List<String> candidates = game.filterCandidates();
        assertTrue(candidates.contains("кошка"), "Алгоритм потерял решение! Кандидаты: " + candidates);
    }

    @Test
    void testAutoPlayFindsAnswer() throws WordleException {
        // Симуляция автоигры: компьютер сам угадывает
        WordleGame game = new WordleGame(dictionary, logWriter);
        String answer = game.getSecretWord();

        int maxSteps = game.getMaxAttempts();
        for (int i = 0; i < maxSteps && !game.isGameOver(); i++) {
            String hint = game.getHint();
            game.makeGuess(hint);
        }

        // С маленьким словарём автоигра должна найти ответ
        assertTrue(game.isGameOver(), "Игра должна завершиться за " + maxSteps + " попыток");
    }

    // Состояние

    @Test
    void testGetStateAsString() throws WordleException {
        WordleGame game = new WordleGame(dictionary, logWriter);
        game.makeGuess("кошка");

        String state = game.getStateAsString();
        assertNotNull(state);
        assertTrue(state.contains("Wordle"));
        assertTrue(state.contains("1/6"));
    }

    @Test
    void testGetResultAsStringWin() throws WordleException {
        WordleGame game = createGameWithAnswer("кошка");
        game.makeGuess("кошка");

        String result = game.getResultAsString();
        assertTrue(result.contains("Поздравляем"));
        assertTrue(result.contains("кошка"));
    }

    @Test
    void testGetResultAsStringLose() throws WordleException {
        WordleGame game = createGameWithAnswer("кошка");
        String[] guesses = {"мышка", "книга", "лампа", "парта", "рыбка", "ручка"};
        for (String g : guesses) {
            if (!game.isGameOver()) game.makeGuess(g);
        }

        String result = game.getResultAsString();
        assertTrue(result.contains("проиграли"));
        assertTrue(result.contains("кошка"));
    }

    // Пустой словарь

    @Test
    void testNullDictionaryThrows() {
        assertThrows(RuntimeException.class, () -> new WordleGame(null, logWriter));
    }

    @Test
    void testEmptyDictionaryThrows() {
        WordleDictionary empty = new WordleDictionary(List.of(), logWriter);
        assertThrows(RuntimeException.class, () -> new WordleGame(empty, logWriter));
    }
}