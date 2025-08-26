package com.tterrag.dummyplayers.client.renderer.dummy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tterrag.dummyplayers.entity.DummyPlayerEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.checkerframework.checker.units.qual.A;

public class DummyArmorLayer<S extends DummyPlayerRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends RenderLayer<S, M> {
	private final HumanoidArmorLayer<S, M, A> normal, slim;

	public DummyArmorLayer(RenderLayerParent<S, M> parent, HumanoidArmorLayer<S, M, A> normal, HumanoidArmorLayer<S, M, A> slim) {
		super(parent);
		this.normal = normal;
		this.slim = slim;
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, S state, float yRot, float xRot) {
		HumanoidArmorLayer<S, M, A> layer = switch (state.skin.model()) {
			case WIDE -> normal;
			case SLIM -> slim;
		};
		layer.render(poseStack, bufferSource, packedLight, state, yRot, xRot);
	}
}
