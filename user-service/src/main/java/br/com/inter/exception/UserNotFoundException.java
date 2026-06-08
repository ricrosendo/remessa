package br.com.inter.exception;

import java.util.UUID;

public final class UserNotFoundException extends RuntimeException implements UserException {

    public UserNotFoundException(UUID id) {
        super("User not found with id: " + id);
    }

    @Override
    public String message() {
        return getMessage();
    }
}
