package com.leroyai;

import com.google.gson.JsonObject;

import com.leroyai.controller.ChatController;
import com.leroyai.controller.InteractionController;
import com.leroyai.model.Produto;
import com.leroyai.service.GeminiService;
import com.leroyai.service.InteractionTrackingService;
import com.leroyai.service.ProdutoService;
import com.leroyai.util.JsonUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException {

        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", port),
            0
        );

        ProdutoService produtoService = new ProdutoService();
        InteractionTrackingService trackingService =
            new InteractionTrackingService(
                Path.of("data", "interactions.json")
            );
        GeminiService geminiService = new GeminiService();
        ChatController chatController =
            new ChatController(
                produtoService,
                geminiService,
                trackingService
            );
        InteractionController interactionController =
            new InteractionController(trackingService, produtoService);

        server.createContext("/api/health", exchange -> {

            JsonObject response = new JsonObject();

            response.addProperty("status", "online");
            response.addProperty(
                "mensagem",
                "LeRoy AI Assistant está funcionando!"
            );
            response.addProperty("ia", geminiService.configurado());

            sendResponse(exchange, 200, JsonUtil.toJson(response));
        });

        server.createContext("/api/products", exchange -> {

            try {

                List<Produto> produtos =
                    produtoService.listarProdutos();

                sendResponse(
                    exchange,
                    200,
                    JsonUtil.toJson(produtos)
                );

            } catch (Exception e) {

                e.printStackTrace();

                sendResponse(
                    exchange,
                    500,
                    "{\"erro\":\"Erro ao consultar os produtos.\"}"
                );
            }
        });

        server.createContext("/api/search", exchange -> {

            try {

                String query = exchange.getRequestURI()
                    .getRawQuery();

                if (query == null || !query.startsWith("q=")) {

                    sendResponse(
                        exchange,
                        400,
                        "{\"erro\":\"Informe um termo de busca.\"}"
                    );

                    return;
                }

                String termo = URLDecoder.decode(
                    query.substring(2),
                    StandardCharsets.UTF_8
                );

                List<Produto> produtos =
                    produtoService.buscarProdutos(termo);

                String sessionId =
                    exchange.getRequestHeaders().getFirst("X-Session-Id");
                if (!InteractionTrackingService.isValidUuid(sessionId)) {
                    sessionId = UUID.randomUUID().toString();
                }
                exchange.getResponseHeaders().set("X-Session-Id", sessionId);
                recordSearch(trackingService, sessionId, produtos);

                sendResponse(
                    exchange,
                    200,
                    JsonUtil.toJson(produtos)
                );

            } catch (Exception e) {

                e.printStackTrace();

                sendResponse(
                    exchange,
                    500,
                    "{\"erro\":\"Erro ao buscar produtos.\"}"
                );
            }
        });

        server.createContext("/api/chat", chatController::handle);

        server.createContext("/api/analytics/events", exchange -> {
            if (!exchange.getRequestURI().getPath()
                    .equals("/api/analytics/events")) {
                sendResponse(exchange, 404, "{\"erro\":\"Rota não encontrada.\"}");
                return;
            }
            interactionController.handleEvent(exchange);
        });

        server.createContext("/api/analytics/summary", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "{\"erro\":\"Método não permitido.\"}");
                return;
            }

            try {
                sendResponse(
                    exchange,
                    200,
                    JsonUtil.toJson(trackingService.resumo())
                );
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(
                    exchange,
                    500,
                    "{\"erro\":\"Não foi possível gerar o resumo.\"}"
                );
            }
        });

        server.createContext("/", exchange -> {

            if (!exchange.getRequestURI().getPath().equals("/")) {
                sendResponse(
                    exchange,
                    404,
                    "{\"erro\":\"Rota não encontrada.\"}"
                );
                return;
            }

            serveHtml(exchange, "/static/index.html");
        });

        server.setExecutor(
            Executors.newFixedThreadPool(10)
        );

        server.start();

        System.out.println(
            "LeRoy AI Assistant iniciado na porta " + port
                + " (IA " + (geminiService.configurado()
                    ? "configurada" : "NÃO configurada, usando busca local")
                + ")"
        );
    }

    private static void recordSearch(
            InteractionTrackingService trackingService,
            String sessionId,
            List<Produto> products
    ) {
        try {
            trackingService.recordSearch(sessionId, products);
        } catch (IOException e) {
            System.err.println("Não foi possível registrar a pesquisa.");
            e.printStackTrace();
        }
    }

    private static void serveHtml(
            HttpExchange exchange,
            String resourcePath
    ) throws IOException {
        byte[] html;
        try (InputStream input = Main.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                sendResponse(
                    exchange,
                    404,
                    "{\"erro\":\"Página não encontrada.\"}"
                );
                return;
            }
            html = input.readAllBytes();
        } catch (IOException e) {
            System.err.println("Não foi possível carregar a página " + resourcePath);
            e.printStackTrace();
            sendResponse(
                exchange,
                500,
                "{\"erro\":\"Erro ao carregar a página.\"}"
            );
            return;
        }

        exchange.getResponseHeaders().set(
            "Content-Type",
            "text/html; charset=UTF-8"
        );
        exchange.getResponseHeaders().set(
            "X-Content-Type-Options",
            "nosniff"
        );
        exchange.getResponseHeaders().set(
            "Cache-Control",
            "no-store"
        );
        exchange.sendResponseHeaders(200, html.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(html);
        }
    }

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response
    ) throws IOException {

        byte[] responseBytes =
            response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type",
            "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
            statusCode,
            responseBytes.length
        );

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(responseBytes);
        }
    }
}