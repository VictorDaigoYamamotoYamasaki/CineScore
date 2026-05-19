package com.cinescore.exception;

public class SelfFollowException extends RuntimeException {

    public SelfFollowException() {
        super("Um usuário não pode seguir a si mesmo.");
    }
}
