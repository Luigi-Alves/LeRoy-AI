package com.leroyai.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;

import java.util.Locale;

public class GeminiService {

    public record Interpretacao(String termo, String explicacao) {
    }

    private static final String MODEL = modelo();
    private static final int MAX_EXPLICACAO = 300;

    private static volatile Client client;

    private static String modelo() {
        String valor = System.getenv("GEMINI_MODEL");
        return valor == null || valor.isBlank() ? "gemini-3.8-flash" : valor.trim();
    }

    public boolean configurado() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        return apiKey != null && !apiKey.isBlank();
    }

    private static Client getClient() {
        Client current = client;

        if (current == null) {
            synchronized (GeminiService.class) {
                current = client;

                if (current == null) {
                    String apiKey = System.getenv("GEMINI_API_KEY");

                    if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException(
                            "A variável GEMINI_API_KEY não foi configurada."
                        );
                    }

                    HttpOptions httpOptions = HttpOptions.builder()
                        .timeout(8_000)
                        .retryOptions(
                            HttpRetryOptions.builder()
                                .attempts(1)
                                .build()
                        )
                        .build();

                    current = Client.builder()
                        .apiKey(apiKey)
                        .httpOptions(httpOptions)
                        .build();

                    client = current;
                }
            }
        }

        return current;
    }

    /**
     * Interpreta a necessidade do cliente. Lança exceção se a IA falhar;
     * quem chama deve tratar e seguir com a busca local.
     */
    public Interpretacao interpretar(String mensagem) {

        String prompt = """
            Você é o interpretador de necessidades de um assistente
            de compras da Leroy Merlin.

            Analise a necessidade do cliente e responda EXATAMENTE em duas
            linhas, neste formato:

            TERMO: <até três palavras que existam no nome, descrição ou
            categoria de um catálogo de construção e casa>
            RESPOSTA: <uma ou duas frases curtas, em português, dizendo
            que tipos de produto o cliente deve procurar para resolver o
            que pediu>

            Regras:
            - Considere sinônimos e problemas descritos sem o nome da peça.
            - Na RESPOSTA, cite apenas tipos de produto. Não informe preços,
              estoque, marcas, medidas, voltagens nem instruções técnicas
              de instalação.
            - Se a solicitação não for sobre casa, construção ou reforma,
              responda TERMO: sem resultado e RESPOSTA: sem resultado.

            Exemplos:
            "Preciso pintar uma parede"
            TERMO: tinta parede
            RESPOSTA: Para pintar uma parede você vai precisar de tinta, algo para preparar a superfície e acessórios de aplicação como rolo e fita crepe.

            "Minha torneira está pingando"
            TERMO: torneira
            RESPOSTA: Vazamento em torneira costuma envolver a própria torneira ou itens de vedação; veja as opções abaixo.

            Mensagem do cliente: %s
            """.formatted(mensagem);

        GenerateContentResponse response =
            getClient().models.generateContent(MODEL, prompt, null);

        String texto = response.text();

        if (texto == null || texto.isBlank()) {
            throw new IllegalStateException("O Gemini não retornou resposta.");
        }

        return interpretarResposta(texto);
    }

    static Interpretacao interpretarResposta(String texto) {
        String termo = null;
        String explicacao = null;
        String primeiraLinha = null;

        for (String linha : texto.split("\\R")) {
            String limpa = linha.replace("*", "").trim();

            if (limpa.isEmpty()) {
                continue;
            }

            if (primeiraLinha == null) {
                primeiraLinha = limpa;
            }

            String minusculo = limpa.toLowerCase(Locale.ROOT);

            if (minusculo.startsWith("termo:")) {
                termo = limpar(limpa.substring(6));
            } else if (minusculo.startsWith("resposta:")) {
                explicacao = limpar(limpa.substring(9));
            }
        }

        if (termo == null) {
            termo = limpar(primeiraLinha);
        }

        if (termo.isBlank()
                || termo.toLowerCase(Locale.ROOT).contains("sem resultado")) {
            return new Interpretacao(null, null);
        }

        if (explicacao != null) {
            if (explicacao.isBlank()
                    || explicacao.toLowerCase(Locale.ROOT)
                        .contains("sem resultado")) {
                explicacao = null;
            } else if (explicacao.length() > MAX_EXPLICACAO) {
                explicacao = explicacao.substring(0, MAX_EXPLICACAO).trim()
                    + "...";
            }
        }

        return new Interpretacao(termo, explicacao);
    }

    private static String limpar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\"", "").replace("\n", " ").trim();
    }
}