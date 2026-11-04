package com.gestourant.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class ColombiaTime {
    public static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private ColombiaTime() {}

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
