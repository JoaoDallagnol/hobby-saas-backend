package io.github.joaodallagnol.backend.user;

public class UsernameAlreadyTakenException extends RuntimeException {
    public UsernameAlreadyTakenException() {
        super("Username is already in use.");
    }
}
