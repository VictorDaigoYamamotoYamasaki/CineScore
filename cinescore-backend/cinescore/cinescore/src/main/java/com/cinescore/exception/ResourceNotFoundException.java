package com.cinescore.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " não encontrado(a) com id: " + id);
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super(resource + " não encontrado(a) com " + field + ": " + value);
    }
}
