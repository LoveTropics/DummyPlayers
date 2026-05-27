package com.tterrag.dummyplayers.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.tterrag.dummyplayers.DummyPlayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DummyPlayerEntity extends ArmorStand {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final EntityDataAccessor<ResolvableProfile> GAME_PROFILE = SynchedEntityData.defineId(DummyPlayerEntity.class, DummyPlayers.PROFILE_SERIALIZER.get());
    private static final EntityDataAccessor<Optional<Component>> PREFIX = SynchedEntityData.defineId(DummyPlayerEntity.class, EntityDataSerializers.OPTIONAL_COMPONENT);
    private static final EntityDataAccessor<Optional<Component>> SUFFIX = SynchedEntityData.defineId(DummyPlayerEntity.class, EntityDataSerializers.OPTIONAL_COMPONENT);

    private static final ResolvableProfile DEFAULT_PROFILE = ResolvableProfile.Static.EMPTY;

    @Nullable
    private ClientData clientData;

    public DummyPlayerEntity(EntityType<? extends DummyPlayerEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            // Show arms always on
            entityData.set(DATA_CLIENT_FLAGS, (byte) 0b100);
        } else {
            clientData = new ClientData();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GAME_PROFILE, DEFAULT_PROFILE);
        builder.define(PREFIX, Optional.empty());
        builder.define(SUFFIX, Optional.empty());
    }

    @Override
    public Component getTypeName() {
        return getProfile().name().isEmpty() ? super.getTypeName() : Component.literal(getProfile().name().get());
    }

    @Override
    public Component getDisplayName() {
        MutableComponent ret = super.getDisplayName().copy();
        Component prefix = this.entityData.get(PREFIX).orElse(null);
        Component suffix = this.entityData.get(SUFFIX).orElse(null);
        if (prefix != null) {
            ret = prefix.copy().append(ret);
        }
        if (suffix != null) {
            ret = ret.append(suffix.copy());
        }
        return ret;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(DummyPlayers.SPAWNER.get());
    }

    public ResolvableProfile getProfile() {
        return entityData.get(GAME_PROFILE);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        entityData.get(PREFIX).ifPresent(prefix -> output.store("name_prefix", ComponentSerialization.CODEC, prefix));
        entityData.get(SUFFIX).ifPresent(suffix -> output.store("name_suffix", ComponentSerialization.CODEC, suffix));
        output.store("profile", ResolvableProfile.CODEC, getProfile());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        entityData.set(PREFIX, input.read("name_prefix", ComponentSerialization.CODEC).map(this::resolveComponent));
        entityData.set(SUFFIX, input.read("name_suffix", ComponentSerialization.CODEC).map(this::resolveComponent));

        setAndFillProfile(input.read("profile", ResolvableProfile.CODEC).orElse(DEFAULT_PROFILE));
    }

    private Component resolveComponent(Component component) {
        if (level() instanceof ServerLevel serverLevel) {
            try {
                CommandSourceStack source = createCommandSourceStackForNameResolution(serverLevel).withPermission(LevelBasedPermissionSet.GAMEMASTER);
                ResolutionContext resolutionContext = ResolutionContext.builder().withSource(source).withEntityOverride(this).build();
                return ComponentUtils.resolve(resolutionContext, component);
            } catch (Exception e) {
                LOGGER.warn("Failed to resolve dummy entity text {}", component, e);
            }
        }
        return component;
    }

    public void setAndFillProfile(ResolvableProfile profile) {
        // Only update the profile (and thus the texture) if it has changed in some way
        // Avoids unnecessary texture reloads on the client when changing pose/name
        ResolvableProfile oldProfile = getProfile();
        if (profile.name().equals(oldProfile.name()) && profile.skinPatch().equals(oldProfile.skinPatch())) {
            return;
        }
        entityData.set(GAME_PROFILE, profile);
        fillProfile();
    }

    void fillProfile() {
        var server = this.level().getServer();
        if (server == null) {
            return;
        }
        var resolver = server.services().profileResolver();
        if (getProfile() instanceof ResolvableProfile.Static) {
            return;
        }
        getProfile().resolveProfile(resolver).thenAcceptAsync(
                gameProfile -> entityData.set(GAME_PROFILE, ResolvableProfile.createResolved(gameProfile))
        );
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (GAME_PROFILE.equals(key) && clientData != null) {
            clientData.invalidate();
        }
    }

    public ClientData clientData() {
        return Objects.requireNonNull(clientData, "Cannot access client data on server");
    }

    public class ClientData {
        private Supplier<PlayerSkin> skinLookup = () -> DefaultPlayerSkin.get(getUUID());
        private boolean reloadTextures = true;

        public void invalidate() {
            reloadTextures = true;
        }

        private static Supplier<PlayerSkin> createSkinLookup(ResolvableProfile profile) {
            PlayerSkin defaultSkin = DefaultPlayerSkin.get(profile.partialProfile().id());
            if (profile.partialProfile().properties().isEmpty()) {
                return () -> defaultSkin;
            }
            return createSkinLookup(profile.partialProfile(), defaultSkin);
        }

        private static Supplier<PlayerSkin> createSkinLookup(GameProfile profile, PlayerSkin defaultSkin) {
            CompletableFuture<Optional<PlayerSkin>> skinFuture = Minecraft.getInstance().getSkinManager().get(profile);
            if (skinFuture.isDone()) {
                PlayerSkin skin = skinFuture.getNow(Optional.empty()).orElse(defaultSkin);
                return () -> skin;
            }
            return () -> skinFuture.getNow(Optional.empty()).orElse(defaultSkin);
        }

        public PlayerSkin skin() {
            if (reloadTextures) {
                reloadTextures = false;
                skinLookup = createSkinLookup(getProfile());
            }
            return skinLookup.get();
        }
    }
}
