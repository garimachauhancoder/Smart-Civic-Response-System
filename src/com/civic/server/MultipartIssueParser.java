package com.civic.server;

import com.civic.service.ImageExif;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

final class MultipartIssueParser {
    private static final int MAX_REQUEST_BYTES = 10 * 1024 * 1024;
    private MultipartIssueParser() {}

    static Map<String, Object> parse(HttpExchange exchange, Path uploadDir) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String boundary = parameter(contentType, "boundary");
        if (boundary == null || boundary.length() > 200) throw new IllegalArgumentException("Invalid multipart boundary");
        byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) throw new IllegalArgumentException("Request exceeds 10 MB limit");
        String raw = new String(body, StandardCharsets.ISO_8859_1);
        Map<String, Object> fields = new LinkedHashMap<>();
        double[] imageGps = null;
        String delimiter = "--" + boundary;
        int cursor = raw.indexOf(delimiter);
        while (cursor >= 0) {
            cursor += delimiter.length();
            if (raw.startsWith("--", cursor)) break;
            if (raw.startsWith("\r\n", cursor)) cursor += 2;
            int headerEnd = raw.indexOf("\r\n\r\n", cursor);
            if (headerEnd < 0) break;
            String headers = raw.substring(cursor, headerEnd);
            int dataStart = headerEnd + 4;
            int dataEnd = raw.indexOf("\r\n" + delimiter, dataStart);
            if (dataEnd < 0) break;
            String disposition = headerValue(headers, "Content-Disposition");
            String name = parameter(disposition, "name");
            if (name != null) {
                String filename = parameter(disposition, "filename");
                if (filename != null && !filename.isBlank()) {
                    byte[] file = Arrays.copyOfRange(body, dataStart, dataEnd);
                    String type = headerValue(headers, "Content-Type");
                    String ext = imageExtension(type, file);
                    Path absoluteUploadDir = uploadDir.toAbsolutePath().normalize();
                    Files.createDirectories(absoluteUploadDir);
                    String storedName = UUID.randomUUID() + ext;
                    Path destination = absoluteUploadDir.resolve(storedName).normalize();
                    if (!destination.getParent().equals(absoluteUploadDir)) {
                        throw new IllegalArgumentException("Invalid upload path");
                    }
                    Files.write(destination, file);
                    fields.put("photoUrl", "/uploads/" + storedName);
                    imageGps = ImageExif.readGps(file);
                } else {
                    fields.put(name, new String(body, dataStart, dataEnd - dataStart, StandardCharsets.UTF_8));
                }
            }
            cursor = dataEnd + 2;
        }
        if (imageGps == null && fields.containsKey("photoUrl")) {
            fields.put("fraudFlag", true);
            fields.put("fraudReason", "Image has no readable EXIF GPS metadata; location is unverified.");
        } else if (imageGps != null) {
            Double lat = number(fields.get("latitude")), lon = number(fields.get("longitude"));
            if (lat != null && lon != null && distanceMeters(lat, lon, imageGps[0], imageGps[1]) > 1000) {
                fields.put("fraudFlag", true);
                fields.put("fraudReason", "EXIF GPS is more than 1 km from the reported complaint location.");
            }
        }
        return fields;
    }

    private static String imageExtension(String type, byte[] b) {
        if (b.length < 12) throw new IllegalArgumentException("Uploaded image is empty or invalid");
        if (b[0] == (byte)0xff && b[1] == (byte)0xd8 && b[2] == (byte)0xff) return ".jpg";
        if ((b[0]&255)==0x89 && b[1]=='P' && b[2]=='N' && b[3]=='G') return ".png";
        if (b[0]=='R' && b[1]=='I' && b[2]=='F' && b[3]=='F' && b[8]=='W' && b[9]=='E' && b[10]=='B' && b[11]=='P') return ".webp";
        throw new IllegalArgumentException("Only valid JPEG, PNG, and WebP image uploads are supported");
    }
    private static String headerValue(String headers, String name) {
        for (String line : headers.split("\r\n")) if (line.regionMatches(true, 0, name + ":", 0, name.length()+1)) return line.substring(name.length()+1).trim();
        return null;
    }
    private static String parameter(String header, String name) {
        if (header == null) return null;
        for (String part : header.split(";")) {
            String p = part.trim(); int eq = p.indexOf('=');
            if (eq > 0 && p.substring(0, eq).trim().equalsIgnoreCase(name)) {
                String v = p.substring(eq+1).trim();
                if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length()-1);
                return v;
            }
        }
        return null;
    }
    private static Double number(Object o) { try { return o == null ? null : Double.valueOf(o.toString()); } catch (Exception e) { return null; } }
    private static double distanceMeters(double a, double b, double c, double d) {
        double p1=Math.toRadians(a), p2=Math.toRadians(c), dp=Math.toRadians(c-a), dl=Math.toRadians(d-b);
        double h=Math.sin(dp/2)*Math.sin(dp/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dl/2)*Math.sin(dl/2);
        return 6371000*2*Math.atan2(Math.sqrt(h), Math.sqrt(1-h));
    }
}
