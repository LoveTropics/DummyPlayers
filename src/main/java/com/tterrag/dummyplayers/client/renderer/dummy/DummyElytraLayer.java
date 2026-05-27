package com.tterrag.dummyplayers.client.renderer.dummy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import javax.annotation.Nullable;

public class DummyElytraLayer extends RenderLayer<DummyPlayerRenderState, DummyPlayerModel> {
    private final ElytraModel model;
    private final EquipmentLayerRenderer equipmentRenderer;

    public DummyElytraLayer(RenderLayerParent<DummyPlayerRenderState, DummyPlayerModel> parent, EntityModelSet models, EquipmentLayerRenderer equipmentRenderer) {
        super(parent);
        this.model = new ElytraModel(models.bakeLayer(ModelLayers.ELYTRA));
        this.equipmentRenderer = equipmentRenderer;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, DummyPlayerRenderState state, float yRot, float xRot) {
        ItemStack chestEquipment = state.chestEquipment;
        Equippable equippable = chestEquipment.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) {
            return;
        }
        Identifier texture = getTextureOverride(state);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        this.model.setupAnim(state);
        this.equipmentRenderer.renderLayers(
                EquipmentClientInfo.LayerType.WINGS,
                equippable.assetId().get(),
                this.model,
                state,
                chestEquipment,
                poseStack,
                submitNodeCollector,
                lightCoords,
                texture,
                state.outlineColor,
                0
        );
        poseStack.popPose();
    }

    @Nullable
    private static Identifier getTextureOverride(DummyPlayerRenderState state) {
        PlayerSkin skin = state.skin;
        if (skin.elytra() != null) {
            return skin.elytra().texturePath();
        } else if (skin.cape() != null) {
            return skin.cape().texturePath();
        }
        return null;
    }
}
