package ru.yandex.practicum;

/**
 * Слово не найдено в словаре.
 */
public class WordNotFoundInDictionaryException extends WordleException {

    private final String word;

    public WordNotFoundInDictionaryException(String word) {
        super("Слова \"" + word + "\" нет в словаре");
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}