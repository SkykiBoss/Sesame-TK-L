package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.data.Status;

public class Privilege {
    private static final String TAG = Privilege.class.getSimpleName();

    private static final String Flag = "youth_privilege_forest_received";
    private static final String Flag2 = "youth_privilege_student_task";
    private static final String YOUTH_PRIVILEGE_PREFIX = "青春特权🌸";
    private static final String STUDENT_SIGN_PREFIX = "青春特权🧧";

    private static final String TASK_RECEIVED = "RECEIVED";
    private static final String TASK_FINISHED = "FINISHED";
    private static final String RPC_SUCCESS = "SUCCESS";

    private static final int SIGN_IN_START_HOUR = 5;
    private static final int SIGN_IN_END_HOUR = 10;

    private static final List<List<String>> YOUTH_TASKS = Arrays.asList(
            Arrays.asList("DNHZ_SL_college", "DAXUESHENG_SJK", "双击卡"),
            Arrays.asList("DXS_BHZ", "NENGLIANGZHAO_20230807", "保护罩"),
            Arrays.asList("DXS_JSQ", "JIASUQI_20230808", "加速器")
    );

    // ==== ✅ 统一入口 ====
    public static void executeTasks() {
        Log.debug(TAG, "==== 开始执行特权任务 ====");
        boolean youthResult = youthPrivilege();
        boolean signResult = executeStudentSignIn();
        Log.debug(TAG, "任务执行结果: [青春特权=" + youthResult + ", 学生签到=" + signResult + "]");
    }

    // ==== 🎯 青春特权任务 ====
    public static boolean youthPrivilege() {
        try {
            if (Status.hasFlagToday(Flag)) {
                Log.record(YOUTH_PRIVILEGE_PREFIX + "今日已处理，跳过");
                return false;
            }

            if (!shouldRunYouthPrivilege()) {
                Log.debug(TAG, "当前不在青春特权处理时间段");
                return false;
            }

            List<String> processResults = new ArrayList<>();
            for (List<String> task : YOUTH_TASKS) {
                processResults.addAll(processYouthPrivilegeTask(task));
            }

            boolean allSuccess = true;
            for (String result : processResults) {
                if (!"处理成功".equals(result)) {
                    allSuccess = false;
                    break;
                }
            }

            if (allSuccess) Status.setFlagToday(Flag);
            return allSuccess;
        } catch (Exception e) {
            Log.printStackTrace(TAG + "青春特权领取异常", e);
            Status.clearFlag(Flag); // 恢复标记以便重试
            return false;
        }
    }

    private static boolean shouldRunYouthPrivilege() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= 6 && hour <= 22;
    }

    private static List<String> processYouthPrivilegeTask(List<String> taskConfig) throws JSONException {
        String queryParam = taskConfig.get(0);
        String receiveParam = taskConfig.get(1);
        String taskName = taskConfig.get(2);

        JSONArray taskList = getTaskList(queryParam);
        return handleTaskList(taskList, receiveParam, taskName);
    }

    private static JSONArray getTaskList(String queryParam) throws JSONException {
        String response = AntForestRpcCall.queryTaskListV2(queryParam);
        JSONObject result = new JSONObject(response);

        if (!result.has("forestTasksNew")) {
            throw new JSONException("Invalid response: forestTasksNew missing");
        }

        JSONArray tasks = result.getJSONArray("forestTasksNew");
        if (tasks.length() == 0) return new JSONArray();

        return tasks.getJSONObject(0).optJSONArray("taskInfoList");
    }

    private static List<String> handleTaskList(JSONArray taskInfoList, String taskType, String taskName) {
        List<String> results = new ArrayList<>();
        if (taskInfoList == null) return results;

        for (int i = 0; i < taskInfoList.length(); i++) {
            JSONObject task = taskInfoList.optJSONObject(i);
            if (task == null) continue;

            JSONObject baseInfo = task.optJSONObject("taskBaseInfo");
            if (baseInfo == null) continue;

            String currentTaskType = safeGetString(baseInfo, "taskType");
            if (!taskType.equals(currentTaskType)) continue;

            processSingleTask(baseInfo, taskType, taskName, results);
        }
        return results;
    }

    private static void processSingleTask(JSONObject baseInfo, String taskType, String taskName, List<String> results) {
        String taskStatus = safeGetString(baseInfo, "taskStatus");

        if (TASK_RECEIVED.equals(taskStatus)) {
            Log.forest(YOUTH_PRIVILEGE_PREFIX + "[" + taskName + "]已领取");
        } else if (TASK_FINISHED.equals(taskStatus)) {
            handleFinishedTask(taskType, taskName, results);
        }
    }

    private static void handleFinishedTask(String taskType, String taskName, List<String> results) {
        try {
            JSONObject response = new JSONObject(AntForestRpcCall.receiveTaskAwardV2(taskType));
            String resultDesc = response.optString("desc");
            results.add(resultDesc);

            String logMessage = "处理成功".equals(resultDesc) ? "领取成功" : "领取结果：" + resultDesc;
            Log.forest(YOUTH_PRIVILEGE_PREFIX + "[" + taskName + "]" + logMessage);
        } catch (JSONException e) {
            Log.printStackTrace(TAG + "奖励领取解析失败", e);
            results.add("处理异常");
        }
    }

    // ==== 🎯 学生签到任务 ====
    private static boolean executeStudentSignIn() {
        Log.debug(TAG, "开始学生签到检查");
        try {
            if (!isSignInTimeValid()) {
                Log.record(STUDENT_SIGN_PREFIX + "不在签到时段（5-10点）");
                return false;
            }

            if (Status.hasFlagToday(Flag2)) {
                Log.record(STUDENT_SIGN_PREFIX + "今日已完成");
                return true;
            }

            String response = AntForestRpcCall.studentQqueryCheckInModel();
            JSONObject result = new JSONObject(response);

            if (!RPC_SUCCESS.equals(result.optString("resultCode"))) {
                Log.error(TAG, STUDENT_SIGN_PREFIX + "查询失败: " + result.optString("resultDesc"));
                return false;
            }

            JSONObject checkInInfo = result.optJSONObject("studentCheckInInfo");
            if (checkInInfo == null) {
                Log.error(TAG, STUDENT_SIGN_PREFIX + "响应格式错误");
                return false;
            }

            if ("DO_TASK".equals(checkInInfo.optString("action"))) {
                Status.setFlagToday(Flag2);
                Log.record(STUDENT_SIGN_PREFIX + "已签到");
                return true;
            }

            // 执行签到
            String tag = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 8 ? "double" : "single";
            JSONObject signResult = new JSONObject(AntForestRpcCall.studentCheckin());

            if (RPC_SUCCESS.equals(signResult.optString("resultCode"))) {
                Status.setFlagToday(Flag2);
                Log.forest(STUDENT_SIGN_PREFIX + tag + "签到成功");
                return true;
            } else {
                String errorMsg = signResult.optString("resultDesc");
                Log.error(TAG, STUDENT_SIGN_PREFIX + tag + "失败: " + errorMsg);
                return false;
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG + "学生签到异常", e);
            Log.debug(TAG, "当前时间: " + Calendar.getInstance().getTime());
            return false;
        }
    }

    private static boolean isSignInTimeValid() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= SIGN_IN_START_HOUR && hour < SIGN_IN_END_HOUR;
    }

    // ==== 🔧 工具方法 ====
    private static String safeGetString(JSONObject obj, String key) {
        return obj != null && obj.has(key) ? obj.optString(key) : "";
    }
}
