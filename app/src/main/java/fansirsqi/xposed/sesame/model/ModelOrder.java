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
 * ModelOrder 类用于控制所有模块的加载顺序。
 *
 * - 默认情况下，加载全部模块（包括菜单模块）；
 * - 若用户手动关闭菜单开关（BaseModel.enableMenus），则只加载核心模块；
 * - 核心模块不可被关闭，始终执行；
 * - 菜单模块可被开关统一控制隐藏与禁用；
 */
public class ModelOrder {

    /**
     * 核心模块：始终加载，无论开关是否关闭。
     */
    private static final List<Class<? extends Model>> coreModules = List.of(
            BaseModel.class,    // 基础配置（开关定义在此）
            AntForest.class,    // 蚂蚁森林模块（核心功能）
            AntFarm.class       // 蚂蚁庄园模块（核心功能）
    );

    /**
     * 菜单模块：可通过开关 BaseModel.enableMenus 控制启用。
     * 关闭开关后，以下模块全部不加载。
     */
    private static final List<Class<? extends Model>> menuModules = List.of(
            AntOrchard.class,     // 蚂蚁农场
            AntOcean.class,       // 蚂蚁海洋
            AntDodo.class,        // 神奇物种
            AncientTree.class,    // 古树
            AntCooperate.class,   // 合种
            Reserve.class,        // 保护地
            AntSports.class,      // 蚂蚁运动
            AntMember.class,      // 蚂蚁会员
            AntStall.class,       // 蚂蚁新村
            GreenFinance.class,   // 绿色经营
            AnswerAI.class        // AI答题
            // 其他预留模块可以在此添加，如：ConsumeGold.class, AntBookRead.class 等
    );

    /**
     * 最终加载到系统中的模块类列表，按顺序执行。
     * 注意：顺序很重要，可能影响 Hook 初始化时机。
     */
    @Getter
    private static final List<Class<? extends Model>> clazzList = new ArrayList<>();

    static {
        reloadModules(); // 初始化时自动加载模块
    }

    /**
     * 根据当前配置动态重新加载模块。
     * 需要在 BaseModel.enableMenus 被修改后调用。
     */
    public static void reloadModules() {
        clazzList.clear();
        clazzList.addAll(coreModules); // 必须加载的模块
        // 如果未明确关闭开关，或为 true，则加载菜单模块
        if (BaseModel.enableMenus.getValue() == null || Boolean.TRUE.equals(BaseModel.enableMenus.getValue())) {
            clazzList.addAll(menuModules);
        }
    }
}
