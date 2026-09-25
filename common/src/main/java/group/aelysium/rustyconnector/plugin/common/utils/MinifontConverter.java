package group.aelysium.rustyconnector.plugin.common.utils;

import java.text.Normalizer;

public class MinifontConverter {

    public static String convert(String text) {
        if (text == null) return null;

        // Remove accents and diacritics
        String stripped = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        StringBuilder res = new StringBuilder(stripped.length());
        for (char c : stripped.toCharArray()) {
            String converted = switch (c) {
                case 'a' -> "ᴀ"; case 'b' -> "ʙ"; case 'c' -> "ᴄ";
                case 'd' -> "ᴅ"; case 'e' -> "ᴇ"; case 'f' -> "ꜰ";
                case 'g' -> "ɢ"; case 'h' -> "ʜ"; case 'i' -> "ɪ";
                case 'j' -> "ᴊ"; case 'k' -> "ᴋ"; case 'l' -> "ʟ";
                case 'm' -> "ᴍ"; case 'n' -> "ɴ"; case 'o' -> "ᴏ";
                case 'p' -> "ᴘ"; case 'q' -> "ǫ"; case 'r' -> "ʀ";
                case 's' -> "ѕ"; case 't' -> "ᴛ"; case 'u' -> "ᴜ";
                case 'v' -> "ᴠ"; case 'w' -> "ᴡ"; case 'x' -> "x";
                case 'y' -> "ʏ"; case 'z' -> "ᴢ";
                default  -> String.valueOf(c);
            };
            res.append(converted);
        }
        return res.toString();
    }
}