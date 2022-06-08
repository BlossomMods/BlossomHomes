package dev.codedsakura.blossom.homes;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.codedsakura.blossom.lib.BlossomLib;
import dev.codedsakura.blossom.lib.ConfigManager;
import dev.codedsakura.blossom.lib.CustomLogger;
import dev.codedsakura.blossom.lib.Permissions;
import net.fabricmc.api.ModInitializer;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.apache.logging.log4j.core.Logger;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BlossomHomes implements ModInitializer {
    static BlossomHomesConfig CONFIG = ConfigManager.register(BlossomHomesConfig.class, "BlossomHomes.json", newConfig -> CONFIG = newConfig);
    public static final Logger LOGGER = CustomLogger.createLogger("BlossomHomes");

    @Override
    public void onInitialize() {
         BlossomLib.addCommand(literal("home")
                 .requires(Permissions.require("blossom.home", true))
                 .executes(this::runHomeDefault)
                 .then(argument("home", StringArgumentType.string())
                        .executes(this::runHomeNamed)));


         LiteralArgumentBuilder<ServerCommandSource> addHome = literal("sethome")
                .requires(Permissions.require("blossom.home.set", true))
                .executes(this::addHomeDefault)
                .then(argument("name", StringArgumentType.string())
                        .executes(this::addHomeNamed)
                        .then(argument("position", Vec3ArgumentType.vec3(true))
                                .requires(Permissions.require("blossom.home.set.pos", false))
                                .then(argument("rotation", RotationArgumentType.rotation())
                                        .executes(this::addHomePosRot)
                                        .then(argument("dimension", DimensionArgumentType.dimension())
                                                .requires(Permissions.require("blossom.home.set.dim", false))
                                                .executes(this::addHomeDimension)))));

         BlossomLib.addCommand(addHome);


         LiteralArgumentBuilder<ServerCommandSource> removeHome = literal("delhome")
                 .requires(Permissions.require("blossom.home.remove", true))
                 .executes(this::removeHomeDefault)
                 .then(argument("name", StringArgumentType.string())
                         .executes(this::removeHomeNamed));

         BlossomLib.addCommand(removeHome);


         LiteralArgumentBuilder<ServerCommandSource> listHomes = literal("listhomes")
                 .requires(Permissions.require("blossom.home.list", true))
                 .executes(this::listHomes);

         BlossomLib.addCommand(listHomes);


         BlossomLib.addCommand(literal("homes")
                 .redirect(listHomes.getRedirect())
                 .then(literal("set").redirect(addHome.getRedirect()))
                 .then(literal("add").redirect(addHome.getRedirect()))
                 .then(literal("remove").redirect(removeHome.getRedirect()))
                 .then(literal("delete").redirect(removeHome.getRedirect()))
                 .then(literal("list").redirect(listHomes.getRedirect()))
                 .then(literal("player")
                         .requires(Permissions.require("blossom.home.others", 2))
                         .then(argument("player", EntityArgumentType.player())
                                 .redirect(listHomes.getRedirect())
                                 .then(literal("set").redirect(addHome.getRedirect()))
                                 .then(literal("add").redirect(addHome.getRedirect()))
                                 .then(literal("remove").redirect(removeHome.getRedirect()))
                                 .then(literal("delete").redirect(removeHome.getRedirect()))
                                 .then(literal("list").redirect(listHomes.getRedirect())))));
    }


    private ServerPlayerEntity getPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        try {
            return EntityArgumentType.getPlayer(ctx, "player");
        } catch (IllegalArgumentException e) {
            return ctx.getSource().getPlayer();
        }
    }


    private int listHomes(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = getPlayer(ctx);
        LOGGER.debug("list {}", player);
        // todo
        return Command.SINGLE_SUCCESS;
    }


    private int runHome(CommandContext<ServerCommandSource> ctx, String homeName) throws CommandSyntaxException {
        ServerPlayerEntity player = getPlayer(ctx);
        LOGGER.debug("run {}", player);
        // todo
        return Command.SINGLE_SUCCESS;
    }

    private int runHomeDefault(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return runHome(ctx, null);
    }

    private int runHomeNamed(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "home");
        return runHome(ctx, homeName);
    }


    private int addHome(CommandContext<ServerCommandSource> ctx, Home home) throws CommandSyntaxException {
        ServerPlayerEntity player = getPlayer(ctx);
        LOGGER.debug("add {}", player);
        // todo
        return Command.SINGLE_SUCCESS;
    }

    private int addHomeCoordinates(CommandContext<ServerCommandSource> ctx, ServerWorld dimension) throws CommandSyntaxException {
        // fixme
        return addHome(ctx, null);
    }

    private int addHomeDefault(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        // fixme
        return addHomeCoordinates(ctx, null);
    }

    private int addHomePosRot(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        // fixme
        return addHomeCoordinates(ctx, null);
    }

    private int addHomeDimension(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        // fixme
        return addHomeCoordinates(ctx, null);
    }

    private int addHomeNamed(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        // fixme
        return addHome(ctx, null);
    }


    private int removeHome(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        ServerPlayerEntity player = getPlayer(ctx);
        LOGGER.debug("del {}", player);
        // todo
        return Command.SINGLE_SUCCESS;
    }

    private int removeHomeDefault(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return removeHome(ctx, null);
    }

    private int removeHomeNamed(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String homeName = StringArgumentType.getString(ctx, "home");
        return removeHome(ctx, homeName);
    }
}
