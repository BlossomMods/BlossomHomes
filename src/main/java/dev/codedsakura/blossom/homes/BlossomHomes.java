package dev.codedsakura.blossom.homes;

import com.mojang.brigadier.Command;
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
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.core.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

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


        RequiredArgumentBuilder<ServerCommandSource, String> addHomeNamePosDim =
                argument("name", StringArgumentType.string())
                        .executes(this::addHomeNamed)
                        .then(argument("position", Vec3ArgumentType.vec3(true))
                                .requires(Permissions.require("blossom.home.set.pos", false))
                                .then(argument("rotation", RotationArgumentType.rotation())
                                        .executes(this::addHomePosRot)
                                        .then(argument("dimension", DimensionArgumentType.dimension())
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
                                .then(argument("players", EntityArgumentType.players())
                                        .executes(this::setMax)))));
    }


    private int listHomes(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            throw ServerCommandSource.REQUIRES_PLAYER_EXCEPTION.create();
        }

        LOGGER.trace("home list {}", player);

        List<Home> homes = homeController.findPlayerHomes(player);

        if (homes.size() == 0) {
            TextUtils.send(ctx, "blossom.homes.list.empty", homeController.getMaxHomes(player));
            return Command.SINGLE_SUCCESS;
        }

        MutableText result = homes
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
                .collect(JoiningCollector.collector(MutableText::append, new LiteralText("\n")));

        ctx.getSource().sendFeedback(
                TextUtils.translation("blossom.homes.list.header", homes.size(), homeController.getMaxHomes(player))
                        .append(result),
                false
        );
        return Command.SINGLE_SUCCESS;
    }


    private int runHome(CommandContext<ServerCommandSource> ctx, String homeName) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            throw ServerCommandSource.REQUIRES_PLAYER_EXCEPTION.create();
        }

        Home home = homeController.findHome(player, homeName);

        LOGGER.trace("home player {} to {}", player, home);

        if (home != null) {
            TeleportUtils.teleport(
                    CONFIG.teleportation,
                    CONFIG.standStill,
                    CONFIG.cooldown,
                    BlossomHomes.class,
                    player,
                    () -> home.toDestination(ctx.getSource().getServer())
            );
        }

        return Command.SINGLE_SUCCESS;
    }

    private int runHomeDefault(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return runHome(ctx, CONFIG.defaultHome);
    }

    private int runHomeNamed(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "name");
        return runHome(ctx, homeName);
    }


    private int addHome(CommandContext<ServerCommandSource> ctx, Home home) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            throw ServerCommandSource.REQUIRES_PLAYER_EXCEPTION.create();
        }

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

    private int addHomeDimension(CommandContext<ServerCommandSource> ctx, ServerWorld dimension) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "name");
        Vec3d position = Vec3ArgumentType.getPosArgument(ctx, "position").toAbsolutePos(ctx.getSource());
        Vec2f rotation = RotationArgumentType.getRotation(ctx, "rotation").toAbsoluteRotation(ctx.getSource());
        return addHome(ctx, new Home(
                homeName,
                new TeleportUtils.TeleportDestination(
                        dimension,
                        position,
                        rotation
                )
        ));
    }

    private int addHomeNamed(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            throw ServerCommandSource.REQUIRES_PLAYER_EXCEPTION.create();
        }

        return addHome(ctx, new Home(
                name,
                new TeleportUtils.TeleportDestination(player)
        ));
    }

    private int addHomeDefault(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return addHomeNamed(ctx, CONFIG.defaultHome);
    }

    private int addHomeNamed(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "name");
        return addHomeNamed(ctx, homeName);
    }

    private int addHomePosRot(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return addHomeDimension(ctx, ctx.getSource().getWorld());
    }

    private int addHomeDimension(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
        return addHomeDimension(ctx, dimension);
    }


    private int removeHome(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            throw ServerCommandSource.REQUIRES_PLAYER_EXCEPTION.create();
        }

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

    private int removeHomeDefault(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return removeHome(ctx, CONFIG.defaultHome);
    }

    private int removeHomeNamed(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "name");
        return removeHome(ctx, homeName);
    }


    private int setMax(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        int newMax = IntegerArgumentType.getInteger(ctx, "new-max");
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "players");

        players.forEach(p -> homeController.setMaxHomes(p, newMax));

        TextUtils.sendOps(
                ctx,
                "blossom.homes.setMax",
                newMax,
                players
                        .stream()
                        .map(ServerPlayerEntity::getPlayerListName)
                        .filter(Objects::nonNull)
                        .map(Text::copy)
                        .collect(JoiningCollector.<MutableText>collector(
                                MutableText::append,
                                () -> TextUtils.translation("blossom.homes.setMax.delimiter")
                        ))
        );

        return Command.SINGLE_SUCCESS;
    }
}
