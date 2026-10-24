package com.civic.util;

import com.civic.model.CivicIssue;
import com.civic.model.Department;
import com.civic.model.TimelineEvent;
import com.civic.service.SmartTriageService;

import java.util.*;

/**
 * Pure Java JSON parser and serializer with zero external dependencies.
 */
public class SimpleJson {

    public static void main(String[] args) {
        String test = "{\"title\":\"crater pothole on highway\",\"description\":\"Fatal danger accident zone near school\"}";
        Map<String, Object> map = parseObject(test);
        System.out.println("Parsed map keys: " + map.keySet());
        System.out.println("Title: " + map.get("title"));
        System.out.println("Desc: " + map.get("description"));

        SmartTriageService triage = new SmartTriageService();
        SmartTriageService.TriageResult res = triage.analyze((String) map.get("title"), (String) map.get("description"));
        System.out.println("Category: " + res.category + ", Urgency: " + res.urgencyScore + ", Keywords: " + res.detectedKeywords);
    }

    // ==========================================
    // Serialization
    // ==========================================

    public static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        serializeValue(obj, sb);
        return sb.toString();
    }

    private static void serializeValue(Object obj, StringBuilder sb) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String) {
            sb.append('"').append(escapeString((String) obj)).append('"');
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj.toString());
        } else if (obj instanceof Enum<?>) {
            sb.append('"').append(((Enum<?>) obj).name()).append('"');
        } else if (obj instanceof Map<?, ?>) {
            serializeMap((Map<?, ?>) obj, sb);
        } else if (obj instanceof Collection<?>) {
            serializeCollection((Collection<?>) obj, sb);
        } else if (obj instanceof CivicIssue) {
            serializeCivicIssue((CivicIssue) obj, sb);
        } else if (obj instanceof Department) {
            serializeDepartment((Department) obj, sb);
        } else if (obj instanceof TimelineEvent) {
            serializeTimelineEvent((TimelineEvent) obj, sb);
        } else {
            sb.append('"').append(escapeString(obj.toString())).append('"');
        }
    }

    private static void serializeMap(Map<?, ?> map, StringBuilder sb) {
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append('"').append(escapeString(String.valueOf(entry.getKey()))).append("\":");
            serializeValue(entry.getValue(), sb);
        }
        sb.append("}");
    }

    private static void serializeCollection(Collection<?> col, StringBuilder sb) {
        sb.append("[");
        boolean first = true;
        for (Object item : col) {
            if (!first) sb.append(",");
            first = false;
            serializeValue(item, sb);
        }
        sb.append("]");
    }

    public static void serializeCivicIssue(CivicIssue issue, StringBuilder sb) {
        sb.append("{");
        sb.append("\"id\":").append(toJson(issue.getId())).append(",");
        sb.append("\"title\":").append(toJson(issue.getTitle())).append(",");
        sb.append("\"description\":").append(toJson(issue.getDescription())).append(",");
        sb.append("\"category\":").append(toJson(issue.getCategory())).append(",");
        sb.append("\"priority\":").append(toJson(issue.getPriority() != null ? issue.getPriority().name() : "MEDIUM")).append(",");
        sb.append("\"priorityLabel\":").append(toJson(issue.getPriority() != null ? issue.getPriority().getDisplayName() : "Medium")).append(",");
        sb.append("\"priorityColor\":").append(toJson(issue.getPriority() != null ? issue.getPriority().getColorCode() : "#FBBF24")).append(",");
        sb.append("\"status\":").append(toJson(issue.getStatus() != null ? issue.getStatus().name() : "REPORTED")).append(",");
        sb.append("\"statusLabel\":").append(toJson(issue.getStatus() != null ? issue.getStatus().getLabel() : "Reported")).append(",");
        sb.append("\"department\":").append(toJson(issue.getDepartment())).append(",");
        sb.append("\"assignedOfficer\":").append(toJson(issue.getAssignedOfficer())).append(",");
        sb.append("\"address\":").append(toJson(issue.getAddress())).append(",");
        sb.append("\"ward\":").append(toJson(issue.getWard())).append(",");
        sb.append("\"latitude\":").append(issue.getLatitude()).append(",");
        sb.append("\"longitude\":").append(issue.getLongitude()).append(",");
        sb.append("\"reporterName\":").append(toJson(issue.getReporterName())).append(",");
        sb.append("\"reporterPhone\":").append(toJson(issue.getReporterPhone())).append(",");
        sb.append("\"reporterEmail\":").append(toJson(issue.getReporterEmail())).append(",");
        sb.append("\"photoUrl\":").append(toJson(issue.getPhotoUrl())).append(",");
        sb.append("\"resolutionPhotoUrl\":").append(toJson(issue.getResolutionPhotoUrl())).append(",");
        sb.append("\"upvotes\":").append(issue.getUpvotes()).append(",");
        sb.append("\"createdAt\":").append(toJson(issue.getCreatedAt())).append(",");
        sb.append("\"updatedAt\":").append(toJson(issue.getUpdatedAt())).append(",");
        sb.append("\"resolvedAt\":").append(toJson(issue.getResolvedAt())).append(",");
        sb.append("\"slaHours\":").append(issue.getSlaHours()).append(",");
        sb.append("\"isSlaBreached\":").append(issue.isSlaBreached()).append(",");
        sb.append("\"remainingSlaHours\":").append(issue.getRemainingSlaHours()).append(",");
        sb.append("\"resolutionNotes\":").append(toJson(issue.getResolutionNotes())).append(",");
        sb.append("\"timeline\":").append(toJson(issue.getTimeline()));
        sb.append("}");
    }

    public static void serializeDepartment(Department dept, StringBuilder sb) {
        sb.append("{");
        sb.append("\"id\":").append(toJson(dept.getId())).append(",");
        sb.append("\"name\":").append(toJson(dept.getName())).append(",");
        sb.append("\"code\":").append(toJson(dept.getCode())).append(",");
        sb.append("\"categoryKey\":").append(toJson(dept.getCategoryKey())).append(",");
        sb.append("\"icon\":").append(toJson(dept.getIcon())).append(",");
        sb.append("\"headOfficer\":").append(toJson(dept.getHeadOfficer())).append(",");
        sb.append("\"contactEmail\":").append(toJson(dept.getContactEmail())).append(",");
        sb.append("\"phone\":").append(toJson(dept.getPhone())).append(",");
        sb.append("\"activeOfficers\":").append(dept.getActiveOfficers());
        sb.append("}");
    }

    public static void serializeTimelineEvent(TimelineEvent evt, StringBuilder sb) {
        sb.append("{");
        sb.append("\"timestamp\":").append(toJson(evt.getTimestamp())).append(",");
        sb.append("\"status\":").append(toJson(evt.getStatus())).append(",");
        sb.append("\"actor\":").append(toJson(evt.getActor())).append(",");
        sb.append("\"action\":").append(toJson(evt.getAction())).append(",");
        sb.append("\"note\":").append(toJson(evt.getNote()));
        sb.append("}");
    }

    public static String escapeString(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // ==========================================
    // Parsing
    // ==========================================

    public static Object parse(String json) {
        if (json == null) return null;
        CharReader reader = new CharReader(json.trim());
        return parseValue(reader);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object val = parse(json);
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        Object val = parse(json);
        if (val instanceof List) {
            return (List<Object>) val;
        }
        return new ArrayList<>();
    }

    private static Object parseValue(CharReader reader) {
        reader.skipWhitespace();
        if (reader.isEnd()) return null;

        char c = reader.peek();
        if (c == '{') {
            return parseObjectInternal(reader);
        } else if (c == '[') {
            return parseArrayInternal(reader);
        } else if (c == '"' || c == '\'') {
            return parseString(reader);
        } else if (c == 't' || c == 'f') {
            // Check if boolean or string
            return parseBoolean(reader);
        } else if (c == 'n') {
            return parseNull(reader);
        } else if (Character.isDigit(c) || (c == '-' && reader.hasMoreDigits())) {
            return parseNumber(reader);
        } else {
            return parseUnquotedString(reader);
        }
    }

    private static Map<String, Object> parseObjectInternal(CharReader reader) {
        Map<String, Object> map = new LinkedHashMap<>();
        reader.expect('{');
        reader.skipWhitespace();

        if (reader.peek() == '}') {
            reader.read(); // consume '}'
            return map;
        }

        while (!reader.isEnd()) {
            reader.skipWhitespace();
            char c = reader.peek();
            String key;
            if (c == '"' || c == '\'') {
                key = parseString(reader);
            } else {
                key = parseUnquotedString(reader);
            }

            reader.skipWhitespace();
            if (reader.peek() == ':') {
                reader.read();
            }
            reader.skipWhitespace();
            Object value = parseValue(reader);
            if (key != null && !key.isEmpty()) {
                map.put(key, value);
            }

            reader.skipWhitespace();
            char next = reader.peek();
            if (next == '}') {
                reader.read();
                break;
            } else if (next == ',') {
                reader.read();
                continue;
            } else {
                break;
            }
        }
        return map;
    }

    private static List<Object> parseArrayInternal(CharReader reader) {
        List<Object> list = new ArrayList<>();
        reader.expect('[');
        reader.skipWhitespace();

        if (reader.peek() == ']') {
            reader.read();
            return list;
        }

        while (!reader.isEnd()) {
            reader.skipWhitespace();
            Object item = parseValue(reader);
            list.add(item);

            reader.skipWhitespace();
            char next = reader.peek();
            if (next == ']') {
                reader.read();
                break;
            } else if (next == ',') {
                reader.read();
                continue;
            } else {
                break;
            }
        }
        return list;
    }

    private static String parseString(CharReader reader) {
        char quote = reader.read();
        StringBuilder sb = new StringBuilder();
        while (!reader.isEnd()) {
            char c = reader.read();
            if (c == quote) {
                return sb.toString();
            }
            if (c == '\\') {
                if (reader.isEnd()) break;
                char escaped = reader.read();
                switch (escaped) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        StringBuilder hex = new StringBuilder();
                        for (int i = 0; i < 4 && !reader.isEnd(); i++) {
                            hex.append(reader.read());
                        }
                        try {
                            sb.append((char) Integer.parseInt(hex.toString(), 16));
                        } catch (NumberFormatException ignored) {}
                        break;
                    default:
                        sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String parseUnquotedString(CharReader reader) {
        StringBuilder sb = new StringBuilder();
        while (!reader.isEnd()) {
            char c = reader.peek();
            if (c == ':' || c == ',' || c == '}' || c == ']' || c == '\n' || c == '\r') {
                break;
            }
            sb.append(reader.read());
        }
        return sb.toString().trim();
    }

    private static Number parseNumber(CharReader reader) {
        StringBuilder sb = new StringBuilder();
        boolean isDouble = false;
        while (!reader.isEnd()) {
            char c = reader.peek();
            if (Character.isDigit(c) || c == '-' || c == '+' || c == 'e' || c == 'E') {
                sb.append(reader.read());
            } else if (c == '.') {
                isDouble = true;
                sb.append(reader.read());
            } else {
                break;
            }
        }
        String str = sb.toString();
        try {
            if (isDouble) {
                return Double.parseDouble(str);
            } else {
                return Long.parseLong(str);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Boolean parseBoolean(CharReader reader) {
        if (reader.peek() == 't') {
            for (int i = 0; i < 4 && !reader.isEnd(); i++) reader.read();
            return Boolean.TRUE;
        } else {
            for (int i = 0; i < 5 && !reader.isEnd(); i++) reader.read();
            return Boolean.FALSE;
        }
    }

    private static Object parseNull(CharReader reader) {
        for (int i = 0; i < 4 && !reader.isEnd(); i++) reader.read();
        return null;
    }

    // Helper reader
    private static class CharReader {
        private final String src;
        private int pos = 0;

        public CharReader(String src) {
            this.src = src;
        }

        public boolean isEnd() {
            return pos >= src.length();
        }

        public boolean hasMoreDigits() {
            return pos + 1 < src.length() && Character.isDigit(src.charAt(pos + 1));
        }

        public char peek() {
            if (isEnd()) return '\0';
            return src.charAt(pos);
        }

        public char read() {
            if (isEnd()) return '\0';
            return src.charAt(pos++);
        }

        public void expect(char expected) {
            char c = read();
            if (c != expected) {
                // soft skip or tolerate
            }
        }

        public void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(peek())) {
                pos++;
            }
        }
    }
}
