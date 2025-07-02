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

public class ModelOrder {

    @Getter
    private static final List<Class<? extends Model>> clazzList = new ArrayList<>();

    static {
        // ✅ 核心模块：始终加载
        clazzList.add(BaseModel.class);
        clazzList.add(AntForest.class);
        clazzList.add(AntFarm.class);

        // 🔘 模块分组映射（根据分组开关决定加载哪些模块）
        Map<Supplier<Boolean>, List<Class<? extends Model>>> groupModules = new LinkedHashMap<>();

        groupModules.put(() -> BaseModel.enableOrchardGroup.getValue(), List.of(
                AntOrchard.class        // 农场
        ));

        groupModules.put(() -> BaseModel.enableOceanGroup.getValue(), List.of(
                AntOcean.class          // 海洋
        ));

        groupModules.put(() -> BaseModel.enableDodoGroup.getValue(), List.of(
                AntDodo.class           // 神奇物种
        ));

        groupModules.put(() -> BaseModel.enableTreeGroup.getValue(), List.of(
                AncientTree.class       // 古树
        ));

        groupModules.put(() -> BaseModel.enableCooperateGroup.getValue(), List.of(
                AntCooperate.class      // 合种
        ));

        groupModules.put(() -> BaseModel.enableReserveGroup.getValue(), List.of(
                Reserve.class           // 保护地
        ));

        groupModules.put(() -> BaseModel.enableSportsGroup.getValue(), List.of(
                AntSports.class         // 运动
        ));

        groupModules.put(() -> BaseModel.enableMemberGroup.getValue(), List.of(
                AntMember.class         // 会员
        ));

        groupModules.put(() -> BaseModel.enableStallGroup.getValue(), List.of(
                AntStall.class          // 蚂蚁新村
        ));

        groupModules.put(() -> BaseModel.enableGreenGroup.getValue(), List.of(
                GreenFinance.class      // 绿色经营
        ));

        groupModules.put(() -> BaseModel.enableAIGroup.getValue(), List.of(
                AnswerAI.class          // AI答题
        ));

        // 遍历每组，判断是否启用，决定是否添加模块
        for (Map.Entry<Supplier<Boolean>, List<Class<? extends Model>>> entry : groupModules.entrySet()) {
            try {
                if (Boolean.TRUE.equals(entry.getKey().get())) {
                    clazzList.addAll(entry.getValue());
                }
            } catch (Exception ignored) {
                // 防御式容错
            }
        }
    }
}
