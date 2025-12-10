package pl.koszela.nowoczesnebud.Model;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Konwerter dla GroupOption - obsługuje konwersję starych wartości Boolean (TRUE/FALSE) na enum
 * Używany podczas odczytu z bazy danych, gdy mogą być jeszcze stare wartości
 */
@Converter(autoApply = false)
public class GroupOptionConverter implements AttributeConverter<GroupOption, String> {

    @Override
    public String convertToDatabaseColumn(GroupOption attribute) {
        if (attribute == null) {
            return GroupOption.NONE.name();
        }
        return attribute.name();
    }

    @Override
    public GroupOption convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return GroupOption.NONE;
        }
        
        // Obsługa starych wartości Boolean (TRUE/FALSE) z bazy danych
        String upperDbData = dbData.trim().toUpperCase();
        
        if ("TRUE".equals(upperDbData) || "1".equals(upperDbData) || "true".equalsIgnoreCase(upperDbData)) {
            return GroupOption.MAIN;
        }
        if ("FALSE".equals(upperDbData) || "0".equals(upperDbData) || "false".equalsIgnoreCase(upperDbData)) {
            return GroupOption.OPTIONAL;
        }
        
        // Próba normalnej konwersji enum
        try {
            return GroupOption.valueOf(upperDbData);
        } catch (IllegalArgumentException e) {
            // Jeśli nie można skonwertować, zwróć NONE jako domyślną wartość
            return GroupOption.NONE;
        }
    }
}





