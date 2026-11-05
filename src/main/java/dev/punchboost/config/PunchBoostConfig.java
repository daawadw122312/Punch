package dev.punchboost.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.punchboost.PunchBoostClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain JSON config at .minecraft/config/punchboost.json.
 * Every field can be edited in-game (Mod Menu or the config key) or by hand.
 */
public final class PunchBoostConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("punchboost.json");

    private static PunchBoostConfig instance = new PunchBoostConfig();

    // ---- Settings (defaults) ----

    /** Master switch. */
    public boolean enabled = true;

    /** Pitch to aim at while drawing. -90 = straight up. */
    public float pitch = -90.0f;

    /** Yaw added to your current facing. 0 = facing direction, 180 = behind you. */
    public float yawOffset = 0.0f;

    /** Ticks to draw the bow (3 = minimum that fires, 20 = full charge). Lower = arrow returns sooner. */
    public int chargeTicks = 6;

    /** Ticks to hold the aim before drawing, so the server has your new rotation. */
    public int aimDelayTicks = 1;

    /** Ticks to keep the aim after releasing. */
    public int holdAfterReleaseTicks = 2;

    /** Ticks to wait after a shot before another can start. */
    public int cooldownTicks = 4;

    /** Minimum Punch level a bow needs to be picked. 0 = any bow. */
    public int minPunchLevel = 1;

    /** Snap the camera back to where you were looking afterwards. */
    public boolean restoreRotation = true;

    /** Switch back to the previously held hotbar slot afterwards. */
    public boolean restoreSlot = true;

    /** Keep firing while the boost key is held down. */
    public boolean repeatWhileHeld = false;

    /** Show action-bar messages (no bow found, disabled by server, ...). */
    public boolean showMessages = true;

    public static PunchBoostConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                PunchBoostConfig loaded = GSON.fromJson(reader, PunchBoostConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                PunchBoostClient.LOGGER.warn("Couldn't read {}, using defaults", FILE, e);
                instance = new PunchBoostConfig();
            }
        }
        instance.clamp();
        save(); // writes defaults on first run and fills in any new fields
    }

    public static void save() {
        instance.clamp();
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            PunchBoostClient.LOGGER.error("Couldn't save {}", FILE, e);
        }
    }

    public static void replace(PunchBoostConfig newConfig) {
        instance = newConfig;
        save();
    }

    public PunchBoostConfig copy() {
        return GSON.fromJson(GSON.toJson(this), PunchBoostConfig.class);
    }

    private void clamp() {
        pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);
        yawOffset = MathHelper.clamp(yawOffset, -180.0f, 180.0f);
        chargeTicks = MathHelper.clamp(chargeTicks, 3, 20);
        aimDelayTicks = MathHelper.clamp(aimDelayTicks, 0, 10);
        holdAfterReleaseTicks = MathHelper.clamp(holdAfterReleaseTicks, 0, 20);
        cooldownTicks = MathHelper.clamp(cooldownTicks, 0, 40);
        minPunchLevel = MathHelper.clamp(minPunchLevel, 0, 5);
    }
}
