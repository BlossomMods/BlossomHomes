package dev.codedsakura.blossom.homes;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.codedsakura.blossom.lib.BlossomLib;
import dev.codedsakura.blossom.lib.config.ConfigManager;
import dev.codedsakura.blossom.lib.permissions.Permissions;
import dev.codedsakura.blossom.lib.teleport.TeleportUtils;
import dev.codedsakura.blossom.lib.text.CommandTextBuilder;
import dev.codedsakura.blossom.lib.text.JoiningCollector;
import dev.codedsakura.blossom.lib.text.TextUtils;
import dev.codedsakura.blossom.lib.utils.CustomLogger;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.core.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class BlossomHomes implements ModInitializer {
    static BlossomHomesConfig CONFIG = ConfigManager.register(BlossomHomesConfig.class, "BlossomHomes.json", newConfig -> CONFIG = newConfig);
    public static final Logger LOGGER = CustomLogger.createLogger("BlossomHomes");
    static HomeController homeController;

    @Override
    public void onInitialize() {
        homeController = new HomeController();

        BlossomLib.addCommand(literal("home")
                .requires(Permissions.require("blossom.home", true))
                .executes(this::runHomeDefault)
                .then(argument("name", StringArgumentType.string())
                        .suggests(homeController)
                        .executes(this::runHomeNamed)));


        RequiredArgumentBuilder<CommandSourceStack, String> addHomeNamePosDim =
                argument("name", StringArgumentType.string())
                        .executes(this::addHomeNamed)
                        .then(argument("position", Vec3Argument.vec3(true))
                                .requires(Permissions.require("blossom.home.set.pos", false))
                                .then(argument("rotation", RotationArgument.rotation())
                                        .executes(this::addHomePosRot)
                                        .then(argument("dimension", DimensionArgument.dimension())
                                                .requires(Permissions.require("blossom.home.set.dim", false))
                                                .executes(this::addHomeDimension))));

        BlossomLib.addCommand(literal("sethome")
                .requires(Permissions.require("blossom.home.set", true))
                .executes(this::addHomeDefault)
                .then(addHomeNamePosDim));


        BlossomLib.addCommand(literal("delhome")
                .requires(Permissions.require("blossom.home.remove", true))
                .executes(this::removeHomeDefault)
                .then(argument("name", StringArgumentType.string())
                        .suggests(homeController)
                        .executes(this::removeHomeNamed)));


        BlossomLib.addCommand(literal("listhomes")
                .requires(Permissions.require("blossom.home.list", true))
                .executes(this::listHomes));


        BlossomLib.addCommand(literal("homes")
                .requires(Permissions.require("blossom.homes.list", true))
                .executes(this::listHomes)
                .then(literal("list")
                        .requires(Permissions.require("blossom.homes.list", true))
                        .executes(this::listHomes))

                .then(literal("set")
                        .requires(Permissions.require("blossom.homes.set", true))
                        .executes(this::addHomeDefault)
                        .then(addHomeNamePosDim))
                .then(literal("add")
                        .requires(Permissions.require("blossom.homes.add", false))
                        .executes(this::addHomeDefault)
                        .then(addHomeNamePosDim))

                .then(literal("remove")
                        .requires(Permissions.require("blossom.homes.remove", false))
                        .executes(this::removeHomeDefault)
                        .then(argument("name", StringArgumentType.string())
                                .suggests(homeController)
                                .executes(this::removeHomeNamed)))
                .then(literal("delete")
                        .requires(Permissions.require("blossom.homes.delete", true))
                        .executes(this::removeHomeDefault)
                        .then(argument("name", StringArgumentType.string())
                                .suggests(homeController)
                                .executes(this::removeHomeNamed)))

                .then(literal("set-max")
                        .requires(Permissions.require("blossom.homes.set-max", 2))
                        .then(argument("new-max", IntegerArgumentType.integer(0))
                                .then(argument("players", EntityArgument.players())
                                        .executes(this::setMax))))

                .then(literal("load-legacy")
                        .requires(Permissions.require("blossom.homes.load-legacy", 4))
                        .executes(this::loadLegacyDefault)
                        .then(argument("overwrite", BoolArgumentType.bool())
                                .executes(this::loadLegacyArgument))));
    }


    private int listHomes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        LOGGER.trace("home list {}", player);

        List<Home> homes = homeController.findPlayerHomes(player);

        if (homes.isEmpty()) {
            TextUtils.send(ctx, "blossom.homes.list.empty", homeController.getMaxHomes(player));
            return Command.SINGLE_SUCCESS;
        }

        MutableComponent result = homes
                .stream()
                .map(home -> TextUtils.translation("blossom.homes.list.item.before")
                        .append(TextUtils.translation(
                                "blossom.homes.list.item",
                                new CommandTextBuilder(home.name)
                                        .setClickSuggest()
                                        .setCommandRun("/home " + home.name)
                                        .setHoverShowRun()
                                        .setDescription(TextUtils.translation("blossom.homes.list.item.description", home.toArgs()))))
                        .append(TextUtils.translation("blossom.homes.list.item.after")))
                .collect(JoiningCollector.collector(MutableComponent::append, Component.literal("\n")));

        ctx.getSource().sendSuccess(() ->
                TextUtils.translation("blossom.homes.list.header", homes.size(), homeController.getMaxHomes(player))
                        .append(result),
                false
        );
        return Command.SINGLE_SUCCESS;
    }


    private int runHome(CommandContext<CommandSourceStack> ctx, String homeName) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        Home home = homeController.findHome(player, homeName);

        LOGGER.trace("home player {} to {}", player, home);

        if (home == null) {
            if (!(homeName.equals(CONFIG.defaultHome) && CONFIG.fallbackToPlayerSpawnPoint)) {
                TextUtils.sendErr(ctx, "blossom.homes.not-found", homeName);
                return Command.SINGLE_SUCCESS;
            }

            TeleportTransition teleportTarget = player.findRespawnPositionAndUseSpawnBlock(true, TeleportTransition.DO_NOTHING);

            LOGGER.trace("found spawn position for {} @ {}", player.getStringUUID(), teleportTarget);

            TextUtils.sendWarn(ctx, "blossom.homes.spawn");
            home = new Home(
                    CONFIG.defaultHome,
                    teleportTarget.newLevel().dimension().identifier().toString(),
                    teleportTarget.position().x,
                    teleportTarget.position().y,
                    teleportTarget.position().z,
                    teleportTarget.xRot(),
                    teleportTarget.yRot()
            );
        }

        Home finalHome = home;
        TeleportUtils.teleport(
                CONFIG.teleportation,
                CONFIG.standStill,
                CONFIG.cooldown,
                BlossomHomes.class,
                player,
                () -> finalHome.toDestination(ctx.getSource().getServer())
        );


        return Command.SINGLE_SUCCESS;
    }

    private int runHomeDefault(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return runHome(ctx, CONFIG.defaultHome);
    }

    private int runHomeNamed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "name");
        return runHome(ctx, homeName);
    }


    private int addHome(CommandContext<CommandSourceStack> ctx, Home home) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        LOGGER.info("adding home {} to {}", home, player);

        boolean invalidDimension = CONFIG.dimensionBlacklist.contains(home.world);
        if (CONFIG.useBlacklistAsWhitelist) {
            invalidDimension = !invalidDimension;
        }

        if (invalidDimension) {
            if (!Permissions.check(ctx.getSource(), "blossom.homes.set.in-blacklist", 2)) {
                TextUtils.sendErr(ctx, "blossom.homes.add.failed.dimension", home.world);
                return Command.SINGLE_SUCCESS;
            }
        }

        HomeController.AddHomeResult result = homeController.addHome(player, home);
        switch (result) {
            case SUCCESS -> TextUtils.sendSuccess(ctx, "blossom.homes.add", home.name);
            case NOT_ENOUGH_HOMES ->
                    TextUtils.sendErr(ctx, "blossom.homes.add.failed.max", homeController.getMaxHomes(player));
            case NAME_TAKEN -> TextUtils.sendErr(ctx, "blossom.homes.add.failed.name", home.name);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int addHomeDimension(CommandContext<CommandSourceStack> ctx, ServerLevel dimension) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "name");
        Vec3 position = Vec3Argument.getCoordinates(ctx, "position").getPosition(ctx.getSource());
        Vec2 rotation = RotationArgument.getRotation(ctx, "rotation").getRotation(ctx.getSource());
        return addHome(ctx, new Home(
                homeName,
                new TeleportUtils.TeleportDestination(
                        dimension,
                        position,
                        rotation
                )
        ));
    }

    private int addHomeNamed(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        return addHome(ctx, new Home(
                name,
                new TeleportUtils.TeleportDestination(player)
        ));
    }

    private int addHomeDefault(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return addHomeNamed(ctx, CONFIG.defaultHome);
    }

    private int addHomeNamed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "name");
        return addHomeNamed(ctx, homeName);
    }

    private int addHomePosRot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return addHomeDimension(ctx, ctx.getSource().getLevel());
    }

    private int addHomeDimension(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
        return addHomeDimension(ctx, dimension);
    }


    private int removeHome(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        Home home = homeController.findHome(player, name);
        LOGGER.debug("removing home {} from {}", home, player);

        boolean result = homeController.removeHome(player, name);
        if (result) {
            TextUtils.sendWarn(ctx, "blossom.homes.remove", name);
        } else {
            TextUtils.sendErr(ctx, "blossom.homes.remove.failed", name);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int removeHomeDefault(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return removeHome(ctx, CONFIG.defaultHome);
    }

    private int removeHomeNamed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "name");
        return removeHome(ctx, homeName);
    }


    private int setMax(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int newMax = IntegerArgumentType.getInteger(ctx, "new-max");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");

        players.forEach(p -> homeController.setMaxHomes(p, newMax));

        TextUtils.sendOps(
                ctx,
                "blossom.homes.setMax",
                newMax,
                players
                        .stream()
                        .map(player -> {
                            if (player.getTabListDisplayName() != null)
                                return player.getTabListDisplayName();
                            return player.getDisplayName();
                        })
                        .filter(Objects::nonNull)
                        .map(Component::copy)
                        .collect(JoiningCollector.<MutableComponent>collector(
                                MutableComponent::append,
                                TextUtils.translation("blossom.homes.setMax.delimiter")
                        ))
        );

        return Command.SINGLE_SUCCESS;
    }


    private int loadLegacy(CommandContext<CommandSourceStack> ctx, boolean overwrite) {
        TextUtils.sendOps(ctx, "blossom.homes.load-legacy.info");

        if (overwrite) {
            TextUtils.sendOps(ctx, "blossom.homes.load-legacy.overwrite");
        }

        MinecraftServer server = ctx.getSource().getServer();

        File[] playerDataFiles = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile().listFiles();

        int totalHomes = 0, totalPlayers = 0;

        try {
            assert playerDataFiles != null;
            for (File playerDataFile : playerDataFiles) {
                InputStream pdfIs = new FileInputStream(playerDataFile);
                CompoundTag data = NbtIo.readCompressed(pdfIs, NbtAccounter.unlimitedHeap());

                if (!data.contains("cardinal_components")) {
                    continue;
                }
                data = data.getCompound("cardinal_components").get();

                if (!data.contains("fabrichomes:homes")) {
                    continue;
                }
                var homes = data.getCompound("fabrichomes:homes").get()
                        .getList("homes").get()
                        .stream()
                        .map(home -> {
                            String name = ((CompoundTag) home).getString("name").get();
                            String world = ((CompoundTag) home).getString("dim").get();
                            double x = ((CompoundTag) home).getFloat("x").get();
                            double y = ((CompoundTag) home).getFloat("y").get();
                            double z = ((CompoundTag) home).getFloat("z").get();
                            float yaw = ((CompoundTag) home).getFloat("yaw").get();
                            float pitch = ((CompoundTag) home).getFloat("pitch").get();
                            return new Home(name, world, x, y, z, yaw, pitch);
                        })
                        .toList();

                UUID uuid = UUID.fromString(FilenameUtils.removeExtension(playerDataFile.getName()));

                totalPlayers++;
                totalHomes += homes.size();

                homeController.appendHomes(uuid, homes, overwrite);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        TextUtils.sendOps(ctx, "blossom.homes.load-legacy.done", totalHomes, totalPlayers);
        return Command.SINGLE_SUCCESS;
    }

    private int loadLegacyArgument(CommandContext<CommandSourceStack> ctx) {
        boolean overwrite = BoolArgumentType.getBool(ctx, "overwrite");

        return loadLegacy(ctx, overwrite);
    }

    private int loadLegacyDefault(CommandContext<CommandSourceStack> ctx) {
        return loadLegacy(ctx, false);
    }
}
