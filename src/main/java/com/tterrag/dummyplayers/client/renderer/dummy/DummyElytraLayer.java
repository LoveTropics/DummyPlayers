package com.tterrag.dummyplayers.client.renderer.dummy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
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
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, DummyPlayerRenderState state, float yRot, float xRot) {
		ItemStack chestEquipment = state.chestEquipment;
		Equippable equippable = chestEquipment.get(DataComponents.EQUIPPABLE);
		if (equippable == null || equippable.assetId().isEmpty()) {
			return;
		}
		ResourceLocation texture = getTextureOverride(state);
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.0F, 0.125F);
		this.model.setupAnim(state);
		this.equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.WINGS, equippable.assetId().get(), this.model, chestEquipment, poseStack, bufferSource, packedLight, texture);
		poseStack.popPose();
	}

	@Nullable
	private static ResourceLocation getTextureOverride(DummyPlayerRenderState state) {
		PlayerSkin skin = state.skin;
		if (skin.elytraTexture() != null) {
			return skin.elytraTexture();
		} else if (skin.capeTexture() != null) {
			return skin.capeTexture();
		}
		return null;
	}
}
