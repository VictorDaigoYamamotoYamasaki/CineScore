package com.cinescore.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resource, String field, String value) {
        super(resource + " já existe com " + field + ": " + value);
    }
}
