package nl.bertriksikken.shapefile;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class DbfStringTest {

    @Test
    public void test() {
        byte[] bytes = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
        String s = new String(bytes, Charsets.US_ASCII);
        s = s.trim();
        Assertions.assertEquals(0, s.length());
    }
}
