package com.cinescore.exception;

public class ContentModerationException extends RuntimeException {

    public ContentModerationException(String field) {
        super("Conteúdo inapropriado detectado em '" + field
                + "'. Por favor, revise o texto antes de enviar.");
    }
}
