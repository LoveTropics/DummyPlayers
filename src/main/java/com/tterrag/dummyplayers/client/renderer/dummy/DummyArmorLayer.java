package com.tterrag.dummyplayers.client.renderer.dummy;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class DummyArmorLayer<S extends DummyPlayerRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final HumanoidArmorLayer<S, M, A> normal, slim;

    public DummyArmorLayer(RenderLayerParent<S, M> parent, HumanoidArmorLayer<S, M, A> normal, HumanoidArmorLayer<S, M, A> slim) {
        super(parent);
        this.normal = normal;
        this.slim = slim;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        HumanoidArmorLayer<S, M, A> layer = switch (state.skin.model()) {
            case WIDE -> normal;
            case SLIM -> slim;
        };
        layer.submit(poseStack, submitNodeCollector, lightCoords, state, yRot, xRot);
    }
}
