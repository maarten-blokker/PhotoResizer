package nl.debijenkorf.tools.photoresizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import nl.debijenkorf.tools.photoresizer.resizer.Preset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maarten Blokker
 */
public class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    public static final int TARGET_WIDTH = 1108;
    public static final int TARGET_HEIGHT = 1528;

    private static final Preferences PREFERENCES = Preferences.userRoot();

    private static final List<Preset> PRESETS = Arrays.asList(
            new Preset(0D, 70D, 9D),
            new Preset(10D, 70D, 9D),
            new Preset(0D, 80D, 9D),
            new Preset(10D, 80D, 9D),
            new Preset(0D, 90D, 9D),
            new Preset(10D, 90D, 9D)
    );

    public static boolean isDebug() {
        return Boolean.getBoolean("debug");
    }

    public static List<Preset> getPresets() {
        return Collections.unmodifiableList(PRESETS);
    }

    public static Optional<String> getPreference(String key) {
        return Optional.ofNullable(PREFERENCES.get(key, null));
    }

    public static void setPreference(String key, String value) {
        PREFERENCES.put(key, value);
        try {
            PREFERENCES.sync();
        } catch (BackingStoreException ex) {
            LOG.error("Failed to store preference: key=" + key + ", value=" + value, ex);
        }
    }

}
