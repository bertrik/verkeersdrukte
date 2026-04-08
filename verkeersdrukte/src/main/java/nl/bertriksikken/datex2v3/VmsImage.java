package nl.bertriksikken.datex2v3;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Base64;

public record VmsImage(@JacksonXmlProperty(localName = "vms:imageData") String imageData,
                       @JacksonXmlProperty(localName = "vms:imageFormat") String imageFormat) {

    public boolean hasImageData() {
        return !imageData.isEmpty();
    }

    public byte[] asBytes() {
        return Base64.getDecoder().decode(imageData);
    }

}
