package com.tterrag.dummyplayers.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tterrag.dummyplayers.DummyPlayers;
import com.tterrag.dummyplayers.entity.DummyPlayerEntity;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class DummyPlayersCommand {
    private static final SimpleCommandExceptionType ONLY_DUMMIES = new SimpleCommandExceptionType(Component.translatable("commands.dummyplayer.only_dummies"));

    private static final DynamicCommandExceptionType NO_PROFILE = new DynamicCommandExceptionType(
            id -> Component.translatableEscape("commands.dummyplayer.no_profile", id)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dummyplayer")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.literal("id")
                                        .then(Commands.argument("id", UuidArgument.uuid())
                                                .executes(c -> resolveId(c.getSource(), UuidArgument.getUuid(c, "id"), gameProfile -> spawnDummyPlayer(c, gameProfile))))
                                )

                                .then(Commands.literal("name")
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(c -> resolveName(c.getSource(), StringArgumentType.getString(c, "name"), gameProfile -> spawnDummyPlayer(c, gameProfile))))
                                )
                                .then(
                                        Commands.literal("entity")
                                                .then(
                                                        Commands.argument("entity", EntityArgument.entity())
                                                                .executes(c -> spawnDummyPlayer(c, EntityArgument.getEntity(c, "entity")))
                                                )
                                )
                        )
                )
                .then(Commands.literal("modify")
                        .then(Commands.argument("dummy", EntityArgument.entity())
                                .then(Commands.literal("id")
                                        .then(Commands.argument("id", UuidArgument.uuid())
                                                .executes(c -> {
                                                    var entity = EntityArgument.getEntity(c, "dummy");
                                                    if (!(entity instanceof DummyPlayerEntity dummy)) {
                                                        throw ONLY_DUMMIES.create();
                                                    }
                                                    return resolveId(c.getSource(), UuidArgument.getUuid(c, "id"), gameProfile -> modifyDummyProfile(c, dummy, gameProfile));
                                                }))
                                )

                                .then(Commands.literal("name")
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(c -> {
                                                    var entity = EntityArgument.getEntity(c, "dummy");
                                                    if (!(entity instanceof DummyPlayerEntity dummy)) {
                                                        throw ONLY_DUMMIES.create();
                                                    }
                                                    return resolveName(c.getSource(), StringArgumentType.getString(c, "name"), gameProfile -> modifyDummyProfile(c, dummy, gameProfile));
                                                }))
                                )
                                .then(Commands.literal("entity")
                                        .then(Commands.argument("entity", EntityArgument.entity())
                                                .executes(c -> {
                                                    var entity = EntityArgument.getEntity(c, "dummy");
                                                    if (!(entity instanceof DummyPlayerEntity dummy)) {
                                                        throw ONLY_DUMMIES.create();
                                                    }
                                                    return modifyDummyProfile(c, dummy, EntityArgument.getEntity(c, "entity"));
                                                })
                                        )
                                )
                        )
                )
        );
    }

    private static int modifyDummyProfile(CommandContext<CommandSourceStack> ctx, DummyPlayerEntity dummy, Entity entity) throws CommandSyntaxException {
        if (entity instanceof Avatar avatar) {
            return modifyDummyProfile(ctx, dummy, avatar.getProfile());
        } else {
            throw NO_PROFILE.create(entity.getDisplayName());
        }
    }

    private static int modifyDummyProfile(CommandContext<CommandSourceStack> ctx, DummyPlayerEntity dummy, GameProfile gameProfile) {
        return modifyDummyProfile(ctx, dummy, ResolvableProfile.createResolved(gameProfile));
    }

    private static int modifyDummyProfile(CommandContext<CommandSourceStack> ctx, DummyPlayerEntity dummy, ResolvableProfile profile) {
        dummy.setAndFillProfile(profile);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.dummyplayer.modified_dummy"), true);

        return Command.SINGLE_SUCCESS;
    }

    private static int spawnDummyPlayer(CommandContext<CommandSourceStack> ctx, Entity entity) throws CommandSyntaxException {
        if (entity instanceof Avatar avatar) {
            return spawnDummyPlayer(ctx, avatar.getProfile());
        } else {
            throw NO_PROFILE.create(entity.getDisplayName());
        }
    }

    private static int spawnDummyPlayer(CommandContext<CommandSourceStack> ctx, GameProfile gameProfile) {
        return spawnDummyPlayer(ctx, ResolvableProfile.createResolved(gameProfile));
    }

    private static int spawnDummyPlayer(CommandContext<CommandSourceStack> ctx, ResolvableProfile profile) {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

        ServerLevel serverLevel = ctx.getSource().getLevel();
        DummyPlayerEntity dummy = DummyPlayers.DUMMY_PLAYER.get().create(serverLevel, dummyPlayerEntity -> dummyPlayerEntity
                .setAndFillProfile(profile), pos, EntitySpawnReason.SPAWN_ITEM_USE, true, true);
        serverLevel.addFreshEntityWithPassengers(dummy);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.dummyplayer.spawned_profile"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int resolveId(CommandSourceStack source, UUID id, Consumer<GameProfile> consumer) {
        MinecraftServer server = source.getServer();
        ProfileResolver resolver = server.services().profileResolver();
        Util.nonCriticalIoPool().execute(() -> {
            Component idComponent = Component.translationArg(id);
            Optional<GameProfile> result = resolver.fetchById(id);
            server.execute(() -> result.ifPresentOrElse(
                    consumer,
                    () -> source.sendFailure(Component.translatable("commands.dummyplayer.id.failure", idComponent))
            ));
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int resolveName(CommandSourceStack source, String name, Consumer<GameProfile> consumer) {
        MinecraftServer server = source.getServer();
        ProfileResolver resolver = server.services().profileResolver();
        Util.nonCriticalIoPool().execute(() -> {
            Component nameComponent = Component.literal(name);
            Optional<GameProfile> result = resolver.fetchByName(name);
            server.execute(() -> result.ifPresentOrElse(
                    consumer,
                    () -> source.sendFailure(Component.translatable("commands.dummyplayer.name.failure", nameComponent))
            ));
        });
        return Command.SINGLE_SUCCESS;
    }

    public static void addTranslations(RegistrateLangProvider provider) {
        provider.add("commands.dummyplayer.only_dummies", "Only Dummy Players entities can be modified");
        provider.add("commands.dummyplayer.spawned_profile", "Spawn Dummy Player with profile");
        provider.add("commands.dummyplayer.modified_dummy", "Modified Dummy Player profile");
        provider.add("commands.dummyplayer.id.failure", "Failed to resolve profile for ID %s");
        provider.add("commands.dummyplayer.id.success", "Resolved profile for ID %s: %s");
        provider.add("commands.dummyplayer.name.failure", "Failed to resolve profile for name %s");
        provider.add("commands.dummyplayer.name.success", "Resolved profile for name %s: %s");
        provider.add("commands.dummyplayer.no_profile", "Entity %s has no profile");
    }
}
