package nl.bertriksikken.shapefile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public final class DbfStringTest {

    @Test
    public void test() {
        byte[] bytes = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
        String s = new String(bytes, StandardCharsets.US_ASCII);
        s = s.trim();
        Assertions.assertEquals(0, s.length());
    }
}
