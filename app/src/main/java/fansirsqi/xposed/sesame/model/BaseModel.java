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
import fansirsqi.xposed.sesame.util.maps.BeachMap;
import fansirsqi.xposed.sesame.util.maps.IdMapManager;
import lombok.Getter;

/**
 * 基础配置模块
 */
public class BaseModel extends Model {
    private static final String TAG = "BaseModel";

    // ================= 基础功能开关 =================

    @Getter public static final BooleanModelField stayAwake = new BooleanModelField("stayAwake", "保持唤醒", true);
    @Getter public static final IntegerModelField.MultiplyIntegerModelField checkInterval =
            new IntegerModelField.MultiplyIntegerModelField("checkInterval", "执行间隔(分钟)", 50, 1, 12 * 60, 60_000);
    @Getter public static final ListModelField.ListJoinCommaToStringModelField execAtTimeList =
            new ListModelField.ListJoinCommaToStringModelField("execAtTimeList", "定时执行(关闭:-1)", ListUtil.newArrayList("0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359"));
    @Getter public static final ListModelField.ListJoinCommaToStringModelField wakenAtTimeList =
            new ListModelField.ListJoinCommaToStringModelField("wakenAtTimeList", "定时唤醒(关闭:-1)", ListUtil.newArrayList("0650", "2350"));
    @Getter public static final ListModelField.ListJoinCommaToStringModelField energyTime =
            new ListModelField.ListJoinCommaToStringModelField("energyTime", "只收能量时间(范围|关闭:-1)", ListUtil.newArrayList("0700-0730"));
    @Getter public static final ListModelField.ListJoinCommaToStringModelField modelSleepTime =
            new ListModelField.ListJoinCommaToStringModelField("modelSleepTime", "模块休眠时间(范围|关闭:-1)", ListUtil.newArrayList("0100-0540"));
    @Getter public static final ChoiceModelField timedTaskModel = new ChoiceModelField("timedTaskModel", "定时任务模式", TimedTaskModel.SYSTEM, TimedTaskModel.nickNames);
    @Getter public static final BooleanModelField timeoutRestart = new BooleanModelField("timeoutRestart", "超时重启", true);
    @Getter public static final IntegerModelField.MultiplyIntegerModelField waitWhenException =
            new IntegerModelField.MultiplyIntegerModelField("waitWhenException", "异常等待时间(分钟)", 60, 0, 24 * 60, 60_000);
    @Getter public static final BooleanModelField errNotify = new BooleanModelField("errNotify", "开启异常通知", false);
    @Getter public static final IntegerModelField setMaxErrorCount = new IntegerModelField("setMaxErrorCount", "异常次数阈值", 8);
    @Getter public static final BooleanModelField newRpc = new BooleanModelField("newRpc", "使用新接口(最低支持v10.3.96.8100)", true);
    @Getter public static final BooleanModelField debugMode = new BooleanModelField("debugMode", "开启抓包(基于新接口)", false);
    @Getter public static final BooleanModelField batteryPerm = new BooleanModelField("batteryPerm", "为支付宝申请后台运行权限", true);
    @Getter public static final BooleanModelField recordLog = new BooleanModelField("recordLog", "全部 | 记录日志", true);
    @Getter public static final BooleanModelField showToast = new BooleanModelField("showToast", "气泡提示", true);
    @Getter public static final IntegerModelField toastOffsetY = new IntegerModelField("toastOffsetY", "气泡纵向偏移", 99);
    @Getter public static final BooleanModelField languageSimplifiedChinese = new BooleanModelField("languageSimplifiedChinese", "只显示中文并设置时区", true);
    @Getter public static final BooleanModelField enableOnGoing = new BooleanModelField("enableOnGoing", "开启状态栏禁删", false);
    @Getter public static final BooleanModelField sendHookData = new BooleanModelField("sendHookData", "启用Hook数据转发", false);
    @Getter static final StringModelField sendHookDataUrl = new StringModelField("sendHookDataUrl", "Hook数据转发地址", "http://127.0.0.1:9527/hook");

    // ================= 模块分组控制（菜单显示控制） =================

    @Getter public static final BooleanModelField enableForestGroup = new BooleanModelField("enableForestGroup", "🌲 启用森林类功能", true);
    @Getter public static final BooleanModelField enableFarmGroup = new BooleanModelField("enableFarmGroup", "🐔 启用庄园类功能", true);
    @Getter public static final BooleanModelField enableOrchardGroup = new BooleanModelField("enableOrchardGroup", "🌾 启用农场类功能", true);
    @Getter public static final BooleanModelField enableOceanGroup = new BooleanModelField("enableOceanGroup", "🐟 启用海洋类功能", true);
    @Getter public static final BooleanModelField enableDodoGroup = new BooleanModelField("enableDodoGroup", "🦕 启用神奇物种类功能", true);
    @Getter public static final BooleanModelField enableTreeGroup = new BooleanModelField("enableTreeGroup", "🌳 启用古树类功能", true);
    @Getter public static final BooleanModelField enableCooperateGroup = new BooleanModelField("enableCooperateGroup", "🤝 启用合种类功能", true);
    @Getter public static final BooleanModelField enableReserveGroup = new BooleanModelField("enableReserveGroup", "🏞 启用保护地类功能", true);
    @Getter public static final BooleanModelField enableSportsGroup = new BooleanModelField("enableSportsGroup", "🏃 启用运动类功能", true);
    @Getter public static final BooleanModelField enableMemberGroup = new BooleanModelField("enableMemberGroup", "👤 启用会员类功能", true);
    @Getter public static final BooleanModelField enableStallGroup = new BooleanModelField("enableStallGroup", "🛒 启用蚂蚁新村类功能", true);
    @Getter public static final BooleanModelField enableGreenGroup = new BooleanModelField("enableGreenGroup", "🌱 启用绿色经营类功能", true);
    @Getter public static final BooleanModelField enableAIGroup = new BooleanModelField("enableAIGroup", "🧠 启用AI答题类功能", true);

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

        // 添加所有基础字段
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

        // 添加菜单模块分组控制字段
        modelFields.addField(enableForestGroup);
        modelFields.addField(enableFarmGroup);
        modelFields.addField(enableOrchardGroup);
        modelFields.addField(enableOceanGroup);
        modelFields.addField(enableDodoGroup);
        modelFields.addField(enableTreeGroup);
        modelFields.addField(enableCooperateGroup);
        modelFields.addField(enableReserveGroup);
        modelFields.addField(enableSportsGroup);
        modelFields.addField(enableMemberGroup);
        modelFields.addField(enableStallGroup);
        modelFields.addField(enableGreenGroup);
        modelFields.addField(enableAIGroup);

        return modelFields;
    }

    /**
     * 初始化数据，延迟模拟加载
     */
    public static void initData() {
        new Thread(() -> {
            try {
                GlobalThreadPools.sleep(4500 + (int) (Math.random() * 1500));
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }).start();
    }

    /**
     * 清理缓存数据
     */
    public static void destroyData() {
        try {
            Log.runtime(TAG, "🧹清理所有数据");
            IdMapManager.getInstance(BeachMap.class).clear();
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /** 定时任务模型 */
    public interface TimedTaskModel {
        int SYSTEM = 0;
        int PROGRAM = 1;
        String[] nickNames = {"🤖系统计时", "📦程序计时"};
    }
}
