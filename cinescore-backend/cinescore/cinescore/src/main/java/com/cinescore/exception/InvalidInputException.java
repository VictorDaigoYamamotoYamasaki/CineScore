package com.cinescore.exception;

public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String field, String reason) {
        super("Campo inválido '" + field + "': " + reason);
    }
}
