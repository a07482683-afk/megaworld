package com.megaworld.client;

import com.megaworld.MegaWorld;
import com.megaworld.entity.GroxDwellerEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * Собственная модель жителя Грокса: большая круглая голова, чубчик, острые уши,
 * короткие ручки и ножки и рюкзак со светящимся кристаллом.
 * Текстура 64x64: assets/megaworld/textures/entity/grox_dweller.png
 */
public class GroxDwellerModel extends EntityModel<GroxDwellerEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(MegaWorld.id("grox_dweller"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart crystal;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public GroxDwellerModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.crystal = root.getChild("body").getChild("backpack").getChild("crystal");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-5F, -10F, -5F, 10F, 10F, 10F),
                ModelTransform.pivot(0F, 8F, 0F));
        head.addChild("hair",
                ModelPartBuilder.create().uv(40, 0).cuboid(-2F, -3F, -2F, 4F, 3F, 4F),
                ModelTransform.pivot(0F, -10F, 0F));
        head.addChild("left_ear",
                ModelPartBuilder.create().uv(40, 8).cuboid(0F, -2F, -1F, 1F, 4F, 2F),
                ModelTransform.pivot(5F, -5F, 0F));
        head.addChild("right_ear",
                ModelPartBuilder.create().uv(48, 8).cuboid(-1F, -2F, -1F, 1F, 4F, 2F),
                ModelTransform.pivot(-5F, -5F, 0F));

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 20).cuboid(-4F, 0F, -2.5F, 8F, 9F, 5F),
                ModelTransform.pivot(0F, 8F, 0F));
        ModelPartData backpack = body.addChild("backpack",
                ModelPartBuilder.create().uv(28, 20).cuboid(-3F, 0F, 0F, 6F, 7F, 3F),
                ModelTransform.pivot(0F, 1F, 2.5F));
        backpack.addChild("crystal",
                ModelPartBuilder.create().uv(48, 20).cuboid(-1F, -4F, 0.5F, 2F, 4F, 2F),
                ModelTransform.pivot(0F, 0F, 0F));

        root.addChild("right_arm",
                ModelPartBuilder.create().uv(0, 36).cuboid(-1.5F, -1F, -1.5F, 3F, 8F, 3F),
                ModelTransform.pivot(-5.5F, 9F, 0F));
        root.addChild("left_arm",
                ModelPartBuilder.create().uv(12, 36).cuboid(-1.5F, -1F, -1.5F, 3F, 8F, 3F),
                ModelTransform.pivot(5.5F, 9F, 0F));
        root.addChild("right_leg",
                ModelPartBuilder.create().uv(24, 36).cuboid(-1.5F, 0F, -1.5F, 3F, 7F, 3F),
                ModelTransform.pivot(-2F, 17F, 0F));
        root.addChild("left_leg",
                ModelPartBuilder.create().uv(36, 36).cuboid(-1.5F, 0F, -1.5F, 3F, 7F, 3F),
                ModelTransform.pivot(2F, 17F, 0F));

        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public void setAngles(GroxDwellerEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        head.yaw = headYaw * ((float) Math.PI / 180F);
        head.pitch = headPitch * ((float) Math.PI / 180F);

        rightArm.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 1.4F * limbDistance * 0.5F;
        leftArm.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance * 0.5F;
        rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
        leftLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 1.4F * limbDistance;

        // кристалл на рюкзаке медленно вращается
        crystal.yaw = animationProgress * 0.1F;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
