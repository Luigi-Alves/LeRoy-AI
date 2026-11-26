
package com.leroyai;

import com.google.gson.JsonObject;

import com.leroyai.controller.ChatController;
import com.leroyai.model.Produto;
import com.leroyai.service.GeminiService;
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
import java.util.List;
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

        // Serviço de produtos
        ProdutoService produtoService = new ProdutoService();
        ChatController chatController =
            new ChatController(produtoService, new GeminiService());

        // -----------------------------------------
        // 1. Verificar se o servidor está funcionando
        // GET /api/health
        // -----------------------------------------

        server.createContext("/api/health", exchange -> {

            JsonObject response = new JsonObject();

            response.addProperty("status", "online");
            response.addProperty(
                "mensagem",
                "LeRoy AI Assistant está funcionando!"
            );

            sendResponse(exchange, 200, JsonUtil.toJson(response));
        });

        // -----------------------------------------
        // 2. Listar produtos do catálogo demonstrativo
        // GET /api/products
        // -----------------------------------------

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

        // -----------------------------------------
        // 3. Buscar produtos pelo termo informado
        // GET /api/search?q=tinta
        // -----------------------------------------

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

        // -----------------------------------------
        // 4. Chat com inteligência artificial
        // POST /api/chat
        // -----------------------------------------

        server.createContext("/api/chat", chatController::handle);

        // -----------------------------------------
        // 5. Página inicial
        // GET /
        // -----------------------------------------

        server.createContext("/", exchange -> {

            if (!exchange.getRequestURI().getPath().equals("/")) {
                sendResponse(
                    exchange,
                    404,
                    "{\"erro\":\"Rota não encontrada.\"}"
                );
                return;
            }

            try {

                InputStream input = Main.class
                    .getResourceAsStream("/static/index.html");

                if (input == null) {

                    String mensagem =
                        "Arquivo index.html não encontrado.";

                    exchange.sendResponseHeaders(404, 0);

                    try (OutputStream output =
                             exchange.getResponseBody()) {

                        output.write(
                            mensagem.getBytes(StandardCharsets.UTF_8)
                        );
                    }

                    return;
                }

                byte[] html;

                try (input) {
                    html = input.readAllBytes();
                }

                exchange.getResponseHeaders().set(
                    "Content-Type",
                    "text/html; charset=UTF-8"
                );

                exchange.sendResponseHeaders(200, html.length);

                try (OutputStream output =
                         exchange.getResponseBody()) {

                    output.write(html);
                }

            } catch (Exception e) {

                e.printStackTrace();

                sendResponse(
                    exchange,
                    500,
                    "{\"erro\":\"Erro ao carregar a página inicial.\"}"
                );
            }
        });

        // -----------------------------------------
        // Inicialização do servidor
        // -----------------------------------------

        server.setExecutor(
            Executors.newFixedThreadPool(10)
        );

        server.start();

        System.out.println(
            "LeRoy AI Assistant iniciado na porta " + port
        );
    }

    // -----------------------------------------
    // Método para enviar respostas JSON
    // -----------------------------------------

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

        try (OutputStream output =
                 exchange.getResponseBody()) {

            output.write(responseBytes);
        }
    }
}