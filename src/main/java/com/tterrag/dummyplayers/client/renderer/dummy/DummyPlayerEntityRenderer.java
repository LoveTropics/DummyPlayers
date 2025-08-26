package com.tterrag.dummyplayers.client.renderer.dummy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tterrag.dummyplayers.entity.DummyPlayerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class DummyPlayerEntityRenderer extends LivingEntityRenderer<DummyPlayerEntity, DummyPlayerRenderState, DummyPlayerModel> {

	private final DummyPlayerModel slim;
	private final DummyPlayerModel normal;

	public DummyPlayerEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new DummyPlayerModel(context.bakeLayer(ModelLayers.PLAYER)), 0);
		this.normal = this.model;
		this.slim = new DummyPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM));
		this.addLayer(new DummyStandLayer(this, context.getModelSet()));
		this.addLayer(new DummyArmorLayer<>(this,
				new HumanoidArmorLayer<>(this, new DummyPlayerModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new DummyPlayerModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getEquipmentRenderer()),
				new HumanoidArmorLayer<>(this, new DummyPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)), new DummyPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)), context.getEquipmentRenderer())));
		this.addLayer(new ItemInHandLayer<>(this));
		this.addLayer(new DummyCapeLayer(this, context.getModelSet(), context.getEquipmentAssets()));
		this.addLayer(new DummyElytraLayer(this, context.getModelSet(), context.getEquipmentRenderer()));
		this.addLayer(new CustomHeadLayer<>(this, context.getModelSet()));
	}

	@Override
	public DummyPlayerRenderState createRenderState() {
		return new DummyPlayerRenderState();
	}

	@Override
	public void extractRenderState(DummyPlayerEntity entity, DummyPlayerRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
		state.skin = entity.clientData().skin();
		state.headPose = entity.getHeadPose();
		state.bodyPose = entity.getBodyPose();
		state.leftArmPose = entity.getLeftArmPose();
		state.rightArmPose = entity.getRightArmPose();
		state.leftLegPose = entity.getLeftLegPose();
		state.rightLegPose = entity.getRightLegPose();
		state.showBasePlate = entity.showBasePlate();
	}

	@Override
	public void render(DummyPlayerRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		this.model = switch (state.skin.model()) {
			case WIDE -> normal;
			case SLIM -> slim;
		};
		super.render(state, poseStack, bufferSource, packedLight);
	}

	@Override
	protected boolean shouldShowName(DummyPlayerEntity entity, double distanceSq) {
		return entity.getProfile().name().isPresent() || entity.hasCustomName();
	}

	@Override
	protected void scale(DummyPlayerRenderState state, PoseStack poseStack) {
		if (state.showBasePlate) {
			poseStack.translate(0, -1f / 16f, 0);
		}
		float scale = 0.9375F;
		poseStack.scale(scale, scale, scale);
	}

	@Override
	public ResourceLocation getTextureLocation(DummyPlayerRenderState state) {
		return state.skin.texture();
	}
}
