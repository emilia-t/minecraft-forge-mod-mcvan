package com.myacghome.mcvan.entity;

import com.myacghome.mcvan.IndexMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
/**CLASS
 describe: boss实体渲染器
 notepad: 无
 **/
@SuppressWarnings("all")
public class BossRenderer extends MobRenderer<BossEntity, BossModel<BossEntity>> {
    public BossRenderer(EntityRendererProvider.Context context) {
        super(context, new BossModel<>(context.bakeLayer(BossModel.LAYER_LOCATION)), 0.7f);
    }

    @Override
    public ResourceLocation getTextureLocation(BossEntity entity) {
        return new ResourceLocation(IndexMod.MOD_ID, "textures/entity/boss.png");
    }
}
