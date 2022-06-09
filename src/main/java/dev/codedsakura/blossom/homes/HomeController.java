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
}

class PlayerWithHomes {
    public UUID uuid;
    public ArrayList<Home> homes;
    public int maxHomes;
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
}
