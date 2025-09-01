package nl.bertriksikken.datex2;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.util.ArrayList;
import java.util.List;

public final class MultilingualString {

    @JacksonXmlElementWrapper(localName = "values")
    @JacksonXmlProperty(localName = "value")
    List<MultilingualStringValue> values = new ArrayList<>();

    // no-arg constructor for jackson
    private MultilingualString() {
    }

    public MultilingualString(String language, String text) {
        values.add(new MultilingualStringValue(language, text));
    }

    public List<MultilingualStringValue> values() {
        return List.copyOf(values);
    }

    public static final class MultilingualStringValue {
        @JacksonXmlProperty(localName = "lang", isAttribute = true)
        @SuppressWarnings("UnusedVariable")
        private final String language;
        @JacksonXmlText
        private final String value;

        // no-arg jackson constructor
        private MultilingualStringValue() {
            this("", "");
        }

        private MultilingualStringValue(String language, String value) {
            this.language = language;
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}