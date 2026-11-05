package dev.punchboost;

import dev.punchboost.config.PunchBoostConfig;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

/**
 * The boost sequence as a small tick-driven state machine:
 *
 *   IDLE -> AIMING   pick the Punch bow, remember slot + view, look where configured
 *   AIMING -> CHARGING   after aimDelayTicks, start drawing (exactly like right-click)
 *   CHARGING -> RELEASED after chargeTicks, release (exactly like letting go of right-click)
 *   RELEASED -> COOLDOWN after holdAfterReleaseTicks, restore view and slot
 *   COOLDOWN -> IDLE     after cooldownTicks
 *
 * Everything goes through the normal client input path (interactItem / stopUsingItem),
 * so the server sees the same packets as a player doing it by hand.
 */
public final class BoostController {
    private enum State { IDLE, AIMING, CHARGING, RELEASED, COOLDOWN }

    private static State state = State.IDLE;
    private static int ticks;
    private static boolean requested;

    private static int savedSlot = -1;
    private static float savedYaw;
    private static float savedPitch;
    private static float targetYaw;
    private static float targetPitch;

    private BoostController() {
    }

    /** Called when the boost key is tapped. Handled on the next tick. */
    public static void request() {
        requested = true;
    }

    public static boolean isActive() {
        return state != State.IDLE;
    }

    public static void onStartTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            state = State.IDLE;
            requested = false;
            return;
        }

        PunchBoostConfig cfg = PunchBoostConfig.get();

        switch (state) {
            case IDLE -> {
                boolean explicit = requested;
                requested = false;
                boolean held = cfg.repeatWhileHeld
                        && client.currentScreen == null
                        && PunchBoostClient.boostKey.isPressed();
                if (!explicit && !held) {
                    return;
                }
                String blockReason = blockReason(client, player, cfg);
                if (blockReason != null) {
                    if (explicit) {
                        notify(player, cfg, blockReason, Formatting.RED);
                    }
                    return;
                }
                start(player, cfg);
            }

            case AIMING -> {
                if (blockReason(client, player, cfg) != null) {
                    finish(client, player, cfg);
                    return;
                }
                applyRotation(player);
                if (++ticks >= cfg.aimDelayTicks) {
                    client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                    if (!player.isUsingItem()) {
                        notify(player, cfg, "punchboost.msg.draw_failed", Formatting.RED);
                        finish(client, player, cfg);
                        return;
                    }
                    // Keep "use" held so vanilla doesn't cancel the draw on its own.
                    client.options.useKey.setPressed(true);
                    state = State.CHARGING;
                    ticks = 0;
                }
            }

            case CHARGING -> {
                if (blockReason(client, player, cfg) != null
                        || !player.isUsingItem()
                        || !player.getMainHandStack().isOf(Items.BOW)) {
                    finish(client, player, cfg);
                    return;
                }
                applyRotation(player);
                if (++ticks >= cfg.chargeTicks) {
                    release(client, player);
                    state = State.RELEASED;
                    ticks = 0;
                }
            }

            case RELEASED -> {
                applyRotation(player);
                if (++ticks >= cfg.holdAfterReleaseTicks) {
                    finish(client, player, cfg);
                }
            }

            case COOLDOWN -> {
                if (++ticks >= cfg.cooldownTicks) {
                    state = State.IDLE;
                    ticks = 0;
                }
            }
        }
    }

    /** Stops any running sequence and puts things back. Safe to call any time. */
    public static void cancel(MinecraftClient client) {
        requested = false;
        if (state == State.IDLE) {
            return;
        }
        if (client.player != null && client.interactionManager != null && state != State.COOLDOWN) {
            finish(client, client.player, PunchBoostConfig.get());
        }
        client.options.useKey.setPressed(false);
        state = State.IDLE;
        ticks = 0;
    }

    // ------------------------------------------------------------------

    private static void start(ClientPlayerEntity player, PunchBoostConfig cfg) {
        PlayerInventory inv = player.getInventory();
        int bowSlot = findBowSlot(player, cfg);
        if (bowSlot < 0) {
            notify(player, cfg,
                    cfg.minPunchLevel > 0 ? "punchboost.msg.no_punch_bow" : "punchboost.msg.no_bow",
                    Formatting.YELLOW);
            return;
        }

        savedSlot = inv.getSelectedSlot();
        savedYaw = player.getYaw();
        savedPitch = player.getPitch();

        targetYaw = savedYaw + cfg.yawOffset;
        targetPitch = MathHelper.clamp(cfg.pitch, -90.0f, 90.0f);

        if (bowSlot != savedSlot) {
            inv.setSelectedSlot(bowSlot); // synced to the server by the interaction manager
        }

        applyRotation(player);
        state = State.AIMING;
        ticks = 0;
    }

    private static void release(MinecraftClient client, ClientPlayerEntity player) {
        client.options.useKey.setPressed(false);
        if (player.isUsingItem()) {
            client.interactionManager.stopUsingItem(player);
        }
    }

    private static void finish(MinecraftClient client, ClientPlayerEntity player, PunchBoostConfig cfg) {
        if (state == State.AIMING || state == State.CHARGING) {
            release(client, player);
        }
        client.options.useKey.setPressed(false);

        if (cfg.restoreRotation) {
            player.setYaw(savedYaw);
            player.setPitch(savedPitch);
        }
        if (cfg.restoreSlot && savedSlot >= 0 && savedSlot < PlayerInventory.getHotbarSize()) {
            player.getInventory().setSelectedSlot(savedSlot);
        }
        savedSlot = -1;
        state = State.COOLDOWN;
        ticks = 0;
    }

    private static void applyRotation(ClientPlayerEntity player) {
        player.setYaw(targetYaw);
        player.setPitch(targetPitch);
    }

    /** Returns a translation key explaining why we can't boost, or null if we can. */
    private static String blockReason(MinecraftClient client, ClientPlayerEntity player, PunchBoostConfig cfg) {
        if (ServerOptOut.isDisabledByServer()) return "punchboost.msg.server_disabled";
        if (!cfg.enabled) return "punchboost.msg.disabled";
        if (client.currentScreen != null) return "punchboost.msg.screen_open";
        if (player.isSpectator() || !player.isAlive()) return "punchboost.msg.cant_now";
        return null;
    }

    /** Prefers the held item, then scans the hotbar left to right. */
    private static int findBowSlot(ClientPlayerEntity player, PunchBoostConfig cfg) {
        PlayerInventory inv = player.getInventory();
        int selected = inv.getSelectedSlot();
        if (isUsableBow(player, inv.getStack(selected), cfg)) {
            return selected;
        }
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            if (isUsableBow(player, inv.getStack(i), cfg)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isUsableBow(ClientPlayerEntity player, ItemStack stack, PunchBoostConfig cfg) {
        if (stack.isEmpty() || !stack.isOf(Items.BOW)) {
            return false;
        }
        if (punchLevel(stack) < cfg.minPunchLevel) {
            return false;
        }
        // Needs an arrow somewhere (Infinity still needs one), unless in creative.
        return player.getAbilities().creativeMode || !player.getProjectileType(stack).isEmpty();
    }

    private static int punchLevel(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesKey(Enchantments.PUNCH)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private static void notify(ClientPlayerEntity player, PunchBoostConfig cfg, String key, Formatting color) {
        if (cfg.showMessages) {
            player.sendMessage(Text.translatable(key).formatted(color), true);
        }
    }
}
