package dev.punchboost;

import dev.punchboost.config.PunchBoostConfig;
import dev.punchboost.config.PunchBoostConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PunchBoostClient implements ClientModInitializer {
    public static final String MOD_ID = "punchboost";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 1.21.9+ key categories are objects, not translation strings.
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    public static KeyBinding boostKey;
    public static KeyBinding toggleKey;
    public static KeyBinding configKey;

    @Override
    public void onInitializeClient() {
        PunchBoostConfig.load();

        boostKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.punchboost.boost", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.punchboost.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.punchboost.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));

        ServerOptOut.register();

        // Drive the boost sequence at the START of each tick, so the rotation we set
        // is what the player's movement packet sends to the server this tick.
        ClientTickEvents.START_CLIENT_TICK.register(BoostController::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(PunchBoostClient::handleKeys);

        LOGGER.info("Punch Boost loaded");
    }

    private static void handleKeys(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        while (configKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new PunchBoostConfigScreen(null));
            }
        }

        while (toggleKey.wasPressed()) {
            PunchBoostConfig cfg = PunchBoostConfig.get();
            cfg.enabled = !cfg.enabled;
            PunchBoostConfig.save();
            client.player.sendMessage(Text.translatable(
                    cfg.enabled ? "punchboost.msg.enabled" : "punchboost.msg.disabled")
                    .formatted(cfg.enabled ? Formatting.GREEN : Formatting.RED), true);
        }

        while (boostKey.wasPressed()) {
            BoostController.request();
        }
    }
}
