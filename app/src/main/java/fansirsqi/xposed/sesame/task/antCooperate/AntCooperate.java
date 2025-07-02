package fansirsqi.xposed.sesame.task.antCooperate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Objects;

import fansirsqi.xposed.sesame.entity.CooperateEntity;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.maps.CooperateMap;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class AntCooperate extends ModelTask {
    private static final String TAG = AntCooperate.class.getSimpleName();

    // 任务名称
    @Override
    public String getName() {
        return "合种";
    }

    // 任务分组，方便分类管理
    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    // 任务图标名称
    @Override
    public String getIcon() {
        return "AntCooperate.png";
    }

    /*
     * 以下为任务相关配置字段，支持 UI 显示和用户配置
     */

    // 合种浇水开关，是否开启合种浇水功能
    private final BooleanModelField cooperateWater = new BooleanModelField("cooperateWater", "合种浇水|开启", false);

    // 合种浇水列表，指定哪些合种项目需要浇水及浇水次数
    private final SelectAndCountModelField cooperateWaterList = new SelectAndCountModelField(
            "cooperateWaterList", "合种浇水列表", new LinkedHashMap<>(), CooperateEntity.Companion.getList(), "开启合种浇水后执行一次重载");

    // 浇水总量限制列表，限制每个合种项目当天最大浇水量
    private final SelectAndCountModelField cooperateWaterTotalLimitList = new SelectAndCountModelField(
            "cooperateWaterTotalLimitList", "浇水总量限制列表", new LinkedHashMap<>(), CooperateEntity.Companion.getList());

    // 合种召唤队友浇水开关，仅限队长可用
    private final BooleanModelField cooperateSendCooperateBeckon = new BooleanModelField("cooperateSendCooperateBeckon", "合种 | 召唤队友浇水| 仅队长 ", false);

    // 任务配置字段集合，返回给框架用于界面展示和存储
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(cooperateWater);
        modelFields.addField(cooperateWaterList);
        modelFields.addField(cooperateWaterTotalLimitList);
        modelFields.addField(cooperateSendCooperateBeckon);
        return modelFields;
    }

    // 任务执行前条件判断，避免模块休眠期或只收能量期执行
    @Override
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME) {
            Log.record(TAG, "⏸ 当前为只收能量时间【" + BaseModel.getEnergyTime().getValue() + "】，停止执行" + getName() + "任务！");
            return false;
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            Log.record(TAG, "💤 模块休眠时间【" + BaseModel.getModelSleepTime().getValue() + "】停止执行" + getName() + "任务！");
            return false;
        } else {
            return true;
        }
    }

    // 任务执行主逻辑
    @Override
    public void run() {
        try {
            Log.record(TAG, "执行开始-" + getName());
            if (cooperateWater.getValue()) { // 仅在开关开启时执行
                String s = AntCooperateRpcCall.queryUserCooperatePlantList();
                JSONObject jo = new JSONObject(s);
                if (ResChecker.checkRes(TAG,jo)) {
                    Log.runtime(TAG, "获取合种列表成功");
                    int userCurrentEnergy = jo.getInt("userCurrentEnergy"); // 当前用户剩余能量
                    JSONArray ja = jo.getJSONArray("cooperatePlants"); // 合种列表
                    for (int i = 0; i < ja.length(); i++) {
                        jo = ja.getJSONObject(i);
                        String cooperationId = jo.getString("cooperationId");
                        // 如果合种信息不完整，补充详情
                        if (!jo.has("name")) {
                            s = AntCooperateRpcCall.queryCooperatePlant(cooperationId);
                            jo = new JSONObject(s).getJSONObject("cooperatePlant");
                        }
                        String admin = jo.getString("admin");
                        String name = jo.getString("name");
                        // 如果开启召唤队友且当前用户为队长，执行召唤
                        if (cooperateSendCooperateBeckon.getValue() && Objects.equals(UserMap.getCurrentUid(), admin)) {
                            cooperateSendCooperateBeckon(cooperationId, name);
                        }
                        int waterDayLimit = jo.getInt("waterDayLimit"); // 当天浇水限额
                        Log.runtime(TAG, "合种[" + name + "]: 日限额:" + waterDayLimit);

                        // 缓存合种信息，方便其他地方使用
                        CooperateMap.getInstance(CooperateMap.class).add(cooperationId, name);

                        // 判断今天是否已浇水
                        if (!Status.canCooperateWaterToday(UserMap.getCurrentUid(), cooperationId)) {
                            Log.runtime(TAG, "[" + name + "]今日已浇水💦");
                            continue;
                        }

                        // 获取配置的浇水次数
                        Integer waterId = cooperateWaterList.getValue().get(cooperationId);
                        if (waterId != null) {
                            // 获取限制总次数
                            Integer limitNum = cooperateWaterTotalLimitList.getValue().get(cooperationId);
                            if (limitNum != null) {
                                int cumulativeWaterAmount = calculatedWaterNum(cooperationId);
                                if (cumulativeWaterAmount < 0) {
                                    Log.runtime(TAG, "当前用户[" + UserMap.getCurrentUid() + "]的累计浇水能量获取失败,跳过本次浇水！");
                                    continue;
                                }
                                // 剩余可用浇水次数 = 限制总次数 - 已用次数
                                waterId = limitNum - cumulativeWaterAmount;
                                Log.runtime(TAG, "[" + name + "] 调整后的浇水数量: " + waterId);
                            }
                            // 限制最大浇水数量不超过每日限额
                            if (waterId > waterDayLimit) {
                                waterId = waterDayLimit;
                            }
                            // 限制最大浇水数量不超过用户当前能量
                            if (waterId > userCurrentEnergy) {
                                waterId = userCurrentEnergy;
                            }
                            // 浇水数量大于0时执行浇水
                            if (waterId > 0) {
                                cooperateWater(cooperationId, waterId, name);
                            } else {
                                Log.runtime(TAG, "浇水数量为0，跳过[" + name + "]");
                            }
                        } else {
                            Log.runtime(TAG, "浇水列表中没有为[" + name + "]配置");
                        }
                    }
                } else {
                    Log.error(TAG, "获取合种列表失败:");
                    Log.runtime(TAG + "获取合种列表失败:", jo.getString("resultDesc"));
                }
            } else {
                Log.runtime(TAG, "合种浇水功能未开启");
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            // 保存状态，避免重复浇水
            CooperateMap.getInstance(CooperateMap.class).save(UserMap.getCurrentUid());
            Log.record(TAG, "执行结束-" + getName());
        }
    }

    /**
     * 合种浇水接口调用
     * @param coopId 合种ID
     * @param count 浇水次数
     * @param name 合种名称（用于日志）
     */
    private static void cooperateWater(String coopId, int count, String name) {
        try {
            String s = AntCooperateRpcCall.cooperateWater(UserMap.getCurrentUid(), coopId, count);
            JSONObject jo = new JSONObject(s);
            if (ResChecker.checkRes(TAG,jo)) {
                Log.forest("合种浇水🚿[" + name + "]" + jo.getString("barrageText"));
                Status.cooperateWaterToday(UserMap.getCurrentUid(), coopId);
            } else {
                Log.runtime(TAG, "浇水失败[" + name + "]: " + jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cooperateWater err:");
            Log.printStackTrace(TAG, t);
        } finally {
            // 请求间隔，避免频率过高被限流
            GlobalThreadPools.sleep(1500);
        }
    }

    /**
     * 查询当前用户累计浇水能量，供限制浇水量计算使用
     * @param coopId 合种ID
     * @return 累计浇水能量，失败返回 -1
     */
    private static int calculatedWaterNum(String coopId) {
        try {
            String s = AntCooperateRpcCall.queryCooperateRank("A", coopId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success", false)) {
                JSONArray jaList = jo.getJSONArray("cooperateRankInfos");
                for (int i = 0; i < jaList.length(); i++) {
                    JSONObject joItem = jaList.getJSONObject(i);
                    String userId = joItem.getString("userId");
                    if (userId.equals(UserMap.getCurrentUid())) {
                        int energySummation = joItem.optInt("energySummation", -1);
                        if (energySummation >= 0) {
                            Log.runtime(TAG, "当前用户[" + userId + "]的累计浇水能量: " + energySummation);
                        }
                        return energySummation;
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "calculatedWaterNum err:");
            Log.printStackTrace(TAG, t);
        }
        return -1; // 查询失败返回 -1，表示不做浇水
    }

    /**
     * 队长召唤队友浇水，限18:00以后执行
     * @param cooperationId 合种ID
     * @param name 合种名称（用于日志）
     */
    private static void cooperateSendCooperateBeckon(String cooperationId, String name) {
        try {
            // 早于18:00不召唤
            if (TimeUtil.isNowBeforeTimeStr("1800")) {
                return;
            }
            TimeUtil.sleep(500);
            JSONObject jo = new JSONObject(AntCooperateRpcCall.queryCooperateRank("D", cooperationId));
            if (ResChecker.checkRes(TAG, jo)) {
                JSONArray cooperateRankInfos = jo.getJSONArray("cooperateRankInfos");
                for (int i = 0; i < cooperateRankInfos.length(); i++) {
                    JSONObject rankInfo = cooperateRankInfos.getJSONObject(i);
                    if (rankInfo.getBoolean("canBeckon")) {
                        jo = new JSONObject(AntCooperateRpcCall.sendCooperateBeckon(rankInfo.getString("userId"), cooperationId));
                        if (ResChecker.checkRes(TAG,jo)) {
                            Log.forest("合种🚿[" + name + "]#召唤队友[" + rankInfo.getString("displayName") + "]成功");
                        }
                        TimeUtil.sleep(1000);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cooperateSendCooperateBeckon err:");
            Log.printStackTrace(TAG, t);
        }
    }
}
