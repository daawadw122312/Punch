package dev.punchboost.config;

import dev.punchboost.ServerOptOut;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Vanilla-widget config screen, so the mod has no required dependencies.
 * Changes are kept on a copy and only saved when you press Done.
 */
public class PunchBoostConfigScreen extends Screen {
    private static final int COL_W = 155;
    private static final int ROW_H = 24;

    private final Screen parent;
    private PunchBoostConfig working;

    public PunchBoostConfigScreen(Screen parent) {
        super(Text.translatable("punchboost.config.title"));
        this.parent = parent;
        this.working = PunchBoostConfig.get().copy();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - COL_W - 5;
        int right = this.width / 2 + 5;
        int top = 36;

        // ---- Left column: aiming & timing ----
        int y = top;
        addToggle(left, y, "enabled", working.enabled, v -> working.enabled = v);
        y += ROW_H;
        addSlider(left, y, "pitch", -90, 90, 1, working.pitch,
                v -> working.pitch = (float) v, v -> String.format("%.0f°", v));
        y += ROW_H;
        addSlider(left, y, "yaw_offset", -180, 180, 5, working.yawOffset,
                v -> working.yawOffset = (float) v, v -> String.format("%+.0f°", v));
        y += ROW_H;
        addSlider(left, y, "charge_ticks", 3, 20, 1, working.chargeTicks,
                v -> working.chargeTicks = (int) v, PunchBoostConfigScreen::ticks);
        y += ROW_H;
        addSlider(left, y, "aim_delay", 0, 10, 1, working.aimDelayTicks,
                v -> working.aimDelayTicks = (int) v, PunchBoostConfigScreen::ticks);
        y += ROW_H;
        addSlider(left, y, "hold_after", 0, 20, 1, working.holdAfterReleaseTicks,
                v -> working.holdAfterReleaseTicks = (int) v, PunchBoostConfigScreen::ticks);
        y += ROW_H;
        addSlider(left, y, "cooldown", 0, 40, 1, working.cooldownTicks,
                v -> working.cooldownTicks = (int) v, PunchBoostConfigScreen::ticks);

        // ---- Right column: behaviour ----
        y = top;
        addSlider(right, y, "min_punch", 0, 5, 1, working.minPunchLevel,
                v -> working.minPunchLevel = (int) v,
                v -> v < 0.5 ? Text.translatable("punchboost.config.any_bow").getString() : String.valueOf((int) v));
        y += ROW_H;
        addToggle(right, y, "restore_rotation", working.restoreRotation, v -> working.restoreRotation = v);
        y += ROW_H;
        addToggle(right, y, "restore_slot", working.restoreSlot, v -> working.restoreSlot = v);
        y += ROW_H;
        addToggle(right, y, "repeat_held", working.repeatWhileHeld, v -> working.repeatWhileHeld = v);
        y += ROW_H;
        addToggle(right, y, "show_messages", working.showMessages, v -> working.showMessages = v);
        y += ROW_H;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("punchboost.config.reset"), b -> {
                    working = new PunchBoostConfig();
                    this.clearAndInit();
                })
                .dimensions(right, y, COL_W, 20)
                .tooltip(Tooltip.of(Text.translatable("punchboost.config.reset.tooltip")))
                .build());

        // ---- Bottom: Done / Cancel ----
        int bottom = this.height - 28;
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> {
                    PunchBoostConfig.replace(working);
                    this.close();
                })
                .dimensions(this.width / 2 - 155, bottom, 150, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, b -> this.close())
                .dimensions(this.width / 2 + 5, bottom, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFFFFFFF);
        if (ServerOptOut.isDisabledByServer()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("punchboost.msg.server_disabled"),
                    this.width / 2, this.height - 44, 0xFFFF5555);
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    // ------------------------------------------------------------------

    private static String ticks(double v) {
        int t = (int) Math.round(v);
        return String.format("%d t (%.2fs)", t, t / 20.0);
    }

    private void addToggle(int x, int y, String key, boolean initial, Consumer<Boolean> setter) {
        boolean[] value = {initial};
        ButtonWidget button = ButtonWidget.builder(toggleLabel(key, initial), b -> {
                    value[0] = !value[0];
                    setter.accept(value[0]);
                    b.setMessage(toggleLabel(key, value[0]));
                })
                .dimensions(x, y, COL_W, 20)
                .tooltip(Tooltip.of(Text.translatable("punchboost.config." + key + ".tooltip")))
                .build();
        this.addDrawableChild(button);
    }

    private static Text toggleLabel(String key, boolean value) {
        return Text.translatable("punchboost.config." + key)
                .append(": ")
                .append(value ? ScreenTexts.ON : ScreenTexts.OFF);
    }

    private void addSlider(int x, int y, String key, double min, double max, double step, double current,
                           DoubleConsumer setter, DoubleFunction<String> format) {
        ValueSlider slider = new ValueSlider(x, y, COL_W, 20, key, min, max, step, current, setter, format);
        slider.setTooltip(Tooltip.of(Text.translatable("punchboost.config." + key + ".tooltip")));
        this.addDrawableChild(slider);
    }

    private static final class ValueSlider extends SliderWidget {
        private final String key;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleConsumer setter;
        private final DoubleFunction<String> format;

        ValueSlider(int x, int y, int w, int h, String key, double min, double max, double step,
                    double current, DoubleConsumer setter, DoubleFunction<String> format) {
            super(x, y, w, h, Text.empty(), (current - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.step = step;
            this.setter = setter;
            this.format = format;
            this.updateMessage();
        }

        private double current() {
            double raw = min + this.value * (max - min);
            double snapped = min + Math.round((raw - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        @Override
        protected void updateMessage() {
            // Fields aren't set yet if the superclass ever calls this during construction.
            if (key == null) {
                return;
            }
            this.setMessage(Text.translatable("punchboost.config." + key)
                    .append(": ")
                    .append(Text.literal(format.apply(current()))));
        }

        @Override
        protected void applyValue() {
            if (setter != null) {
                setter.accept(current());
            }
        }
    }
}
