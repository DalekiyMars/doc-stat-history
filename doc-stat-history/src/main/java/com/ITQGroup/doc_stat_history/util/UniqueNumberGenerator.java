package com.ITQGroup.doc_stat_history.util;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UniqueNumberGenerator {

    private final String prefix;

    public UniqueNumberGenerator(@Value("${app.document.number-prefix:DOC}") String prefix) {
        this.prefix = prefix;
    }

    public String generate() {
        String hex = Long.toHexString(UUID.randomUUID().getMostSignificantBits()).toUpperCase();
        String part = hex.length() >= 12
                ? hex.substring(0, 12)
                : "0".repeat(12 - hex.length()) + hex;
        return prefix + "-" + part;
    }
}