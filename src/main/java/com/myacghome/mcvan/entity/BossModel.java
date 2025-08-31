package com.myacghome.mcvan.entity;

import com.myacghome.mcvan.IndexMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
/**CLASS
 describe: boos实体的模型
 notepad: 无
 **/
@SuppressWarnings("all")
public class BossModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(IndexMod.MOD_ID, "boss"), "main");

    public BossModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(// 使用 createMesh 获取模型网格结构
                CubeDeformation.NONE, // 没有立方体变形
                0.0F                  // 贴图偏移
        );
        return LayerDefinition.create(mesh, 64, 64); // 模型贴图宽高
    }
}
