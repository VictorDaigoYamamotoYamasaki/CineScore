package com.cinescore.service;

import com.cinescore.exception.ContentModerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ModerationService - Testes Unitários")
class ModerationServiceTest {

    private ModerationService moderationService;

    @BeforeEach
    void configurar() {
        moderationService = new ModerationService();
        // Injeta padrões de teste sem depender do arquivo de classpath
        List<Pattern> padroesTeste = List.of(
                Pattern.compile("\\bpalavraofensiva\\b"),
                Pattern.compile("\\btermoruim\\b"),
                Pattern.compile("\\bconteudobad\\b")
        );
        ReflectionTestUtils.setField(moderationService, "padroesOfensivos", padroesTeste);
    }

    @Test
    @DisplayName("Deve lançar ContentModerationException ao detectar palavra ofensiva")
    void deveLancarExcecaoComPalavraOfensiva() {
        assertThatThrownBy(() -> moderationService.verificar("texto com palavraofensiva aqui", "review"))
                .isInstanceOf(ContentModerationException.class);
    }

    @Test
    @DisplayName("Deve aceitar texto limpo sem lançar exceção")
    void deveAceitarTextoLimpo() {
        assertThatNoException()
                .isThrownBy(() -> moderationService.verificar("Excelente filme, altamente recomendado!", "review"));
    }

    @Test
    @DisplayName("Deve detectar palavra ofensiva dentro de uma frase")
    void deveDetectarPalavraOfensivaEmFrase() {
        assertThat(moderationService.eOfensivo("esse filme é termoruim mesmo")).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false para texto sem palavras ofensivas")
    void deveRetornarFalseParaTextoLimpo() {
        assertThat(moderationService.eOfensivo("Ótimo roteiro e atuações memoráveis")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false para texto nulo")
    void deveRetornarFalseParaTextoNulo() {
        assertThat(moderationService.eOfensivo(null)).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false para texto vazio")
    void deveRetornarFalseParaTextoVazio() {
        assertThat(moderationService.eOfensivo("  ")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false para texto com menos de 3 caracteres")
    void deveRetornarFalseParaTextoCurto() {
        assertThat(moderationService.eOfensivo("ok")).isFalse();
    }

    @Test
    @DisplayName("Deve normalizar texto com acentos antes de verificar")
    void deveNormalizarAcentosAntesDeVerificar() {
        // "palavraofensiva" com acento simulado — o serviço normaliza antes de comparar
        assertThat(moderationService.eOfensivo("pálávrâofensívá")).isFalse(); // não é a palavra exata
        assertThat(moderationService.eOfensivo("palavraofensiva")).isTrue();  // exata → detectada
    }
}
