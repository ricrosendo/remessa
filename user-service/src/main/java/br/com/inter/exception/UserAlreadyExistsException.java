package br.com.inter.exception;

public final class UserAlreadyExistsException extends RuntimeException implements UserException {

    public UserAlreadyExistsException(String field) {
        super("User already exists with " + field);
    }

    @Override
    public String message() {
        return getMessage();
    }
}
