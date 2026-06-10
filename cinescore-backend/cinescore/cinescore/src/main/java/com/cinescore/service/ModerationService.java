package com.cinescore.service;

import com.cinescore.exception.ContentModerationException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ModerationService {

    private static final String ARQUIVO_PALAVRAS    = "moderation/palavras-ofensivas.txt";
    private static final String PREFIXO_COMENTARIO  = "#";
    private static final Pattern PATTERN_DIACRITICOS       = Pattern.compile("\\p{InCombiningDiacriticalMarks}");
    private static final Pattern PATTERN_NAO_ALFANUMERICO  = Pattern.compile("[^a-z0-9 ]");

    private List<Pattern> padroesOfensivos;

    @PostConstruct
    public void carregarPalavras() {
        try (BufferedReader leitor = abrirArquivoDePalavras()) {
            padroesOfensivos = leitor.lines()
                    .map(String::trim)
                    .filter(this::linhaValida)
                    .map(this::compilarPadrao)
                    .toList();

            log.info("Moderação: {} termos carregados de '{}'", padroesOfensivos.size(), ARQUIVO_PALAVRAS);
        } catch (Exception e) {
            log.error("Falha ao carregar palavras ofensivas de '{}': {}", ARQUIVO_PALAVRAS, e.getMessage());
        }
    }

    public void verificar(String texto, String contexto) {
        if (eOfensivo(texto)) {
            log.info("Conteúdo ofensivo detectado em '{}': {}",
                    contexto, texto.substring(0, Math.min(50, texto.length())));
            throw new ContentModerationException(contexto);
        }
    }

    public boolean eOfensivo(String texto) {
        if (texto == null || texto.isBlank() || texto.length() < 3) return false;
        String textoNormalizado = normalizarTexto(texto);
        return padroesOfensivos.stream()
                .anyMatch(padrao -> padrao.matcher(textoNormalizado).find());
    }


    private BufferedReader abrirArquivoDePalavras() throws Exception {
        return new BufferedReader(new InputStreamReader(
                new ClassPathResource(ARQUIVO_PALAVRAS).getInputStream(),
                StandardCharsets.UTF_8));
    }

    private boolean linhaValida(String linha) {
        return !linha.isBlank() && !linha.startsWith(PREFIXO_COMENTARIO);
    }

    private Pattern compilarPadrao(String palavra) {
        String palavraNormalizada = normalizarTexto(palavra);
        return Pattern.compile("\\b" + Pattern.quote(palavraNormalizada) + "\\b");
    }

    private String normalizarTexto(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD);
        semAcento = PATTERN_DIACRITICOS.matcher(semAcento).replaceAll("");
        return PATTERN_NAO_ALFANUMERICO.matcher(semAcento.toLowerCase()).replaceAll(" ");
    }
}
