package com.tterrag.dummyplayers.client.renderer.dummy;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Rotations;
import net.minecraft.util.Mth;

public class DummyPlayerModel extends HumanoidModel<DummyPlayerRenderState> {
	public DummyPlayerModel(ModelPart root) {
		super(root);
	}

	private static void applyPose(ModelPart model, Rotations rotations) {
		model.xRot = rotations.x() * Mth.DEG_TO_RAD;
		model.yRot = rotations.y() * Mth.DEG_TO_RAD;
		model.zRot = rotations.z() * Mth.DEG_TO_RAD;
	}

	@Override
	public void setupAnim(DummyPlayerRenderState state) {
		super.setupAnim(state);
		applyPose(head, state.headPose);
		applyPose(body, state.bodyPose);
		applyPose(leftArm, state.leftArmPose);
		applyPose(rightArm, state.rightArmPose);
		applyPose(leftLeg, state.leftLegPose);
		applyPose(rightLeg, state.rightLegPose);
	}
}
