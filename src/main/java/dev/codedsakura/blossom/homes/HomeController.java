package dev.codedsakura.blossom.homes;

import dev.codedsakura.blossom.lib.ListDataController;
import dev.codedsakura.blossom.lib.TeleportUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public Home[] homes;
    public int maxHomes;
}

public class HomeController extends ListDataController<PlayerWithHomes> {
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
}
