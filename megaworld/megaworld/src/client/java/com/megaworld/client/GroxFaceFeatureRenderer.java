package com.megaworld.client;

import com.megaworld.MegaWorld;
import com.megaworld.entity.Emotion;
import com.megaworld.entity.GroxDwellerEntity;
import com.megaworld.entity.Variant;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Рисует «мемное» лицо поверх головы обычного жителя (нос остаётся спереди, как в роликах).
 * Тело, одежда и модель остаются ванильными. У обычного жителя в спокойном состоянии лицо не трогаем.
 */
public class GroxFaceFeatureRenderer extends FeatureRenderer<GroxDwellerEntity, VillagerResemblingModel<GroxDwellerEntity>> {
    private static final Identifier[][] TEXTURES = new Identifier[Variant.values().length][Emotion.values().length];

    static {
        for (Variant v : Variant.values()) {
            for (Emotion e : Emotion.values()) {
                TEXTURES[v.ordinal()][e.ordinal()] = MegaWorld.id("textures/entity/face/" + v.id + "_" + e.id + ".png");
            }
        }
    }

    public GroxFaceFeatureRenderer(FeatureRendererContext<GroxDwellerEntity, VillagerResemblingModel<GroxDwellerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, GroxDwellerEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        Variant variant = entity.getVariant();
        Emotion emotion = entity.getEmotion();
        if (variant == Variant.NORMAL && emotion == Emotion.NEUTRAL) {
            return; // обычное лицо жителя
        }
        Identifier texture = TEXTURES[variant.ordinal()][emotion.ordinal()];

        matrices.push();
        this.getContextModel().getHead().rotate(matrices);
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f position = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();

        // передняя грань головы: x от -4 до 4, y от -10 до 0 (в пикселях модели), чуть впереди кожи
        float z = -4.05F / 16.0F;
        float x0 = -4.0F / 16.0F;
        float x1 = 4.0F / 16.0F;
        float y0 = -10.0F / 16.0F;
        float y1 = 0.0F;
        vertex(consumer, position, normal, x0, y0, z, 0.0F, 0.0F, light);
        vertex(consumer, position, normal, x0, y1, z, 0.0F, 1.0F, light);
        vertex(consumer, position, normal, x1, y1, z, 1.0F, 1.0F, light);
        vertex(consumer, position, normal, x1, y0, z, 1.0F, 0.0F, light);
        matrices.pop();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f position, Matrix3f normal,
                               float x, float y, float z, float u, float v, int light) {
        consumer.vertex(position, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal, 0.0F, 0.0F, -1.0F)
                .next();
    }
}
