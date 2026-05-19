package com.cinescore.exception;

public class UnauthorizedOperationException extends RuntimeException {

    public UnauthorizedOperationException(String operation) {
        super("Sem permissão para " + operation);
    }
}
