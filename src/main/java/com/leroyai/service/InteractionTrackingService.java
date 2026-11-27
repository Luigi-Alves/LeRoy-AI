package com.leroyai.service;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.leroyai.model.Produto;
import com.leroyai.util.JsonUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InteractionTrackingService {

    private static final Type EVENT_LIST_TYPE =
        new TypeToken<List<InteractionEvent>>() { }.getType();

    private final Path dataFile;

    public InteractionTrackingService(Path dataFile) {
        this.dataFile = dataFile.toAbsolutePath().normalize();
    }

    public static boolean isValidUuid(String value) {
        if (value == null) {
            return false;
        }

        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void recordChatInteraction(
            String sessionId,
            String interactionId
    ) throws IOException {
        if (!isValidUuid(sessionId)) {
            return;
        }

        List<InteractionEvent> events = new ArrayList<>();
        events.add(event(
            "CHAT_INTERACTION", sessionId, interactionId,
            null, null, null, null, null, 1
        ));
        append(events);
    }

    public void recordChatResults(
            String sessionId,
            String interactionId,
            List<Produto> products
    ) throws IOException {
        if (!isValidUuid(sessionId)) {
            return;
        }

        List<InteractionEvent> events = new ArrayList<>();
        events.add(event(
            "SEARCH", sessionId, interactionId,
            null, null, null, searchTopic(products), null, null
        ));
        addSearchResultEvents(events, sessionId, interactionId, products);

        for (Produto product : products) {
            events.add(event(
                "RECOMMENDATION", sessionId, interactionId,
                product.getId(), product.getNome(), product.getCategoria(),
                null, null, null
            ));
        }

        append(events);
    }

    public void recordChatSearchFailure(
            String sessionId,
            String interactionId
    ) throws IOException {
        if (!isValidUuid(sessionId)) {
            return;
        }

        append(List.of(event(
            "SEARCH", sessionId, interactionId,
            null, null, null, "sem correspondência", null, null
        )));
    }

    public void recordSearch(
            String sessionId,
            List<Produto> products
    ) throws IOException {
        if (!isValidUuid(sessionId)) {
            return;
        }

        String interactionId = UUID.randomUUID().toString();
        List<InteractionEvent> events = new ArrayList<>();
        events.add(event(
            "SEARCH", sessionId, interactionId,
            null, null, null, searchTopic(products), null, null
        ));
        addSearchResultEvents(events, sessionId, interactionId, products);
        append(events);
    }

    public void recordProductSelection(
            String sessionId,
            String interactionId,
            Produto product
    ) throws IOException {
        append(List.of(event(
            "PRODUCT_SELECTION", sessionId, interactionId,
            product.getId(), product.getNome(), product.getCategoria(),
            null, null, null
        )));
    }

    public boolean hasRecommendation(String sessionId, String interactionId)
            throws IOException {
        if (!isValidUuid(sessionId) || !isValidUuid(interactionId)) {
            return false;
        }

        return readEvents().stream().anyMatch(event ->
            "RECOMMENDATION".equals(event.eventType())
                && sessionId.equals(event.sessionId())
                && interactionId.equals(event.interactionId())
        );
    }

    public synchronized boolean recordRatingIfAbsent(
            String sessionId,
            String interactionId,
            String rating
    ) throws IOException {
        List<InteractionEvent> events = readEvents();
        boolean alreadyRated = events.stream().anyMatch(event ->
            "RATING".equals(event.eventType())
                && sessionId.equals(event.sessionId())
                && interactionId.equals(event.interactionId())
        );
        if (alreadyRated) {
            return false;
        }

        append(List.of(event(
            "RATING", sessionId, interactionId,
            null, null, null, null, rating, null
        )));
        return true;
    }

    /** Resumo agregado e anônimo, sem texto livre nem identificadores de sessão. */
    public synchronized Map<String, Object> resumo() throws IOException {
        List<InteractionEvent> events = readEvents();

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("totalEventos", events.size());
        resumo.put("interacoesDeChat", contar(events, "CHAT_INTERACTION"));
        resumo.put("sessoesAnonimas", events.stream()
            .map(InteractionEvent::sessionId)
            .filter(Objects::nonNull)
            .distinct()
            .count());
        resumo.put("buscasSemCorrespondencia", events.stream()
            .filter(e -> "SEARCH".equals(e.eventType()))
            .filter(e -> "sem correspondência".equals(e.searchTerm()))
            .count());
        resumo.put("categoriasMaisBuscadas",
            ranking(events, "CATEGORY_SEARCH", InteractionEvent::category));
        resumo.put("produtosMaisRecomendados",
            ranking(events, "RECOMMENDATION", InteractionEvent::productName));
        resumo.put("produtosMaisSelecionadosNoMapa",
            ranking(events, "PRODUCT_SELECTION", InteractionEvent::productName));

        Map<String, Object> avaliacoes = new LinkedHashMap<>();
        avaliacoes.put("positivas", events.stream()
            .filter(e -> "RATING".equals(e.eventType()))
            .filter(e -> "positive".equals(e.rating()))
            .count());
        avaliacoes.put("negativas", events.stream()
            .filter(e -> "RATING".equals(e.eventType()))
            .filter(e -> "negative".equals(e.rating()))
            .count());
        resumo.put("avaliacoes", avaliacoes);

        return resumo;
    }

    private long contar(List<InteractionEvent> events, String tipo) {
        return events.stream()
            .filter(e -> tipo.equals(e.eventType()))
            .count();
    }

    private Map<String, Long> ranking(
            List<InteractionEvent> events,
            String tipo,
            Function<InteractionEvent, String> campo
    ) {
        return events.stream()
            .filter(e -> tipo.equals(e.eventType()))
            .map(campo)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                Function.identity(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                LinkedHashMap::new
            ));
    }

    private synchronized void append(List<InteractionEvent> newEvents)
            throws IOException {
        if (newEvents.isEmpty()) {
            return;
        }

        List<InteractionEvent> events = readEvents();
        events.addAll(newEvents);

        Path parent = dataFile.getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(
            parent,
            "interactions-",
            ".tmp"
        );

        try {
            Files.writeString(
                temporaryFile,
                JsonUtil.toJson(events),
                StandardCharsets.UTF_8
            );

            try {
                Files.move(
                    temporaryFile,
                    dataFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                    temporaryFile,
                    dataFile,
                    StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private List<InteractionEvent> readEvents() throws IOException {
        if (!Files.exists(dataFile)) {
            return new ArrayList<>();
        }

        String json = Files.readString(dataFile, StandardCharsets.UTF_8);
        try {
            List<InteractionEvent> events =
                JsonUtil.fromJson(json, EVENT_LIST_TYPE);
            if (events == null) {
                throw new IOException("O arquivo de interações está vazio ou inválido.");
            }
            return new ArrayList<>(events);
        } catch (JsonParseException e) {
            throw new IOException("Não foi possível ler o arquivo de interações.", e);
        }
    }

    private void addSearchResultEvents(
            List<InteractionEvent> events,
            String sessionId,
            String interactionId,
            List<Produto> products
    ) {
        if (products == null || products.isEmpty()) {
            return;
        }

        LinkedHashSet<String> categories = new LinkedHashSet<>();
        for (Produto product : products) {
            if (product.getCategoria() != null
                    && !product.getCategoria().isBlank()) {
                categories.add(product.getCategoria());
            }
            events.add(event(
                "PRODUCT_SEARCH", sessionId, interactionId,
                product.getId(), product.getNome(), product.getCategoria(),
                null, null, null
            ));
        }

        for (String category : categories) {
            events.add(event(
                "CATEGORY_SEARCH", sessionId, interactionId,
                null, null, category, null, null, null
            ));
        }
    }

    private InteractionEvent event(
            String eventType,
            String sessionId,
            String interactionId,
            Integer productId,
            String productName,
            String category,
            String searchTerm,
            String rating,
            Integer itemCount
    ) {
        return new InteractionEvent(
            UUID.randomUUID().toString(),
            eventType,
            Instant.now().toString(),
            sessionId,
            interactionId,
            searchTerm,
            productId,
            productName,
            category,
            rating,
            itemCount
        );
    }

    private String searchTopic(List<Produto> products) {
        if (products == null || products.isEmpty()) {
            return "sem correspondência";
        }

        return products.stream()
            .map(Produto::getCategoria)
            .filter(category -> category != null && !category.isBlank())
            .distinct()
            .collect(Collectors.joining(", "));
    }

    public record InteractionEvent(
            String eventId,
            String eventType,
            String timestamp,
            String sessionId,
            String interactionId,
            String searchTerm,
            Integer productId,
            String productName,
            String category,
            String rating,
            Integer itemCount
    ) {
    }

}