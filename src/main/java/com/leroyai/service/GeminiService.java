
package com.leroyai.service;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.GenerateContentResponse;

public class GeminiService {

    private static volatile Client client;
    private static final String MODEL = "gemini-3.8-flash";

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
                        .timeout(15_000)
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

    public String gerarTermoBusca(String mensagem) {

        String prompt = """
            Você é o interpretador de necessidades de um assistente
            de compras da Leroy Merlin.

            Analise a necessidade do cliente e identifique termos de
            produtos presentes em um catálogo de construção e casa.
            Considere sinônimos, problemas descritos sem o nome da peça,
            cômodo, tarefa e finalidade para chegar ao produto mais provável.

            Retorne APENAS um termo curto de busca, com no máximo
            três palavras que possam existir no nome, descrição ou
            categoria do catálogo. Não escreva explicações, frases,
            especificações técnicas ou recomendações.
            Se a solicitação não for relacionada a produtos para casa,
            construção ou reforma, retorne apenas: sem resultado.

            Exemplos:
            "Preciso pintar uma parede" -> tinta parede
            "Minha torneira está pingando" -> torneira
            "Quero pendurar um quadro" -> suporte parede
            "Preciso iluminar meu quarto" -> lâmpada
            "Preciso instalar uma prateleira" -> suporte prateleira
            "Quero trocar uma lâmpada queimada" -> lâmpada

            Mensagem do cliente: %s
            """.formatted(mensagem);

        GenerateContentResponse response =
            getClient().models.generateContent(MODEL, prompt, null);

        String resultado = response.text();

        if (resultado == null || resultado.isBlank()) {
            throw new IllegalStateException(
                "O Gemini não retornou um termo de busca."
            );
        }

        return resultado.trim()
            .replace("\"", "")
            .replace("\n", " ");
    }
}