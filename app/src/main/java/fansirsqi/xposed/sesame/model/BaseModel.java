package fansirsqi.xposed.sesame.model;

import java.util.concurrent.ExecutorService;

import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.ListUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.IdMapManager;
import fansirsqi.xposed.sesame.util.maps.BeachMap;
import lombok.Getter;

/**
 * BaseModel 是整个模块系统的基础配置模型。
 * - 管理所有公共功能参数
 * - 定义各子模块的启用开关
 */
public class BaseModel extends Model {
    private static final String TAG = "BaseModel";

    // ===================== 基础功能字段 =====================

    @Getter public static final BooleanModelField stayAwake = new BooleanModelField("stayAwake", "保持唤醒", true);
    @Getter public static final IntegerModelField.MultiplyIntegerModelField checkInterval =
            new IntegerModelField.MultiplyIntegerModelField("checkInterval", "执行间隔(分钟)", 50, 1, 12 * 60, 60_000);
    @Getter public static final ListModelField.ListJoinCommaToStringModelField execAtTimeList =
            new ListModelField.ListJoinCommaToStringModelField("execAtTimeList", "定时执行(关闭:-1)", ListUtil.newArrayList("-1"));
    @Getter public static final ListModelField.ListJoinCommaToStringModelField wakenAtTimeList =
            new ListModelField.ListJoinCommaToStringModelField("wakenAtTimeList", "定时唤醒(关闭:-1)", ListUtil.newArrayList("-1"));
    @Getter public static final ListModelField.ListJoinCommaToStringModelField energyTime =
            new ListModelField.ListJoinCommaToStringModelField("energyTime", "只收能量时间(范围|关闭:-1)", ListUtil.newArrayList("0700-0730"));
    @Getter public static final ListModelField.ListJoinCommaToStringModelField modelSleepTime =
            new ListModelField.ListJoinCommaToStringModelField("modelSleepTime", "模块休眠时间(范围|关闭:-1)", ListUtil.newArrayList("-1"));
    @Getter public static final ChoiceModelField timedTaskModel =
            new ChoiceModelField("timedTaskModel", "定时任务模式", TimedTaskModel.SYSTEM, TimedTaskModel.nickNames);
    @Getter public static final BooleanModelField timeoutRestart = new BooleanModelField("timeoutRestart", "超时重启", true);
    @Getter public static final IntegerModelField.MultiplyIntegerModelField waitWhenException =
            new IntegerModelField.MultiplyIntegerModelField("waitWhenException", "异常等待时间(分钟)", 60, 0, 24 * 60, 60_000);
    @Getter public static final BooleanModelField errNotify = new BooleanModelField("errNotify", "开启异常通知", false);
    @Getter public static final IntegerModelField setMaxErrorCount = new IntegerModelField("setMaxErrorCount", "异常次数阈值", 8);
    @Getter public static final BooleanModelField newRpc = new BooleanModelField("newRpc", "使用新接口(最低支持v10.3.96.8100)", true);
    @Getter public static final BooleanModelField debugMode = new BooleanModelField("debugMode", "开启抓包(基于新接口)", true);
    @Getter public static final BooleanModelField batteryPerm = new BooleanModelField("batteryPerm", "为支付宝申请后台运行权限", true);
    @Getter public static final BooleanModelField recordLog = new BooleanModelField("recordLog", "全部 | 记录日志", true);
    @Getter public static final BooleanModelField showToast = new BooleanModelField("showToast", "气泡提示", true);
    @Getter public static final IntegerModelField toastOffsetY = new IntegerModelField("toastOffsetY", "气泡纵向偏移", 99);
    @Getter public static final BooleanModelField languageSimplifiedChinese = new BooleanModelField("languageSimplifiedChinese", "只显示中文并设置时区", true);
    @Getter public static final BooleanModelField enableOnGoing = new BooleanModelField("enableOnGoing", "开启状态栏禁删", false);
    @Getter public static final BooleanModelField sendHookData = new BooleanModelField("sendHookData", "启用Hook数据转发", false);
    @Getter static final StringModelField sendHookDataUrl = new StringModelField("sendHookDataUrl", "Hook数据转发地址", "http://127.0.0.1:9527/hook");

    // ===================== 各模块启用开关字段 =====================

    @Getter public static final BooleanModelField enableAntForest     = new BooleanModelField("enableAntForest", "启用蚂蚁森林", true);
    @Getter public static final BooleanModelField enableAntFarm       = new BooleanModelField("enableAntFarm", "启用蚂蚁庄园", true);
    @Getter public static final BooleanModelField enableAntOrchard    = new BooleanModelField("enableAntOrchard", "启用蚂蚁农场", true);
    @Getter public static final BooleanModelField enableAntOcean      = new BooleanModelField("enableAntOcean", "启用蚂蚁海洋", true);
    @Getter public static final BooleanModelField enableAntDodo       = new BooleanModelField("enableAntDodo", "启用神奇物种", false);
    @Getter public static final BooleanModelField enableAncientTree   = new BooleanModelField("enableAncientTree", "启用古树", false);
    @Getter public static final BooleanModelField enableAntCooperate  = new BooleanModelField("enableAntCooperate", "启用合种", false);
    @Getter public static final BooleanModelField enableReserve       = new BooleanModelField("enableReserve", "启用保护地", false);
    @Getter public static final BooleanModelField enableAntSports     = new BooleanModelField("enableAntSports", "启用蚂蚁运动", true);
    @Getter public static final BooleanModelField enableAntMember     = new BooleanModelField("enableAntMember", "启用蚂蚁会员", true);
    @Getter public static final BooleanModelField enableAntStall      = new BooleanModelField("enableAntStall", "启用蚂蚁新村", false);
    @Getter public static final BooleanModelField enableGreenFinance  = new BooleanModelField("enableGreenFinance", "启用绿色经营", false);
    @Getter public static final BooleanModelField enableAnswerAI      = new BooleanModelField("enableAnswerAI", "启用AI答题", true);

    // ===================== 模型元数据定义 =====================

    @Override
    public String getName() {
        return "基础";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.BASE;
    }

    @Override
    public String getIcon() {
        return "BaseModel.png";
    }

    @Override
    public String getEnableFieldName() {
        return "启用模块";
    }

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();

        // 添加基础功能字段
        modelFields.addField(stayAwake);
        modelFields.addField(checkInterval);
        modelFields.addField(modelSleepTime);
        modelFields.addField(execAtTimeList);
        modelFields.addField(wakenAtTimeList);
        modelFields.addField(energyTime);
        modelFields.addField(timedTaskModel);
        modelFields.addField(timeoutRestart);
        modelFields.addField(waitWhenException);
        modelFields.addField(errNotify);
        modelFields.addField(setMaxErrorCount);
        modelFields.addField(newRpc);
        modelFields.addField(debugMode);
        modelFields.addField(sendHookData);
        modelFields.addField(sendHookDataUrl);
        modelFields.addField(batteryPerm);
        modelFields.addField(recordLog);
        modelFields.addField(showToast);
        modelFields.addField(enableOnGoing);
        modelFields.addField(languageSimplifiedChinese);
        modelFields.addField(toastOffsetY);

        // 添加模块启用字段（顺序保持与 ModelOrder 一致）
        modelFields.addField(enableAntForest);
        modelFields.addField(enableAntFarm);
        modelFields.addField(enableAntOrchard);
        modelFields.addField(enableAntOcean);
        modelFields.addField(enableAntDodo);
        modelFields.addField(enableAncientTree);
        modelFields.addField(enableAntCooperate);
        modelFields.addField(enableReserve);
        modelFields.addField(enableAntSports);
        modelFields.addField(enableAntMember);
        modelFields.addField(enableAntStall);
        modelFields.addField(enableGreenFinance);
        modelFields.addField(enableAnswerAI);

        return modelFields;
    }

    // ===================== 生命周期控制 =====================

    /** 初始化数据（异步执行） */
    public static void initData() {
        new Thread(() -> {
            try {
                GlobalThreadPools.sleep(RandomUtil.nextInt(4500, 6000));
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }).start();
    }

    /** 清理资源数据 */
    public static void destroyData() {
        try {
            Log.runtime(TAG, "🧹清理所有数据");
            IdMapManager.getInstance(BeachMap.class).clear();
            // TODO: 根据需要清理其他缓存，如 ReserveMap、CooperateMap 等
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /** 定时任务模式枚举定义 */
    public interface TimedTaskModel {
        int SYSTEM = 0;
        int PROGRAM = 1;
        String[] nickNames = {"🤖系统计时", "📦程序计时"};
    }
}
