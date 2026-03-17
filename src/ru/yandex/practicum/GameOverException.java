package ru.yandex.practicum;

public class GameOverException extends WordleException {

    public GameOverException() {
        super("Игра уже завершена!");
    }
}