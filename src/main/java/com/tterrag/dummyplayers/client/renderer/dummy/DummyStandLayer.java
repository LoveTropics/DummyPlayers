package com.tterrag.dummyplayers.client.renderer.dummy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tterrag.dummyplayers.DummyPlayers;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;

public class DummyStandLayer extends RenderLayer<DummyPlayerRenderState, DummyPlayerModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DummyPlayers.DUMMY_PLAYER.getId(), "stand");

    private final Model standModel;
    private final ModelPart standBase;

    public DummyStandLayer(RenderLayerParent<DummyPlayerRenderState, DummyPlayerModel> parent, EntityModelSet context) {
        super(parent);
        this.standBase = context.bakeLayer(LAYER);
        this.standModel = new Model.Simple(standBase, RenderTypes::entitySolid);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base_plate",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-6.0F, 11.0F, -6.0F, 12.0F, 1.0F, 12.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, DummyPlayerRenderState state, float yRot, float xRot) {
        if (!state.showBasePlate) {
            return;
        }
        this.standBase.yRot = Mth.DEG_TO_RAD * -yRot;
        poseStack.pushPose();
        poseStack.translate(0, 1f / 16f, 0);
        submitNodeCollector
                .submitModel(
                        this.standModel,
                        state,
                        poseStack,
                        this.standModel.renderType(ArmorStandRenderer.DEFAULT_SKIN_LOCATION),
                        lightCoords,
                        LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                        CommonColors.BLACK,
                        null,
                        state.outlineColor,
                        null
                );
        poseStack.popPose();
    }
}
