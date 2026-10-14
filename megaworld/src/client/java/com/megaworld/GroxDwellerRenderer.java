package com.megaworld;

import com.megaworld.client.GroxDwellerModel;
import com.megaworld.entity.GroxDwellerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Рендерер жителя Грокса: своя модель + своя текстура. */
public class GroxDwellerRenderer extends MobEntityRenderer<GroxDwellerEntity, GroxDwellerModel> {
    private static final Identifier TEXTURE = MegaWorld.id("textures/entity/grox_dweller.png");

    public GroxDwellerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new GroxDwellerModel(ctx.getPart(GroxDwellerModel.LAYER)), 0.5F);
    }

    @Override
    public Identifier getTexture(GroxDwellerEntity entity) {
        return TEXTURE;
    }
}
