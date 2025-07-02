package fansirsqi.xposed.sesame.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
 * ModelOrder 类用于控制各功能模块的加载顺序。
 *
 * <p>加载策略说明：
 * - 始终加载核心模块：BaseModel、AntForest、AntFarm；
 * - 其余模块由 BaseModel 中对应的布尔开关动态决定是否加载；
 * - 保证加载顺序稳定性，避免功能依赖异常；
 */
public class ModelOrder {

    /** 模块类列表，控制最终加载顺序 */
    @Getter
    private static final List<Class<? extends Model>> clazzList = new ArrayList<>();

    static {
        // ✅ 核心模块：始终加载，不受配置控制（必须执行的基础功能）
        clazzList.add(BaseModel.class);     // 基础配置模块，包含所有开关
        clazzList.add(AntForest.class);     // 蚂蚁森林模块
        clazzList.add(AntFarm.class);       // 蚂蚁庄园模块

        // 🔘 可选模块：根据 BaseModel 中的布尔开关动态决定是否启用
        // 使用 LinkedHashMap 保持添加顺序一致
        Map<Supplier<Boolean>, Class<? extends Model>> optionalModules = new LinkedHashMap<>();

        // 每个条目形式：开关字段 -> 模块类
        optionalModules.put(() -> BaseModel.enableAntOrchard.getValue(), AntOrchard.class);       // 蚂蚁农场
        optionalModules.put(() -> BaseModel.enableAntOcean.getValue(), AntOcean.class);           // 蚂蚁海洋
        optionalModules.put(() -> BaseModel.enableAntDodo.getValue(), AntDodo.class);             // 神奇物种
        optionalModules.put(() -> BaseModel.enableAncientTree.getValue(), AncientTree.class);     // 古树
        optionalModules.put(() -> BaseModel.enableAntCooperate.getValue(), AntCooperate.class);   // 合种
        optionalModules.put(() -> BaseModel.enableReserve.getValue(), Reserve.class);             // 保护地
        optionalModules.put(() -> BaseModel.enableAntSports.getValue(), AntSports.class);         // 蚂蚁运动
        optionalModules.put(() -> BaseModel.enableAntMember.getValue(), AntMember.class);         // 蚂蚁会员
        optionalModules.put(() -> BaseModel.enableAntStall.getValue(), AntStall.class);           // 蚂蚁新村
        optionalModules.put(() -> BaseModel.enableGreenFinance.getValue(), GreenFinance.class);   // 绿色经营
        optionalModules.put(() -> BaseModel.enableAnswerAI.getValue(), AnswerAI.class);           // AI答题

        // 遍历 map，根据配置字段是否启用动态决定是否加入到 clazzList 中
        for (Map.Entry<Supplier<Boolean>, Class<? extends Model>> entry : optionalModules.entrySet()) {
            try {
                // 安全判断，避免 null 值引发异常
                if (Boolean.TRUE.equals(entry.getKey().get())) {
                    clazzList.add(entry.getValue());
                }
            } catch (Exception ignored) {
                // 防御式容错：个别字段未初始化或取值异常不影响整体加载流程
            }
        }

        // 🚫 预留位：如后续扩展模块如 AntBookRead、ConsumeGold 等
    }
}
