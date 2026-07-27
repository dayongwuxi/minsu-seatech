package com.seatech.minsu.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageUrlsTest {

    @Test
    void keepsOnlyFilesUrlsInOrder() {
        List<String> in = Arrays.asList(
                "/files/a.jpg",
                "blob:https://seabnb.axionintell.com/055946e9",
                "/files/b.png",
                "https://evil.com/x.jpg",
                null,
                "/files/c.webp"
        );
        assertEquals(List.of("/files/a.jpg", "/files/b.png", "/files/c.webp"), ImageUrls.sanitize(in));
    }

    @Test
    void nullReturnsEmpty() {
        assertTrue(ImageUrls.sanitize(null).isEmpty());
    }

    @Test
    void allInvalidReturnsEmpty() {
        List<String> in = Arrays.asList("blob:xyz", "http://x/y.jpg", "", "files/no-slash.jpg");
        assertTrue(ImageUrls.sanitize(in).isEmpty());
    }

    @Test
    void emptyListReturnsEmpty() {
        assertTrue(ImageUrls.sanitize(Collections.emptyList()).isEmpty());
    }
}
