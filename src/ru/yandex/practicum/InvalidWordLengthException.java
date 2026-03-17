package ru.yandex.practicum;

/**
 * Слово неправильной длины.
 */
public class InvalidWordLengthException extends WordleException {

    private final int expectedLength;
    private final int actualLength;

    public InvalidWordLengthException(int expectedLength, int actualLength) {
        super("Слово должно содержать " + expectedLength + " букв (введено " + actualLength + ")");
        this.expectedLength = expectedLength;
        this.actualLength = actualLength;
    }

    public int getExpectedLength() {
        return expectedLength;
    }

    public int getActualLength() {
        return actualLength;
    }
}