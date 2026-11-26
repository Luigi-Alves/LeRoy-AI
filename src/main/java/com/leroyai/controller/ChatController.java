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
                : request.mensagem();

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

        List<Produto> produtos =
            produtoService.buscarProdutosDireto(mensagem);
        String termoBusca = mensagem;

        if (produtos.isEmpty()) {
            termoBusca = geminiService.gerarTermoBusca(mensagem);
            produtos = produtoService.buscarProdutos(termoBusca);
        }

        return new ChatResponse(
            buildResponseMessage(mensagem, produtos),
            termoBusca,
            produtos,
            interactionId
        );
    }

    private String buildResponseMessage(
            String mensagem,
            List<Produto> produtos
    ) {
        if (produtos.isEmpty()) {
            return "Não encontrei produtos disponíveis no catálogo demonstrativo "
                + "para essa necessidade. Os preços e estoques não representam "
                + "dados reais de uma loja.";
        }

        String base = "Encontrei " + produtos.size()
            + " produto(s) relacionado(s) no catálogo demonstrativo. "
            + "Preços e quantidades são fictícios e não representam o estoque "
            + "real da Leroy Merlin.";

        if (isTechnicalQuestion(mensagem)) {
            return base + " O catálogo não contém especificações de instalação "
                + "ou compatibilidade; confirme esses detalhes na embalagem, "
                + "com o fabricante ou com um profissional qualificado.";
        }

        if (containsAny(normalize(mensagem), "pingando", "vazando", "vazamento")) {
            return base + " O catálogo não permite diagnosticar a causa do "
                + "vazamento; confirme a peça compatível antes de comprar.";
        }

        return base;
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
