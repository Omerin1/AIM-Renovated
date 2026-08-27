package aimclassic;

import java.util.LinkedHashMap;
import java.util.Map;

/** Flat JSON object with string values — enough for the AIM wire protocol. */
public final class Json {
    public final Map<String, String> fields = new LinkedHashMap<>();

    public Json put(String k, String v) {
        fields.put(k, v == null ? "" : v);
        return this;
    }

    public String get(String k) {
        String v = fields.get(k);
        return v == null ? "" : v;
    }

    public String type() {
        return get("type");
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, String> e : fields.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            sb.append('"').append(escape(e.getValue())).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    public static Json of(String type) {
        return new Json().put("type", type);
    }

    public static Json parse(String raw) {
        Json j = new Json();
        if (raw == null) {
            return j;
        }
        String s = raw.trim();
        if (s.length() < 2 || s.charAt(0) != '{') {
            return j;
        }
        int i = 1;
        while (i < s.length()) {
            i = skipWs(s, i);
            if (i >= s.length() || s.charAt(i) == '}') {
                break;
            }
            if (s.charAt(i) != '"') {
                break;
            }
            ParseStr key = readString(s, i);
            i = skipWs(s, key.end);
            if (i >= s.length() || s.charAt(i) != ':') {
                break;
            }
            i = skipWs(s, i + 1);
            if (i >= s.length() || s.charAt(i) != '"') {
                break;
            }
            ParseStr val = readString(s, i);
            j.fields.put(key.value, val.value);
            i = skipWs(s, val.end);
            if (i < s.length() && s.charAt(i) == ',') {
                i++;
            }
        }
        return j;
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static ParseStr readString(String s, int startQuote) {
        StringBuilder out = new StringBuilder();
        int i = startQuote + 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') {
                return new ParseStr(out.toString(), i + 1);
            }
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                switch (n) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    default -> out.append(n);
                }
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return new ParseStr(out.toString(), i);
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private record ParseStr(String value, int end) {}
}
