package ru.yandex.practicum;

public class EmptyInputException extends WordleException {

    public EmptyInputException() {
        super("Слово не может быть пустым");
    }
}