package com.proximityservice.common.geohash;

import java.util.ArrayList;
import java.util.List;

public final class GeoHashUtil {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static final int[] PRECISIONS = {4, 5, 6};

    private static final String[][] NEIGHBORS = {
        {"bc01fg45238967deuvhjyznpkmstqrwx", "p0r21436x8zb9dcf5h7kjnmqesgutwvy"},
        {"238967debc01fg45uvhjyznpkmstqrwx", "14365h7k9dcfesgujnmqp0r2twvyx8zb"},
        {"p0r21436x8zb9dcf5h7kjnmqesgutwvy", "bc01fg45238967deuvhjyznpkmstqrwx"},
        {"14365h7k9dcfesgujnmqp0r2twvyx8zb", "238967debc01fg45uvhjyznpkmstqrwx"}
    };

    private static final String[][] BORDERS = {
        {"bcfguvyz", "prxz"},
        {"0145hjnp", "028b"},
        {"prxz", "bcfguvyz"},
        {"028b", "0145hjnp"}
    };

    private static final int RIGHT = 0, LEFT = 1, TOP = 2, BOTTOM = 3;

    private GeoHashUtil() {}

    public static String encode(double latitude, double longitude, int precision) {
        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        boolean isLon = true;
        StringBuilder geohash = new StringBuilder();
        int bit = 0;
        int ch = 0;

        while (geohash.length() < precision) {
            if (isLon) {
                double mid = (lonRange[0] + lonRange[1]) / 2;
                if (longitude >= mid) { ch |= (1 << (4 - bit)); lonRange[0] = mid; }
                else { lonRange[1] = mid; }
            } else {
                double mid = (latRange[0] + latRange[1]) / 2;
                if (latitude >= mid) { ch |= (1 << (4 - bit)); latRange[0] = mid; }
                else { latRange[1] = mid; }
            }
            isLon = !isLon;
            if (bit < 4) { bit++; }
            else { geohash.append(BASE32.charAt(ch)); bit = 0; ch = 0; }
        }
        return geohash.toString();
    }

    public static int getPrecisionForRadius(double radiusKm) {
        if (radiusKm <= 0.5) return 6;
        if (radiusKm <= 2.0) return 5;
        return 4;
    }

    public static int[] getAllPrecisions() { return PRECISIONS.clone(); }

    public static List<String> getNeighbors(String geohash) {
        String n = adjacent(geohash, TOP);
        String s = adjacent(geohash, BOTTOM);
        String e = adjacent(geohash, RIGHT);
        String w = adjacent(geohash, LEFT);
        String ne = adjacent(e, TOP);
        String se = adjacent(e, BOTTOM);
        String sw = adjacent(w, BOTTOM);
        String nw = adjacent(w, TOP);
        return List.of(n, ne, e, se, s, sw, w, nw);
    }

    public static List<String> getSearchGeohashes(double latitude, double longitude, double radiusKm) {
        int precision = getPrecisionForRadius(radiusKm);
        String center = encode(latitude, longitude, precision);
        List<String> result = new ArrayList<>();
        result.add(center);
        result.addAll(getNeighbors(center));
        return result;
    }

    private static String adjacent(String geohash, int direction) {
        int lastIndex = geohash.length() - 1;
        char lastChar = geohash.charAt(lastIndex);
        int parity = geohash.length() % 2;
        String parent = geohash.substring(0, lastIndex);
        if (BORDERS[direction][parity].indexOf(lastChar) != -1 && !parent.isEmpty()) {
            parent = adjacent(parent, direction);
        }
        return parent + BASE32.charAt(NEIGHBORS[direction][parity].indexOf(lastChar));
    }
}
