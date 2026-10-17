package com.eridani.achernar.identity.admin;

import com.eridani.achernar.identity.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Cursor opaco da consulta de eventos (chave composta {@code occurred_at, id}). */
public record EventCursor(Instant at, UUID id) {

    public String encode() {
        String raw = at.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static EventCursor decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int bar = raw.indexOf('|');
            return new EventCursor(Instant.parse(raw.substring(0, bar)), UUID.fromString(raw.substring(bar + 1)));
        } catch (RuntimeException e) {
            throw ApiException.validation("cursor", "INVALID");
        }
    }
}
