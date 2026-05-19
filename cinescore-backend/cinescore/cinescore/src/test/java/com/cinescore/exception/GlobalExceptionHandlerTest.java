package com.cinescore.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler - Testes Unitários")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void configurar() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Deve retornar 404 para ResourceNotFoundException")
    void deveRetornar404ParaResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Review", 99L);

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleResourceNotFound(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().getError()).isEqualTo("Recurso não encontrado");
        assertThat(resposta.getBody().getMessage()).contains("Review");
    }

    @Test
    @DisplayName("Deve retornar 409 para DuplicateResourceException")
    void deveRetornar409ParaDuplicateResource() {
        DuplicateResourceException ex = new DuplicateResourceException("Usuário", "e-mail", "x@x.com");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleDuplicateResource(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().getMessage()).contains("e-mail");
    }

    @Test
    @DisplayName("Deve retornar 403 para UnauthorizedOperationException")
    void deveRetornar403ParaUnauthorizedOperation() {
        UnauthorizedOperationException ex = new UnauthorizedOperationException("editar esta review");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleUnauthorizedOperation(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody().getMessage()).contains("editar");
    }

    @Test
    @DisplayName("Deve retornar 400 para InvalidInputException com nome do campo")
    void deveRetornar400ParaInvalidInputComCampo() {
        InvalidInputException ex = new InvalidInputException("texto", "não pode ser vazio");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleInvalidInput(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().getMessage()).contains("texto");
    }

    @Test
    @DisplayName("Deve retornar 422 para ContentModerationException")
    void deveRetornar422ParaContentModeration() {
        ContentModerationException ex = new ContentModerationException("review");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleContentModeration(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getBody().getError()).isEqualTo("Conteúdo inapropriado");
    }

    @Test
    @DisplayName("Deve retornar 401 para BadCredentialsException")
    void deveRetornar401ParaBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Credenciais inválidas");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleBadCredentials(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody().getMessage()).contains("incorretos");
    }

    @Test
    @DisplayName("Deve retornar 403 para AccessDeniedException")
    void deveRetornar403ParaAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Acesso negado");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleAccessDenied(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Deve retornar 400 para SelfFollowException")
    void deveRetornar400ParaSelfFollow() {
        SelfFollowException ex = new SelfFollowException();

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleSelfFollow(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().getMessage()).contains("si mesmo");
    }
}
