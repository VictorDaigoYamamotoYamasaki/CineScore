package com.cinescore.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler - Testes Unitários")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void configurar() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Deve retornar 404 para ResourceNotFoundException com id String")
    void deveRetornar404ParaResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Review", "uuid-abc-123");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleResourceNotFound(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().getError()).isEqualTo("Recurso não encontrado");
        assertThat(resposta.getBody().getMessage()).contains("Review");
        assertThat(resposta.getBody().getTimestamp()).isNotNull();
        assertThat(resposta.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Deve retornar 404 para ResourceNotFoundException com field e value")
    void deveRetornar404ParaResourceNotFoundComField() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Usuário", "e-mail", "x@x.com");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleResourceNotFound(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().getMessage()).contains("e-mail").contains("x@x.com");
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
    @DisplayName("Deve retornar 400 para SelfFollowException com mensagem de si mesmo")
    void deveRetornar400ParaSelfFollow() {
        SelfFollowException ex = new SelfFollowException();

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleSelfFollow(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().getMessage()).contains("si mesmo");
    }

    @Test
    @DisplayName("Deve retornar 401 para BadCredentialsException sem expor detalhes internos")
    void deveRetornar401ParaBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Credenciais inválidas");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleBadCredentials(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody().getMessage()).contains("incorretos");
        assertThat(resposta.getBody().getMessage()).doesNotContain("Credenciais inválidas");
    }

    @Test
    @DisplayName("Deve retornar 403 para AccessDeniedException")
    void deveRetornar403ParaAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Acesso negado");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleAccessDenied(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody().getMessage()).contains("permissão");
    }

    @Test
    @DisplayName("Deve retornar 500 para RuntimeException genérica sem expor mensagem interna")
    void deveRetornar500ParaRuntimeException() {
        RuntimeException ex = new RuntimeException("NPE interno detalhado");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleRuntimeException(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().getMessage()).contains("inesperado");
        assertThat(resposta.getBody().getMessage()).doesNotContain("NPE interno detalhado");
    }

    @Test
    @DisplayName("Deve retornar 500 para Exception genérica sem expor mensagem interna")
    void deveRetornar500ParaExceptionGenerica() throws Exception {
        Exception ex = new Exception("stack trace interno");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleGenericException(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().getMessage()).doesNotContain("stack trace interno");
    }

    @Test
    @DisplayName("Deve retornar 400 para ConstraintViolationException com mensagem da violação")
    void deveRetornar400ParaConstraintViolation() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Título não pode ser vazio");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleConstraintViolation(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().getMessage()).contains("Título não pode ser vazio");
    }

    @Test
    @DisplayName("ErrorResponseDTO deve ter timestamp preenchido em todas as respostas")
    void erroResponseDeveConterTimestamp() {
        InvalidInputException ex = new InvalidInputException("campo", "inválido");

        ResponseEntity<ErrorResponseDTO> resposta = handler.handleInvalidInput(ex);

        assertThat(resposta.getBody().getTimestamp()).isNotNull();
    }
}
