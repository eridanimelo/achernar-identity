package com.eridani.achernar.identity.shared.web;

import java.io.IOException;

/** Corpo da requisição acima de P-48. */
public class BodyTooLargeException extends IOException {

    public BodyTooLargeException(long limit) {
        super("Corpo da requisição excede " + limit + " bytes");
    }
}
