package com.tterrag.dummyplayers.client.renderer.dummy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerCapeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public class DummyCapeLayer extends RenderLayer<DummyPlayerRenderState, DummyPlayerModel> {
	private final HumanoidModel<PlayerRenderState> model;
	private final EquipmentAssetManager equipmentAssets;

	public DummyCapeLayer(RenderLayerParent<DummyPlayerRenderState, DummyPlayerModel> parent, EntityModelSet modelSet, EquipmentAssetManager equipmentAssets) {
		super(parent);
		this.model = new PlayerCapeModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_CAPE));
		this.equipmentAssets = equipmentAssets;
	}

	private boolean hasLayer(ItemStack stack, EquipmentClientInfo.LayerType layer) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null || equippable.assetId().isEmpty()) {
			return false;
		}
		EquipmentClientInfo info = this.equipmentAssets.get(equippable.assetId().get());
		return !info.getLayers(layer).isEmpty();
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, DummyPlayerRenderState state, float yRot, float xRot) {
		ResourceLocation capeTexture = state.skin.capeTexture();
		if (state.isInvisible || capeTexture == null) {
			return;
		}
		if (this.hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) {
			return;
		}

		poseStack.pushPose();
		if (this.hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.HUMANOID)) {
			poseStack.translate(0.0f, -0.85f / 16.0f, 1.1f / 16.0f);
		}

		this.model.body.copyFrom(this.getParentModel().body);
		this.model.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entitySolid(capeTexture)), packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}
}
