package com.tterrag.dummyplayers.client.renderer.dummy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tterrag.dummyplayers.entity.DummyPlayerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class DummyPlayerEntityRenderer extends LivingEntityRenderer<DummyPlayerEntity, DummyPlayerRenderState, DummyPlayerModel> {

    private final DummyPlayerModel slim;
    private final DummyPlayerModel normal;

    public DummyPlayerEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new DummyPlayerModel(context.bakeLayer(ModelLayers.PLAYER)), 0);
        this.normal = this.model;
        this.slim = new DummyPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM));
        this.addLayer(new DummyStandLayer(this, context.getModelSet()));
        //TODO: support slim characters
        this.addLayer(new DummyArmorLayer<>(this,
                        new HumanoidArmorLayer<>(
                                this,
                                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), DummyPlayerModel::new),
                                context.getEquipmentRenderer()
                        ),
                        new HumanoidArmorLayer<>(this,
                                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), DummyPlayerModel::new),
                                context.getEquipmentRenderer())
                )
        );
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new DummyCapeLayer(this, context.getModelSet(), context.getEquipmentAssets()));
        this.addLayer(new DummyElytraLayer(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
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
    public void submit(DummyPlayerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.model = switch (state.skin.model()) {
            case WIDE -> normal;
            case SLIM -> slim;
        };
        super.submit(state, poseStack, submitNodeCollector, camera);
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
    public Identifier getTextureLocation(DummyPlayerRenderState state) {
        return state.skin.body().texturePath();
    }
}
