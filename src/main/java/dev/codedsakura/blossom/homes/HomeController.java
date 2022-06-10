package dev.codedsakura.blossom.homes;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.codedsakura.blossom.lib.ListDataController;
import dev.codedsakura.blossom.lib.TeleportUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class Home {
    public String name;
    public String world;
    public double x, y, z;
    public float yaw, pitch;

    Home(String name, TeleportUtils.TeleportDestination destination) {
        this(
                name, destination.world.getRegistryKey().getValue().toString(),
                destination.x, destination.y, destination.z,
                destination.yaw, destination.pitch
        );
    }

    Home(String name, String world, double x, double y, double z, float yaw, float pitch) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return "Home{" +
                "name='" + name + '\'' +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }

    TeleportUtils.TeleportDestination toDestination(MinecraftServer server) {
        return new TeleportUtils.TeleportDestination(
                server.getWorld(RegistryKey.of(Registry.WORLD_KEY, new Identifier(this.world))),
                x, y, z, yaw, pitch
        );
    }

    public Object[] toArgs() {
        return new Object[]{
                name,
                world,
                String.format("%.2f", x),
                String.format("%.2f", y),
                String.format("%.2f", z),
                String.format("%.2f", yaw),
                String.format("%.2f", pitch)
        };
    }
}

class PlayerWithHomes {
    public UUID uuid;
    public ArrayList<Home> homes;
    public int maxHomes;

    public PlayerWithHomes(UUID uuid) {
        this.uuid = uuid;
        homes = new ArrayList<>();
        maxHomes = BlossomHomes.CONFIG.startHomes;
    }

    public PlayerWithHomes(UUID uuid, int maxHomes) {
        this.uuid = uuid;
        this.maxHomes = maxHomes;
        homes = new ArrayList<>();
    }
}

public class HomeController extends ListDataController<PlayerWithHomes> implements SuggestionProvider<ServerCommandSource> {
    @Override
    public Class<PlayerWithHomes[]> getArrayClassType() {
        return PlayerWithHomes[].class;
    }

    @Override
    public List<PlayerWithHomes> defaultData() {
        return new ArrayList<>();
    }

    @Override
    public String getFilename() {
        return "BlossomHomes";
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        UUID player = context.getSource().getPlayer().getUuid();
        String start = builder.getRemaining().toLowerCase();
        data.stream()
                .filter(v -> v.uuid.equals(player))
                .flatMap(v -> v.homes.stream().map(home -> home.name))
                .sorted(String::compareToIgnoreCase)
                .filter(pair -> pair.toLowerCase().startsWith(start))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    List<Home> findPlayerHomes(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        for (PlayerWithHomes playerWithHomes : data) {
            if (playerWithHomes.uuid.equals(uuid)) {
                return playerWithHomes.homes;
            }
        }

        return List.of();
    }

    Home findHome(ServerPlayerEntity player, String name) {
        for (Home home : findPlayerHomes(player)) {
            if (home.name.equals(name)) {
                return home;
            }
        }

        return null;
    }

    int getMaxHomes(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        for (PlayerWithHomes playerWithHomes : data) {
            if (playerWithHomes.uuid.equals(uuid)) {
                return playerWithHomes.maxHomes;
            }
        }

        return BlossomHomes.CONFIG.startHomes;
    }

    void setMaxHomes(ServerPlayerEntity player, int newMaxHomes) {
        UUID uuid = player.getUuid();

        for (PlayerWithHomes playerWithHomes : data) {
            if (playerWithHomes.uuid.equals(uuid)) {
                playerWithHomes.maxHomes = newMaxHomes;
                write();
                return;
            }
        }

        data.add(new PlayerWithHomes(uuid, newMaxHomes));
        write();
    }

    enum AddHomeResult {
        SUCCESS,
        NOT_ENOUGH_HOMES,
        NAME_TAKEN,
    }

    AddHomeResult addHome(ServerPlayerEntity player, Home home) {
        UUID uuid = player.getUuid();
        List<Home> homes = findPlayerHomes(player);

        if (homes.size() + 1 > getMaxHomes(player)) {
            return AddHomeResult.NOT_ENOUGH_HOMES;
        }
        if (homes.stream().map(v -> v.name).anyMatch(v -> v.equals(home.name))) {
            return AddHomeResult.NAME_TAKEN;
        }

        if (data.stream().noneMatch(v -> v.uuid.equals(uuid))) {
            data.add(new PlayerWithHomes(uuid));
        }

        for (PlayerWithHomes playerWithHomes : data) {
            if (playerWithHomes.uuid.equals(uuid)) {
                playerWithHomes.homes.add(home);
                return AddHomeResult.SUCCESS;
            }
        }

        write();
        return AddHomeResult.SUCCESS;
    }

    boolean removeHome(ServerPlayerEntity player, String name) {
        UUID uuid = player.getUuid();
        return data
                .stream()
                .filter(v -> v.uuid.equals(uuid))
                .findAny()
                .map(v -> v.homes.removeIf(home -> home.name.equals(name)))
                .orElse(false);
    }
}
