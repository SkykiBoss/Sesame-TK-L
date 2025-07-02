package fansirsqi.xposed.sesame.task.antBookRead;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.data.RuntimeInfo;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.StringUtil;

/**
 * 读书听书任务
 * 模拟用户完成读书、听书相关任务以获取能量和奖励
 */
public class AntBookRead extends ModelTask {
    private static final String TAG = AntBookRead.class.getSimpleName();

    @Override
    public String getName() {
        return "读书听书";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.OTHER;
    }

    @Override
    public String getIcon() {
        return "AntBookRead.png";
    }

    @Override
    public ModelFields getFields() {
        // 暂无配置字段
        return new ModelFields();
    }

    /**
     * 判断是否执行任务：
     *  - 非能量收集时间且当前时间在8点以后
     *  - 距离上次执行已超过6小时（21600000毫秒）
     */
    @Override
    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME || !TaskCommon.IS_AFTER_8AM) {
            return false;
        }
        long executeTime = RuntimeInfo.getInstance().getLong("consumeGold", 0);
        return System.currentTimeMillis() - executeTime >= 21600000;
    }

    /**
     * 执行任务逻辑
     */
    @Override
    public void run() {
        try {
            Log.other("执行开始-" + getName());
            // 记录本次执行时间，防止短时间内多次执行
            RuntimeInfo.getInstance().put("consumeGold", System.currentTimeMillis());

            queryTaskCenterPage();
            queryTask();
            queryTreasureBox();
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            Log.other("执行结束-" + getName());
        }
    }

    /**
     * 查询任务中心主页，判断听读时长并尝试同步读书进度获取能量
     */
    private static void queryTaskCenterPage() {
        try {
            String s = AntBookReadRpcCall.queryTaskCenterPage();
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                // 获取今日听读时长文本，例如"今日听读时长 200 分钟"
                String todayPlayDurationText = data.getJSONObject("benefitAggBlock").getString("todayPlayDurationText");
                int playDuration = Integer.parseInt(StringUtil.getSubString(todayPlayDurationText, "今日听读时长", "分钟"));
                if (playDuration < 450) {  // 低于7.5小时则继续“听书”
                    jo = new JSONObject(AntBookReadRpcCall.queryHomePage());
                    if (jo.optBoolean("success")) {
                        JSONArray bookList = jo.getJSONObject("data").getJSONArray("dynamicCardList")
                                .getJSONObject(0)
                                .getJSONObject("data")
                                .getJSONArray("bookList");
                        int bookListLength = bookList.length();
                        int position = RandomUtil.nextInt(0, bookListLength - 1);
                        JSONObject book = bookList.getJSONObject(position);
                        String bookId = book.getString("bookId");

                        jo = new JSONObject(AntBookReadRpcCall.queryReaderContent(bookId));
                        if (jo.optBoolean("success")) {
                            String nextChapterId = jo.getJSONObject("data").getString("nextChapterId");
                            String name = jo.getJSONObject("data").getJSONObject("readerHomePageVO").getString("name");

                            // 模拟读17次章节，累计能量达到150g就停止
                            for (int i = 0; i < 17; i++) {
                                int energy = 0;
                                jo = new JSONObject(AntBookReadRpcCall.syncUserReadInfo(bookId, nextChapterId));
                                if (jo.optBoolean("success")) {
                                    jo = new JSONObject(AntBookReadRpcCall.queryReaderForestEnergyInfo(bookId));
                                    if (jo.optBoolean("success")) {
                                        String tips = jo.getJSONObject("data").getString("tips");
                                        if (tips.contains("已得")) {
                                            energy = Integer.parseInt(StringUtil.getSubString(tips, "已得", "g"));
                                        }
                                        Log.forest("阅读书籍📚[" + name + "]#累计能量" + energy + "g");
                                    }
                                }
                                if (energy >= 150) {
                                    break;
                                } else {
                                    GlobalThreadPools.sleep(1500L);
                                }
                            }
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryTaskCenterPage err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 查询任务列表并处理未完成任务（领取奖励、完成任务等）
     */
    private static void queryTask() {
        boolean doubleCheck = false;
        try {
            String s = AntBookReadRpcCall.queryTaskCenterPage();
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                JSONObject data = jo.getJSONObject("data");
                JSONArray userTaskGroupList = data.getJSONObject("userTaskListModuleVO").getJSONArray("userTaskGroupList");
                for (int i = 0; i < userTaskGroupList.length(); i++) {
                    jo = userTaskGroupList.getJSONObject(i);
                    JSONArray userTaskList = jo.getJSONArray("userTaskList");
                    for (int j = 0; j < userTaskList.length(); j++) {
                        JSONObject taskInfo = userTaskList.getJSONObject(j);
                        String taskStatus = taskInfo.getString("taskStatus");
                        String taskType = taskInfo.getString("taskType");
                        String title = taskInfo.getString("title");
                        if ("TO_RECEIVE".equals(taskStatus)) {
                            // 处理多阶段读书任务的子任务奖励领取
                            if ("READ_MULTISTAGE".equals(taskType)) {
                                JSONArray multiSubTaskList = taskInfo.getJSONArray("multiSubTaskList");
                                for (int k = 0; k < multiSubTaskList.length(); k++) {
                                    taskInfo = multiSubTaskList.getJSONObject(k);
                                    taskStatus = taskInfo.getString("taskStatus");
                                    if ("TO_RECEIVE".equals(taskStatus)) {
                                        String taskId = taskInfo.getString("taskId");
                                        collectTaskPrize(taskId, taskType, title);
                                    }
                                }
                            } else {
                                String taskId = taskInfo.getString("taskId");
                                collectTaskPrize(taskId, taskType, title);
                            }
                        } else if ("NOT_DONE".equals(taskStatus)) {
                            // 处理广告视频任务及其他任务完成流程
                            if ("AD_VIDEO_TASK".equals(taskType)) {
                                String taskId = taskInfo.getString("taskId");
                                for (int m = 0; m < 5; m++) {
                                    taskFinish(taskId, taskType);
                                    GlobalThreadPools.sleep(1500L);
                                    collectTaskPrize(taskId, taskType, title);
                                    GlobalThreadPools.sleep(1500L);
                                }
                            } else if ("FOLLOW_UP".equals(taskType) || "JUMP".equals(taskType)) {
                                String taskId = taskInfo.getString("taskId");
                                taskFinish(taskId, taskType);
                                doubleCheck = true;  // 任务完成后再次检查任务状态
                            }
                        }
                    }
                }
                if (doubleCheck)
                    queryTask();
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryTask err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 领取任务奖励
     */
    private static void collectTaskPrize(String taskId, String taskType, String name) {
        try {
            String s = AntBookReadRpcCall.collectTaskPrize(taskId, taskType);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                int coinNum = jo.getJSONObject("data").getInt("coinNum");
                Log.other("阅读任务📖[" + name + "]#" + coinNum);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "collectTaskPrize err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 标记任务完成
     */
    private static void taskFinish(String taskId, String taskType) {
        try {
            String s = AntBookReadRpcCall.taskFinish(taskId, taskType);
            JSONObject jo = new JSONObject(s);
            jo.optBoolean("success"); // 这里没做额外处理
        } catch (Throwable t) {
            Log.runtime(TAG, "taskFinish err:");
            Log.printStackTrace(TAG, t);
        }
    }

    /**
     * 查询并打开宝箱，领取奖励
     */
    private static void queryTreasureBox() {
        try {
            String s = AntBookReadRpcCall.queryTreasureBox();
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                JSONObject treasureBoxVo = jo.getJSONObject("data").getJSONObject("treasureBoxVo");
                // 如果有倒计时则跳过
                if (treasureBoxVo.has("countdown"))
                    return;
                String status = treasureBoxVo.getString("status");
                if ("CAN_OPEN".equals(status)) {
                    jo = new JSONObject(AntBookReadRpcCall.openTreasureBox());
                    if (jo.optBoolean("success")) {
                        int coinNum = jo.getJSONObject("data").getInt("coinNum");
                        Log.other("阅读任务📖[打开宝箱]#" + coinNum);
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryTreasureBox err:");
            Log.printStackTrace(TAG, t);
        }
    }
}
