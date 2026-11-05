package dev.punchboost.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.punchboost.config.PunchBoostConfigScreen;

/** Only loaded when Mod Menu is installed; adds the "Configure" button. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PunchBoostConfigScreen::new;
    }
}
