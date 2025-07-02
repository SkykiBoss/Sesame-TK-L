package fansirsqi.xposed.sesame.model;

import java.util.ArrayList;
import java.util.Collections;
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
import fansirsqi.xposed.sesame.task.consumeGold.ConsumeGold;
import fansirsqi.xposed.sesame.task.greenFinance.GreenFinance;
import fansirsqi.xposed.sesame.task.reserve.Reserve;
import lombok.Getter;

/**
 * ModelOrder 类用于管理所有模块的加载顺序和统一注册。
 * 
 * - array 数组存放所有模块的 Class 对象
 * - clazzList 以 List 形式存储模块 Class，方便遍历和操作
 * 
 * 注意：
 * 1. 这里所有模块都列出来，不建议通过注释屏蔽。
 * 2. 是否启用模块，请通过各模块内部的开关字段控制（例如 BaseModel 中的 BooleanModelField）。
 */
public class ModelOrder {

    // 使用 SuppressWarnings 抑制泛型数组警告
    @SuppressWarnings("unchecked")
    private static final Class<Model>[] array = new Class[]{
            BaseModel.class,      // 基础设置模块，必须有
            AntForest.class,      // 蚂蚁森林模块
            AntFarm.class,        // 蚂蚁庄园模块
            AntOrchard.class,     // 蚂蚁果园模块
            AntOcean.class,       // 蚂蚁海洋模块
            AntDodo.class,        // 神奇物种模块
            AncientTree.class,    // 古树模块
            AntCooperate.class,   // 合种模块
            Reserve.class,        // 保护地模块
            AntSports.class,      // 运动模块
            AntMember.class,      // 会员模块
            AntStall.class,       // 蚂蚁新村模块
            GreenFinance.class,   // 绿色经营模块
//          AntBookRead.class,     // 读书模块（没用）
//          ConsumeGold.class,     // 消费金模块（没用）
//          OmegakoiTown.class,   // 小镇模块（没用）
            AnswerAI.class,       // AI答题模块
    };

    /**
     * clazzList 为模块类的列表，方便统一管理和遍历。
     */
    @Getter
    private static final List<Class<? extends Model>> clazzList = new ArrayList<>();

    // 静态代码块，将数组内容加入到 clazzList
    static {
        Collections.addAll(clazzList, array);
    }
}
