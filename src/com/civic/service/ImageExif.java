package com.civic.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Minimal JPEG EXIF GPS reader; returns null when no usable GPS IFD is present. */
public final class ImageExif {
    private ImageExif() {}

    public static double[] readGps(byte[] image) {
        try {
            int pos = 2;
            while (pos + 4 < image.length && (image[pos] & 0xff) == 0xff) {
                int marker = image[pos + 1] & 0xff;
                int len = u16(image, pos + 2, false);
                if (len < 2 || pos + 2 + len > image.length) return null;
                if (marker == 0xe1 && len > 8 && ascii(image, pos + 4, "Exif\0\0"))
                    return parseTiff(image, pos + 10, pos + 2 + len);
                if (marker == 0xda || marker == 0xd9) break;
                pos += 2 + len;
            }
        } catch (RuntimeException ignored) {}
        return null;
    }

    private static double[] parseTiff(byte[] b, int base, int end) {
        boolean little = b[base] == 'I' && b[base + 1] == 'I';
        if (!little && !(b[base] == 'M' && b[base + 1] == 'M')) return null;
        if (u16(b, base + 2, little) != 42) return null;
        int ifd = base + (int) u32(b, base + 4, little);
        int count = u16(b, ifd, little), gpsOffset = -1;
        for (int i = 0; i < count; i++) {
            int e = ifd + 2 + i * 12;
            if (e + 12 > end) return null;
            if (u16(b, e, little) == 0x8825) gpsOffset = (int) u32(b, e + 8, little);
        }
        if (gpsOffset < 0) return null;
        int gps = base + gpsOffset, gpsCount = u16(b, gps, little);
        double[] lat = null, lon = null; char latRef = 'N', lonRef = 'E';
        for (int i = 0; i < gpsCount; i++) {
            int e = gps + 2 + i * 12;
            if (e + 12 > end) return null;
            int tag = u16(b, e, little), type = u16(b, e + 2, little), n = (int) u32(b, e + 4, little);
            if (tag == 1 || tag == 3) {
                int value = n <= 4 ? e + 8 : base + (int) u32(b, e + 8, little);
                if (value < base || value + n > end) continue;
                char ref = (char) b[value];
                if (tag == 1) latRef = ref; else lonRef = ref;
            } else if ((tag == 2 || tag == 4) && type == 5 && n >= 3) {
                int value = base + (int) u32(b, e + 8, little);
                if (value < base || value + 24 > end) continue;
                double[] d = new double[3];
                for (int j = 0; j < 3; j++) d[j] = u32(b, value + j * 8, little) / u32(b, value + j * 8 + 4, little);
                if (tag == 2) lat = d; else lon = d;
            }
        }
        if (lat == null || lon == null) return null;
        double latitude = lat[0] + lat[1] / 60 + lat[2] / 3600;
        double longitude = lon[0] + lon[1] / 60 + lon[2] / 3600;
        if (latRef == 'S') latitude = -latitude;
        if (lonRef == 'W') longitude = -longitude;
        return new double[]{latitude, longitude};
    }

    private static int u16(byte[] b, int p, boolean le) { return le ? (b[p]&255) | ((b[p+1]&255)<<8) : ((b[p]&255)<<8) | (b[p+1]&255); }
    private static long u32(byte[] b, int p, boolean le) {
        ByteBuffer bb = ByteBuffer.wrap(b, p, 4).order(le ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        return Integer.toUnsignedLong(bb.getInt());
    }
    private static boolean ascii(byte[] b, int p, String s) { for (int i=0;i<s.length();i++) if (b[p+i] != (byte)s.charAt(i)) return false; return true; }
}
