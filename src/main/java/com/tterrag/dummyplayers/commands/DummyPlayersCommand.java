package com.tterrag.dummyplayers.commands;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DummyPlayersCommand {

    private static final SimpleCommandExceptionType ONLY_DUMMIES = new SimpleCommandExceptionType(Component.translatable("commands.dummyplayer.only_dummies"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dummyplayer")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("profile", GameProfileArgument.gameProfile())
                                        .executes(DummyPlayersCommand::spawnDummyPlayer)
                                )
                        )
                )
                .then(Commands.literal("modify")
                        .then(Commands.argument("dummy", EntityArgument.entity())
                                .then(Commands.argument("profile", GameProfileArgument.gameProfile())
                                        .executes(DummyPlayersCommand::modifyDummyProfile)
                                )
                        )
                )
        );
    }

    private static int modifyDummyProfile(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(ctx, "dummy");
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "profile");

        if (!(entity instanceof DummyPlayerEntity dummy)) {
            throw ONLY_DUMMIES.create();
        }

        resolveProfile(profiles).thenAccept(resolvableProfile -> {
            dummy.setAndFillProfile(resolvableProfile);
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.dummyplayer.modified_dummy"), true);
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int spawnDummyPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "profile");

        resolveProfile(profiles).thenAccept(resolvableProfile -> {
            ServerLevel serverLevel = ctx.getSource().getLevel();
            DummyPlayerEntity dummy = DummyPlayers.DUMMY_PLAYER.get().create(serverLevel, dummyPlayerEntity -> dummyPlayerEntity
                    .setAndFillProfile(resolvableProfile), pos, EntitySpawnReason.SPAWN_ITEM_USE, true, true);
            serverLevel.addFreshEntityWithPassengers(dummy);
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.dummyplayer.spawned_profile"), true);
        });
        
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<ResolvableProfile> resolveProfile(Collection<GameProfile> profiles) throws CommandSyntaxException {
        if (profiles.size() != 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_PLAYER.create();
        }

        GameProfile profile = Iterables.getOnlyElement(profiles);
        return SkullBlockEntity.fetchGameProfile(profile.getId())
                .thenApply(optional -> new ResolvableProfile(optional.orElse(profile)));
    }

    public static void addTranslations(RegistrateLangProvider provider) {
        provider.add("commands.dummyplayer.only_dummies", "Only Dummy Players entities can be modified");
        provider.add("commands.dummyplayer.spawned_profile", "Spawn Dummy Player with profile");
        provider.add("commands.dummyplayer.modified_dummy", "Modified Dummy Player profile");
    }
}
