package com.megaworld;

import com.megaworld.client.GroxFaceFeatureRenderer;
import com.megaworld.entity.Gesture;
import com.megaworld.entity.GroxDwellerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Обычная модель и текстура жителя Minecraft. Поверх головы рисуется лицо по эмоции и типу жителя,
 * а жесты (танец, маханье) показаны покачиванием всего тела.
 */
public class GroxDwellerRenderer extends MobEntityRenderer<GroxDwellerEntity, VillagerResemblingModel<GroxDwellerEntity>> {
    private static final Identifier TEXTURE = new Identifier("textures/entity/villager/villager.png");

    public GroxDwellerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new VillagerResemblingModel<>(ctx.getPart(EntityModelLayers.VILLAGER)), 0.5F);
        this.addFeature(new GroxFaceFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(GroxDwellerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void setupTransforms(GroxDwellerEntity entity, MatrixStack matrices, float animationProgress,
                                   float bodyYaw, float tickDelta) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
        Gesture gesture = entity.getGesture();
        if (gesture == Gesture.DANCE) {
            matrices.translate(0.0F, Math.abs(MathHelper.sin(animationProgress * 0.4F)) * 0.12F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(animationProgress * 0.4F) * 10.0F));
        } else if (gesture == Gesture.WAVE) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(animationProgress * 0.7F) * 8.0F));
        }
    }
}
