package fansirsqi.xposed.sesame.model;

import java.util.ArrayList;
import java.util.List;

import fansirsqi.xposed.sesame.task.AnswerAI.AnswerAI;
import fansirsqi.xposed.sesame.task.ancientTree.AncientTree;
import fansirsqi.xposed.sesame.task.antCooperate.AntCooperate;
import fansirsqi.xposed.sesame.task.antDodo.AntDodo;
import fansirsqi.xposed.sesame.task.antFarm.AntFarm;
import fansirsqi.xposed.sesame.task.antForest.AntForest;
import fansirsqi.xposed.sesame.task.antMember.AntMember;
import fansirsqi.xposed.sesame.task.antOcean.AntOcean;
import fansirsqi.xposed.sesame.task.antOrchard.AntOrchard;
import fansirsqi.xposed.sesame.task.antSports.AntSports;
import fansirsqi.xposed.sesame.task.antStall.AntStall;
import fansirsqi.xposed.sesame.task.greenFinance.GreenFinance;
import fansirsqi.xposed.sesame.task.reserve.Reserve;
import lombok.Getter;

/**
 * ModelOrder 类用于控制模块加载顺序。
 * 
 * 策略：
 * - 始终加载核心模块（BaseModel, AntForest, AntFarm）
 * - 其余模块按 BaseModel 中布尔开关选择性加载
 */
public class ModelOrder {

    @Getter
    private static final List<Class<? extends Model>> clazzList = new ArrayList<>();

    static {
        // ✅ 始终加载核心模块
        clazzList.add(BaseModel.class);     // 基础设置（必须）
        clazzList.add(AntForest.class);     // 蚂蚁森林（常驻核心）
        clazzList.add(AntFarm.class);       // 蚂蚁庄园（常驻核心）

        // 🔘 按 BaseModel 配置加载其他模块（需确保字段定义存在且已加载配置值）
        if (BaseModel.enableAntOrchard.getValue())     clazzList.add(AntOrchard.class);
        if (BaseModel.enableAntOcean.getValue())       clazzList.add(AntOcean.class);
        if (BaseModel.enableAntDodo.getValue())        clazzList.add(AntDodo.class);
        if (BaseModel.enableAncientTree.getValue())    clazzList.add(AncientTree.class);
        if (BaseModel.enableAntCooperate.getValue())   clazzList.add(AntCooperate.class);
        if (BaseModel.enableReserve.getValue())        clazzList.add(Reserve.class);
        if (BaseModel.enableAntSports.getValue())      clazzList.add(AntSports.class);
        if (BaseModel.enableAntMember.getValue())      clazzList.add(AntMember.class);
        if (BaseModel.enableAntStall.getValue())       clazzList.add(AntStall.class);
        if (BaseModel.enableGreenFinance.getValue())   clazzList.add(GreenFinance.class);
        if (BaseModel.enableAnswerAI.getValue())       clazzList.add(AnswerAI.class);

        // 🚫 其它未启用模块保留位置：如 AntBookRead、ConsumeGold、OmegakoiTown
    }
}
