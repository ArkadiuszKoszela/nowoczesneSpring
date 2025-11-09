package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom deserializer dla pola quantity w Input
 * Obsługuje null, undefined, stringi, liczby i NaN
 */
public class InputQuantityDeserializer extends JsonDeserializer<Double> {

    @Override
    public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node == null || node.isNull()) {
            return null;
        }
        
        if (node.isNumber()) {
            double value = node.asDouble();
            if (Double.isNaN(value) || !Double.isFinite(value)) {
                return null;
            }
            return value;
        }
        
        if (node.isTextual()) {
            String text = node.asText();
            if (text == null || text.trim().isEmpty() || 
                "null".equalsIgnoreCase(text) || 
                "undefined".equalsIgnoreCase(text) ||
                "NaN".equalsIgnoreCase(text)) {
                return null;
            }
            try {
                double value = Double.parseDouble(text);
                if (Double.isNaN(value) || !Double.isFinite(value)) {
                    return null;
                }
                return value;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        // Dla innych typów zwróć null
        return null;
    }
}








