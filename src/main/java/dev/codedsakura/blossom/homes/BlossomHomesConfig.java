package dev.codedsakura.blossom.homes;


import dev.codedsakura.blossom.lib.teleport.TeleportConfig;

import java.util.List;

public class BlossomHomesConfig {
    TeleportConfig teleportation = null;

    int standStill = 3;
    int cooldown = 30;

    String defaultHome = "main";
    int startHomes = 2;

    List<String> dimensionBlacklist = List.of();
    boolean useBlacklistAsWhitelist = false;

    boolean fallbackToPlayerSpawnPoint = true;
}
