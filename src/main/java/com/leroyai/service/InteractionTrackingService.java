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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
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
