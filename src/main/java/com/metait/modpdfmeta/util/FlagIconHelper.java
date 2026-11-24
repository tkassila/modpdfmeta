package com.metait.modpdfmeta.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Apuluokka aitojen lippukuvakkeiden lataamiseen ja näyttämiseen.
 */
public class FlagIconHelper {

    private static final Map<String, Image> CACHE = new HashMap<>();

    public static Image getFlagImage(String langCode) {
        if (langCode == null) return null;
        String key = langCode.toLowerCase().trim();
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }

        String path = "/com/metait/modpdfmeta/icons/flags/" + key + ".png";
        try (InputStream is = FlagIconHelper.class.getResourceAsStream(path)) {
            if (is != null) {
                Image img = new Image(is);
                CACHE.put(key, img);
                return img;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static ImageView createFlagView(String langCode, double width, double height) {
        Image img = getFlagImage(langCode);
        if (img == null) return null;
        ImageView iv = new ImageView(img);
        iv.setFitWidth(width);
        iv.setFitHeight(height);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }
}
