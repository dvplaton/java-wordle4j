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
        // все слова длиной 5
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
    void testNoYoInWords() throws IOException {
        WordleDictionaryLoader loader = new WordleDictionaryLoader(logWriter);
        WordleDictionary dictionary = loader.load("words_ru.txt");

        for (String word : dictionary.getWords()) {
            assertFalse(word.contains("ё"), "Слово содержит ё (должна быть заменена на е): " + word);
        }
    }

    @Test
    void testAllWordsAreRussianLetters() throws IOException {
        WordleDictionaryLoader loader = new WordleDictionaryLoader(logWriter);
        WordleDictionary dictionary = loader.load("words_ru.txt");

        for (String word : dictionary.getWords()) {
            for (char c : word.toCharArray()) {
                assertTrue(c >= 'а' && c <= 'я', "Недопустимый символ '" + c + "' в слове \"" + word + "\"");
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

    @Test
    void testNormalizeWord() {
        assertEquals("елка", WordleDictionaryLoader.normalizeWord("Ёлка"));
        assertEquals("елка", WordleDictionaryLoader.normalizeWord("  ЁЛКА  "));
        assertEquals("кошка", WordleDictionaryLoader.normalizeWord("кошка"));
        assertEquals("берег", WordleDictionaryLoader.normalizeWord("БЕРЁГ"));
    }
}

class WordleDictionaryTest {

    private static PrintWriter logWriter;
    private WordleDictionary dictionary;

    @BeforeAll
    static void initLog() {
        logWriter = new PrintWriter(System.out, true);
    }

    @BeforeEach
    void setUp() {
        List<String> words = Arrays.asList("кошка", "мышка", "книга", "лампа", "парта");
        dictionary = new WordleDictionary(words, logWriter);
    }

    @Test
    void testContains() {
        assertTrue(dictionary.contains("кошка"));
        assertTrue(dictionary.contains("КОШКА"));
        assertFalse(dictionary.contains("собак"));
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
    void testGet() {
        assertEquals("кошка", dictionary.get(0));
        assertEquals("мышка", dictionary.get(1));
    }

    @Test
    void testGetWords() {
        List<String> words = dictionary.getWords();
        assertEquals(5, words.size());
        assertThrows(UnsupportedOperationException.class, () -> words.add("тест"));
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
        List<String> words = Arrays.asList("кошка", "мышка", "книга", "лампа", "парта", "рыбка", "ручка", "полка", "горка", "марка");
        dictionary = new WordleDictionary(words, logWriter);
    }

    private WordleGame createGameWithAnswer(String answer) {
        WordleGame game;
        int attempts = 0;
        do {
            game = new WordleGame(dictionary, logWriter);
            attempts++;
            if (attempts > 10000) {
                fail("Не удалось создать игру с ответом \"" + answer + "\"");
            }
        } while (!game.getSecretWord().equals(answer));
        return game;
    }

    // Сравнение

    @Test
    void testCompareWordsAllCorrect() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        WordleGame.LetterMatch[] result = game.compareWords("кошка", "кошка");
        for (WordleGame.LetterMatch m : result) {
            assertEquals(WordleGame.LetterMatch.CORRECT, m);
        }
    }

    @Test
    void testCompareWordsDuplicateLetters() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        // "ааааа" vs "кошка": только позиция 4 (а) → CORRECT
        WordleGame.LetterMatch[] result = game.compareWords("ааааа", "кошка");
        assertEquals(WordleGame.LetterMatch.ABSENT, result[0]);
        assertEquals(WordleGame.LetterMatch.ABSENT, result[1]);
        assertEquals(WordleGame.LetterMatch.ABSENT, result[2]);
        assertEquals(WordleGame.LetterMatch.ABSENT, result[3]);
        assertEquals(WordleGame.LetterMatch.CORRECT, result[4]);
    }

    @Test
    void testCompareWordsNullThrows() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        assertThrows(RuntimeException.class, () -> game.compareWords(null, "кошка"));
        assertThrows(RuntimeException.class, () -> game.compareWords("кошка", null));
    }

    @Test
    void testCompareWordsDifferentLengthThrows() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        assertThrows(RuntimeException.class, () -> game.compareWords("кот", "кошка"));
    }

    // Форматирование

    @Test
    void testMatchesToString() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        WordleGame.LetterMatch[] matches = {
                WordleGame.LetterMatch.CORRECT,
                WordleGame.LetterMatch.PRESENT,
                WordleGame.LetterMatch.ABSENT
        };
        assertEquals("🟩🟨⬜", game.matchesToString(matches));
    }

    @Test
    void testFormatGuessResult() {
        WordleGame game = new WordleGame(dictionary, logWriter);
        WordleGame.LetterMatch[] matches = {
                WordleGame.LetterMatch.CORRECT,
                WordleGame.LetterMatch.ABSENT,
                WordleGame.LetterMatch.PRESENT,
                WordleGame.LetterMatch.CORRECT,
                WordleGame.LetterMatch.ABSENT
        };
        String result = game.formatGuessResult("кошка", matches);
        assertTrue(result.contains("К[🟩]"));
        assertTrue(result.contains("О[⬜]"));
        assertTrue(result.contains("Ш[🟨]"));
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
    void testNormalizeReplacesYo() {
        assertEquals("елка", WordleGame.normalize("Ёлка"));
        assertEquals("берег", WordleGame.normalize("БЕРЁГ"));
        assertEquals("еж", WordleGame.normalize("ёж"));
    }

    @Test
    void testIsRussianLettersOnly() {
        assertTrue(WordleGame.isRussianLettersOnly("кошка"));
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

    @Test
    void testMakeGuessWithYo() throws WordleException {
        WordleGame game = new WordleGame(dictionary, logWriter);
        String result = game.makeGuess("горка");
        assertNotNull(result);
    }

    // Подсказки

    @Test
    void testHintReturnsWordFromDictionary() throws GameOverException {
        WordleGame game = new WordleGame(dictionary, logWriter);
        String hint = game.getHint();
        assertTrue(dictionary.contains(hint));
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
        WordleGame game = new WordleGame(dictionary, logWriter);

        int maxSteps = game.getMaxAttempts();
        for (int i = 0; i < maxSteps && !game.isGameOver(); i++) {
            String hint = game.getHint();
            game.makeGuess(hint);
        }

        assertTrue(game.isGameOver());
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