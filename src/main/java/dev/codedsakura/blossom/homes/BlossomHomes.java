package dev.codedsakura.blossom.homes;

import dev.codedsakura.blossom.lib.BlossomConfig;
import dev.codedsakura.blossom.lib.CustomLogger;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.core.Logger;

public class BlossomHomes implements ModInitializer {
    static BlossomHomesConfig CONFIG = BlossomConfig.load(BlossomHomesConfig.class, "BlossomHomes.json");
    public static final Logger LOGGER = CustomLogger.createLogger("BlossomHomes");

    @Override
    public void onInitialize() {
        // BlossomLib.addCommand();
    }
}
