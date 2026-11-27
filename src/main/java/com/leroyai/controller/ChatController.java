package com.leroyai.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.leroyai.model.Produto;
import com.leroyai.service.GeminiService;
import com.leroyai.service.InteractionTrackingService;
import com.leroyai.service.ProdutoService;
import com.leroyai.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatController {

    private static final int MAX_MENSAGEM = 300;

    private final ProdutoService produtoService;
    private final GeminiService geminiService;
    private final InteractionTrackingService trackingService;

    public ChatController(
            ProdutoService produtoService,
            GeminiService geminiService,
            InteractionTrackingService trackingService
    ) {
        this.produtoService = produtoService;
        this.geminiService = geminiService;
        this.trackingService = trackingService;
    }

    public void handle(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendJson(exchange, 405, error("Método não permitido. Use POST."));
            return;
        }

        int statusCode;
        Object response;
        String trackingSessionId = null;
        String trackingInteractionId = null;

        try {
            String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );
            ChatRequest request =
                JsonUtil.fromJson(body, ChatRequest.class);
            String mensagem = request == null || request.mensagem() == null
                ? ""
                : request.mensagem().trim();

            if (mensagem.length() > MAX_MENSAGEM) {
                mensagem = mensagem.substring(0, MAX_MENSAGEM);
            }

            if (mensagem.isBlank()) {
                statusCode = 400;
                response = error("Envie uma mensagem.");
            } else {
                String interactionId = UUID.randomUUID().toString();
                String sessionId =
                    exchange.getRequestHeaders().getFirst("X-Session-Id");
                if (!InteractionTrackingService.isValidUuid(sessionId)) {
                    sessionId = UUID.randomUUID().toString();
                    exchange.getResponseHeaders().set(
                        "X-Session-Id",
                        sessionId
                    );
                }
                trackingSessionId = sessionId;
                trackingInteractionId = interactionId;
                try {
                    trackingService.recordChatInteraction(
                        sessionId,
                        interactionId
                    );
                } catch (IOException e) {
                    logTrackingFailure(e);
                }

                ChatResponse chatResponse =
                    processMessage(mensagem, interactionId);
                try {
                    trackingService.recordChatResults(
                        sessionId,
                        interactionId,
                        chatResponse.produtos()
                    );
                } catch (IOException e) {
                    logTrackingFailure(e);
                }
                response = chatResponse;
                statusCode = 200;
            }
        } catch (JsonParseException e) {
            statusCode = 400;
            response = error("Envie uma requisição JSON válida.");
        } catch (Exception e) {
            System.err.println("ERRO AO PROCESSAR MENSAGEM:");
            e.printStackTrace();

            if (trackingSessionId != null) {
                try {
                    trackingService.recordChatSearchFailure(
                        trackingSessionId,
                        trackingInteractionId
                    );
                } catch (IOException trackingException) {
                    logTrackingFailure(trackingException);
                }
            }

            statusCode = 500;
            response = error(
                "Não foi possível processar sua mensagem. Tente novamente mais tarde."
            );
        }

        sendJson(exchange, statusCode, response);
    }

    private ChatResponse processMessage(
            String mensagem,
            String interactionId
    ) {
        if (isListAllRequest(mensagem)) {
            return new ChatResponse(
                "Aqui estão todos os produtos disponíveis.",
                "todos os produtos disponíveis",
                produtoService.listarProdutosDisponiveis(),
                interactionId
            );
        }

        // A IA interpreta e explica; se falhar, a busca local continua.
        GeminiService.Interpretacao ia = interpretarComIa(mensagem);

        List<Produto> produtos =
            produtoService.buscarProdutosDireto(mensagem);
        String termoBusca = mensagem;

        if (produtos.isEmpty() && ia != null && ia.termo() != null) {
            termoBusca = ia.termo();
            produtos = produtoService.buscarProdutos(termoBusca);
        }

        return new ChatResponse(
            buildResponseMessage(mensagem, produtos, ia),
            termoBusca,
            produtos,
            interactionId
        );
    }

    private GeminiService.Interpretacao interpretarComIa(String mensagem) {
        if (!geminiService.configurado()) {
            return null;
        }

        try {
            return geminiService.interpretar(mensagem);
        } catch (Exception e) {
            System.err.println(
                "Gemini indisponível, usando busca local: " + e.getMessage()
            );
            return null;
        }
    }

    private String buildResponseMessage(
            String mensagem,
            List<Produto> produtos,
            GeminiService.Interpretacao ia
    ) {
        if (produtos.isEmpty()) {
            return "Não encontrei produtos disponíveis no catálogo demonstrativo "
                + "para essa necessidade. Tente descrever de outra forma, "
                + "por exemplo: \"torneira pingando\" ou \"pintar uma parede\". "
                + "Os preços e estoques não representam dados reais de uma loja.";
        }

        StringBuilder texto = new StringBuilder();

        if (ia != null && ia.explicacao() != null
                && !ia.explicacao().isBlank()) {
            texto.append(ia.explicacao()).append("\n\n");
        }

        texto.append("Encontrei ").append(produtos.size())
            .append(" produto(s) relacionado(s) no catálogo demonstrativo. ")
            .append("Preços e quantidades são fictícios e não representam o ")
            .append("estoque real da Leroy Merlin.");

        if (isTechnicalQuestion(mensagem)) {
            texto.append(" O catálogo não contém especificações de instalação ")
                .append("ou compatibilidade; confirme esses detalhes na ")
                .append("embalagem, com o fabricante ou com um profissional ")
                .append("qualificado.");
        } else if (containsAny(
                normalize(mensagem), "pingando", "vazando", "vazamento")) {
            texto.append(" O catálogo não permite diagnosticar a causa do ")
                .append("vazamento; confirme a peça compatível antes de comprar.");
        }

        return texto.toString();
    }

    private boolean isTechnicalQuestion(String mensagem) {
        String normalized = normalize(mensagem);
        return mensagem.contains("?")
            || containsAny(
                normalized,
                "como instalar",
                "como usar",
                "como funciona",
                "compativ",
                "adequado",
                "serve para",
                "qual produto",
                "quais produtos",
                "posso usar"
            );
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return Normalizer.normalize(
                text.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
            )
            .replaceAll("\\p{M}", "");
    }

    private boolean isListAllRequest(String mensagem) {
        String normalized = normalize(mensagem);

        boolean refersToCatalog = normalized.contains("produto")
            || normalized.contains("item")
            || normalized.contains("catalogo");
        boolean asksForAll = normalized.contains("todos")
            || normalized.contains("todas")
            || normalized.contains("tudo")
            || normalized.contains("todo")
            || normalized.contains("toda")
            || normalized.contains("completo")
            || normalized.contains("inteiro");
        boolean asksToList = normalized.contains("listar produto")
            || normalized.contains("liste produto")
            || normalized.contains("mostra produto")
            || normalized.contains("mostrar produto")
            || normalized.contains("exibir produto")
            || normalized.contains("ver produto");

        return refersToCatalog && (asksForAll || asksToList);
    }

    private JsonObject error(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("erro", message);
        return response;
    }

    private void sendJson(
            HttpExchange exchange,
            int statusCode,
            Object body
    ) throws IOException {
        byte[] responseBytes =
            JsonUtil.toJson(body).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type",
            "application/json; charset=UTF-8"
        );
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(responseBytes);
        }
    }

    private void logTrackingFailure(IOException exception) {
        System.err.println("Não foi possível registrar a interação.");
        exception.printStackTrace();
    }

    private record ChatRequest(String mensagem) {
    }

    private record ChatResponse(
            String resposta,
            String termoBusca,
            List<Produto> produtos,
            String interactionId
    ) {
    }
}