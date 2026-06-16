package de.bund.zrb.msdosgames.infrastructure.archive;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

final class ArchiveJsonValues {

    private ArchiveJsonValues() {
    }

    static String text(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }

        if (element.isJsonPrimitive()) {
            return primitiveText(element.getAsJsonPrimitive());
        }

        if (element.isJsonArray()) {
            return arrayText(element.getAsJsonArray());
        }

        return element.toString();
    }

    static long number(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return 0L;
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            try {
                return element.getAsLong();
            } catch (NumberFormatException exception) {
                return 0L;
            }
        }

        String text = text(element);
        if (text.length() == 0) {
            return 0L;
        }

        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private static String primitiveText(JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return String.valueOf(primitive.getAsBoolean());
        }
        return primitive.getAsString().trim();
    }

    private static String arrayText(JsonArray array) {
        StringBuilder text = new StringBuilder();
        for (JsonElement item : array) {
            String itemText = text(item);
            if (itemText.length() > 0) {
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(itemText);
            }
        }
        return text.toString();
    }
}
