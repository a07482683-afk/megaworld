package com.megaworld;

import com.megaworld.entity.GroxDwellerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.util.Identifier;

/** Пока используется модель и текстура ванильного жителя. Замени на свои. */
public class GroxDwellerRenderer extends MobEntityRenderer<GroxDwellerEntity, VillagerResemblingModel<GroxDwellerEntity>> {
    private static final Identifier TEXTURE = new Identifier("textures/entity/villager/villager.png");

    public GroxDwellerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new VillagerResemblingModel<>(ctx.getPart(EntityModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public Identifier getTexture(GroxDwellerEntity entity) {
        return TEXTURE;
    }
}
