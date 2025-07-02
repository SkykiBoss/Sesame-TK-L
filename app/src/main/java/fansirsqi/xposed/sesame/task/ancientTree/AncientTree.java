package fansirsqi.xposed.sesame.task.ancientTree;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;

import fansirsqi.xposed.sesame.entity.AreaCode;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ResChecker;
import fansirsqi.xposed.sesame.data.Status;

/**
 * 古树保护任务
 * 负责周期性保护指定区域内的古树
 */
public class AncientTree extends ModelTask {
    private static final String TAG = AncientTree.class.getSimpleName();

    // 运行时配置字段：是否只在周一、三、五运行
    private BooleanModelField ancientTreeOnlyWeek;
    // 可选古树区划代码集合，供用户选择目标保护区域
    private SelectModelField ancientTreeCityCodeList;

    @Override
    public String getName() {
        return "古树";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    @Override
    public String getIcon() {
        return "AncientTree.png";
    }

    /**
     * 返回配置字段，UI用来渲染设置项
     */
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        // 只在周一、三、五执行，默认false
        modelFields.addField(ancientTreeOnlyWeek = new BooleanModelField("ancientTreeOnlyWeek", "仅星期一、三、五运行保护古树", false));
        // 支持多选区划代码，数据来源为AreaCode.getList()
        modelFields.addField(ancientTreeCityCodeList = new SelectModelField("ancientTreeCityCodeList", "古树区划代码列表", new LinkedHashSet<>(), AreaCode::getList));
        return modelFields;
    }

    /**
     * 判断当前是否满足运行条件
     * - 当前不是能量收集时间，并且时间是8点之后
     * - 若勾选仅周一三五，则判断当天是否符合
     */
    @Override
    public Boolean check() {
        if (!TaskCommon.IS_ENERGY_TIME && TaskCommon.IS_AFTER_8AM) {
            if (!ancientTreeOnlyWeek.getValue()) {
                return true;
            }
            SimpleDateFormat sdf_week = new SimpleDateFormat("EEEE", Locale.getDefault());
            String week = sdf_week.format(new Date());
            return "星期一".equals(week) || "星期三".equals(week) || "星期五".equals(week);
        }
        return false;
    }

    /**
     * 任务执行入口
     */
    @Override
    public void run() {
        try {
            Log.record(TAG, "开始执行" + getName());
            // 调用古树保护具体逻辑，传入选中的城市区划代码列表
            ancientTree(ancientTreeCityCodeList.getValue());
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.record(TAG, "结束执行" + getName());
        }
    }

    /**
     * 遍历城市区划列表，保护符合条件的古树
     */
    private static void ancientTree(Collection<String> ancientTreeCityCodeList) {
        try {
            for (String cityCode : ancientTreeCityCodeList) {
                // 判断该城市当天是否还能保护古树
                if (!Status.canAncientTreeToday(cityCode))
                    continue;
                // 执行保护动作
                ancientTreeProtect(cityCode);
                GlobalThreadPools.sleep(1000L);
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "ancientTree err:");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 保护指定城市代码下的古树
     * @param cityCode 城市代码
     */
    private static void ancientTreeProtect(String cityCode) {
        try {
            // 获取古树主页数据
            JSONObject jo = new JSONObject(AncientTreeRpcCall.homePage(cityCode));
            if (ResChecker.checkRes(TAG, jo)) {
                JSONObject data = jo.getJSONObject("data");
                if (!data.has("districtBriefInfoList")) {
                    return;
                }
                JSONArray districtBriefInfoList = data.getJSONArray("districtBriefInfoList");
                for (int i = 0; i < districtBriefInfoList.length(); i++) {
                    JSONObject districtBriefInfo = districtBriefInfoList.getJSONObject(i);
                    int userCanProtectTreeNum = districtBriefInfo.optInt("userCanProtectTreeNum", 0);
                    if (userCanProtectTreeNum < 1)
                        continue;
                    JSONObject districtInfo = districtBriefInfo.getJSONObject("districtInfo");
                    String districtCode = districtInfo.getString("districtCode");
                    // 针对区划代码，执行详细保护
                    districtDetail(districtCode);
                    GlobalThreadPools.sleep(1000L);
                }
                // 记录当天保护状态
                Status.ancientTreeToday(cityCode);
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "ancientTreeProtect err:");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 处理具体的区划保护逻辑
     * @param districtCode 区划代码
     */
    private static void districtDetail(String districtCode) {
        try {
            JSONObject jo = new JSONObject(AncientTreeRpcCall.districtDetail(districtCode));
            if (ResChecker.checkRes(TAG, jo)) {
                JSONObject data = jo.getJSONObject("data");
                if (!data.has("ancientTreeList")) {
                    return;
                }
                JSONObject districtInfo = data.getJSONObject("districtInfo");
                String cityCode = districtInfo.getString("cityCode");
                String cityName = districtInfo.getString("cityName");
                String districtName = districtInfo.getString("districtName");
                JSONArray ancientTreeList = data.getJSONArray("ancientTreeList");
                for (int i = 0; i < ancientTreeList.length(); i++) {
                    JSONObject ancientTreeItem = ancientTreeList.getJSONObject(i);
                    // 如果古树已经保护过，跳过
                    if (ancientTreeItem.getBoolean("hasProtected"))
                        continue;
                    JSONObject ancientTreeControlInfo = ancientTreeItem.getJSONObject("ancientTreeControlInfo");
                    int quota = ancientTreeControlInfo.optInt("quota", 0);
                    int useQuota = ancientTreeControlInfo.optInt("useQuota", 0);
                    if (quota <= useQuota)
                        continue;
                    String itemId = ancientTreeItem.getString("projectId");
                    JSONObject ancientTreeDetail = new JSONObject(AncientTreeRpcCall.projectDetail(itemId, cityCode));
                    if (ResChecker.checkRes(TAG, ancientTreeDetail)) {
                        data = ancientTreeDetail.getJSONObject("data");
                        if (data.getBoolean("canProtect")) {
                            int currentEnergy = data.getInt("currentEnergy");
                            JSONObject ancientTree = data.getJSONObject("ancientTree");
                            String activityId = ancientTree.getString("activityId");
                            String projectId = ancientTree.getString("projectId");
                            JSONObject ancientTreeInfo = ancientTree.getJSONObject("ancientTreeInfo");
                            String name = ancientTreeInfo.getString("name");
                            int age = ancientTreeInfo.getInt("age");
                            int protectExpense = ancientTreeInfo.getInt("protectExpense");
                            cityCode = ancientTreeInfo.getString("cityCode");
                            // 如果当前能量不足以保护，结束循环
                            if (currentEnergy < protectExpense)
                                break;
                            GlobalThreadPools.sleep(200);
                            // 发送保护请求
                            jo = new JSONObject(AncientTreeRpcCall.protect(activityId, projectId, cityCode));
                            if (ResChecker.checkRes(TAG, jo)) {
                                Log.forest("保护古树🎐[" + cityName + "-" + districtName
                                        + "]#" + age + "年" + name + ",消耗能量" + protectExpense + "g");
                            } else {
                                Log.record(jo.getString("resultDesc"));
                                Log.runtime(jo.toString());
                            }
                        }
                    } else {
                        Log.record(jo.getString("resultDesc"));
                        Log.runtime(ancientTreeDetail.toString());
                    }
                    GlobalThreadPools.sleep(500L);
                }
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "districtDetail err:");
            Log.printStackTrace(TAG, th);
        }
    }
}
