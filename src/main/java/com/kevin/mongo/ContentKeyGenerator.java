package com.kevin.mongo;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * (description)
 *
 */
public class ContentKeyGenerator {
    
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    private static int roller = 0;

    public static String generate(String providerId, String typeId) {
        if (typeId.endsWith(".0")) {
            typeId = typeId.substring(0, typeId.length() - 2);
        }

        providerId = fixNull(providerId);
        typeId = fixNull(typeId);

        while (typeId.length() < 8) {
            typeId = "0" + typeId;
        }

        roller = (roller + 1) % 10000;

        return providerId + typeId + FORMAT.format(new Date()) + String.format("%04d", roller);
    }

    private static String fixNull(String str) {
        return (str == null || str.equals("null"))? "": str;
    }
}
