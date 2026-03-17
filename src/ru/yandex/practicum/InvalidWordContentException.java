package ru.yandex.practicum;

/**
 * Слово содержит недопустимые символы.
 */
public class InvalidWordContentException extends WordleException {

    private final String word;

    public InvalidWordContentException(String word) {
        super("Слово \"" + word + "\" содержит недопустимые символы. Допускаются только русские буквы.");
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}