package com.leroyai.controller;

import com.google.gson.JsonParseException;
import com.leroyai.model.Produto;
import com.leroyai.service.InteractionTrackingService;
import com.leroyai.service.ProdutoService;
import com.leroyai.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class InteractionController {

    private static final int MAX_REQUEST_BYTES = 4096;

    private final InteractionTrackingService trackingService;
    private final ProdutoService produtoService;

    public InteractionController(
            InteractionTrackingService trackingService,
            ProdutoService produtoService
    ) {
        this.trackingService = trackingService;
        this.produtoService = produtoService;
    }

    public void handleEvent(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendJson(exchange, 405, new ErrorResponse("Método não permitido."));
            return;
        }

        byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) {
            sendJson(exchange, 413, new ErrorResponse("Requisição muito grande."));
            return;
        }

        EventRequest request;
        try {
            request = JsonUtil.fromJson(
                new String(body, StandardCharsets.UTF_8),
                EventRequest.class
            );
        } catch (JsonParseException e) {
            sendJson(exchange, 400, new ErrorResponse("Envie um JSON válido."));
            return;
        }

        if (request == null) {
            sendJson(exchange, 400, new ErrorResponse("Evento inválido."));
            return;
        }

        String sessionId = exchange.getRequestHeaders().getFirst("X-Session-Id");
        if (!InteractionTrackingService.isValidUuid(sessionId)) {
            sendJson(exchange, 400, new ErrorResponse("Sessão anônima inválida."));
            return;
        }

        try {
            if ("PRODUCT_SELECTION".equals(request.eventType())) {
                recordSelection(exchange, sessionId, request);
                return;
            }

            if ("RATING".equals(request.eventType())) {
                recordRating(exchange, sessionId, request);
                return;
            }

            sendJson(exchange, 400, new ErrorResponse("Tipo de evento inválido."));
        } catch (IOException e) {
            System.err.println("Não foi possível salvar o evento de analytics.");
            e.printStackTrace();
            sendJson(exchange, 500, new ErrorResponse(
                "Não foi possível registrar esta interação."
            ));
        }
    }

    private void recordSelection(
            HttpExchange exchange,
            String sessionId,
            EventRequest request
    ) throws IOException {
        if (request.productId() == null || request.productId() < 1) {
            sendJson(exchange, 400, new ErrorResponse("Produto inválido."));
            return;
        }

        Produto product = produtoService.listarProdutos().stream()
            .filter(item -> item.getId() == request.productId())
            .findFirst()
            .orElse(null);

        if (product == null) {
            sendJson(exchange, 404, new ErrorResponse("Produto não encontrado."));
            return;
        }

        trackingService.recordProductSelection(
            sessionId,
            validInteractionId(request.interactionId()),
            product
        );
        sendJson(exchange, 202, new SuccessResponse(true));
    }

    private void recordRating(
            HttpExchange exchange,
            String sessionId,
            EventRequest request
    ) throws IOException {
        String rating = request.rating();
        if (!"positive".equals(rating) && !"negative".equals(rating)) {
            sendJson(exchange, 400, new ErrorResponse("Avaliação inválida."));
            return;
        }

        String interactionId = validInteractionId(request.interactionId());
        if (interactionId == null
                || !trackingService.hasRecommendation(sessionId, interactionId)) {
            sendJson(exchange, 400, new ErrorResponse(
                "Não foi encontrada uma recomendação para avaliar."
            ));
            return;
        }
        if (!trackingService.recordRatingIfAbsent(
                sessionId,
                interactionId,
                rating
        )) {
            sendJson(exchange, 409, new ErrorResponse(
                "Esta recomendação já foi avaliada."
            ));
            return;
        }

        sendJson(exchange, 202, new SuccessResponse(true));
    }

    private String validInteractionId(String value) {
        return InteractionTrackingService.isValidUuid(value)
            ? UUID.fromString(value).toString()
            : null;
    }

    private void sendJson(
            HttpExchange exchange,
            int statusCode,
            Object body
    ) throws IOException {
        byte[] response = JsonUtil.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type",
            "application/json; charset=UTF-8"
        );
        exchange.sendResponseHeaders(statusCode, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private record EventRequest(
            String eventType,
            Integer productId,
            String interactionId,
            String rating
    ) {
    }

    private record ErrorResponse(String erro) {
    }

    private record SuccessResponse(boolean registrado) {
    }
}
