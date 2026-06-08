package br.com.inter.exception;

public sealed interface UserException permits UserNotFoundException, UserAlreadyExistsException, InvalidUserDocumentException {
    String message();
}
