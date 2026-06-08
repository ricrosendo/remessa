package br.com.inter.exception;

public final class InvalidUserDocumentException extends RuntimeException implements UserException {

    public InvalidUserDocumentException(String message) {
        super(message);
    }

    @Override
    public String message() {
        return getMessage();
    }
}
