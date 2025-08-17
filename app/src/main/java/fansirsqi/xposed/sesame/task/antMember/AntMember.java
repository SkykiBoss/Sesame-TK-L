package fansirsqi.xposed.sesame.task.antMember;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashSet;

import fansirsqi.xposed.sesame.entity.MemberBenefit;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.entity.CustomOption;
import fansirsqi.xposed.sesame.entity.PromiseSimpleTemplate;
import fansirsqi.xposed.sesame.util.MessageUtil;

public class AntMember extends ModelTask {
  private static final String TAG = AntMember.class.getSimpleName();
  @Override
  public String getName() {
    return "会员";
  }
  @Override
  public ModelGroup getGroup() {
    return ModelGroup.MEMBER;
  }
  @Override
  public String getIcon() {
    return "AntMember.png";
  }
  private BooleanModelField memberSign;
  private BooleanModelField memberPointExchangeBenefit;
  private SelectModelField memberPointExchangeBenefitList;
  private BooleanModelField collectSesame;
  private BooleanModelField promise;
  private SelectModelField promiseList;
  private BooleanModelField KuaiDiFuLiJia;
  private BooleanModelField antInsurance;
  private SelectModelField antInsuranceOptions;
  private BooleanModelField signinCalendar;
  private BooleanModelField enableGoldTicket;
  private BooleanModelField enableGameCenter;
  private BooleanModelField merchantSignIn;
  private BooleanModelField merchantKMDK;
  private BooleanModelField beanSignIn;
  private BooleanModelField beanExchangeBubbleBoost;

  @Override
  public ModelFields getFields() {
    ModelFields modelFields = new ModelFields();
    modelFields.addField(memberSign = new BooleanModelField("memberSign", "会员签到", false));
    modelFields.addField(memberPointExchangeBenefit = new BooleanModelField("memberPointExchangeBenefit", "会员积分 | 兑换权益", false));
    modelFields.addField(memberPointExchangeBenefitList = new SelectModelField("memberPointExchangeBenefitList", "会员积分 | 权益列表", new LinkedHashSet<>(), MemberBenefit.Companion.getList()));
    modelFields.addField(collectSesame = new BooleanModelField("collectSesame", "芝麻粒 | 领取", false));
    modelFields.addField(promise = new BooleanModelField("promise", "生活记录 | 坚持做", false));
    modelFields.addField(promiseList = new SelectModelField("promiseList", "生活记录 | 坚持做列表", new LinkedHashSet<>(), PromiseSimpleTemplate::getList));
    modelFields.addField(KuaiDiFuLiJia = new BooleanModelField("KuaiDiFuLiJia", "我的快递 | 福利加", false));
    modelFields.addField(antInsurance = new BooleanModelField("antInsurance", "蚂蚁保 | 开启", false));
    modelFields.addField(antInsuranceOptions = new SelectModelField("antInsuranceOptions", "蚂蚁保 | 选项", new LinkedHashSet<>(), CustomOption::getAntInsuranceOptions));
    modelFields.addField(signinCalendar = new BooleanModelField("signinCalendar", "消费金 | 签到", false));
    modelFields.addField(enableGoldTicket = new BooleanModelField("enableGoldTicket", "黄金票 | 签到", false));
    modelFields.addField(enableGameCenter = new BooleanModelField("enableGameCenter", "游戏中心 | 签到", false));
    modelFields.addField(merchantSignIn = new BooleanModelField("merchantSignIn", "商家服务 | 签到", false));
    modelFields.addField(merchantKMDK = new BooleanModelField("merchantKMDK", "商家服务 | 开门打卡", false));
    modelFields.addField(beanSignIn = new BooleanModelField("beanSignIn", "安心豆签到", false));
    modelFields.addField(beanExchangeBubbleBoost = new BooleanModelField("beanExchangeBubbleBoost", "安心豆兑换时光加速器", false));
    return modelFields;
  }

  @Override
  public void run() {
    try {
      Log.record(TAG, "执行开始-" + getName());

      if (memberSign.getValue()) {
        signinCalendar(); // 原会员签到
      }

      if (memberPointExchangeBenefit.getValue()) {
        // 这里写原来 memberPointExchangeBenefit 逻辑
        // 例如调用 AntMemberRpcCall.queryMemberPointTask() 或者你已有方法
      }

      if (collectSesame.getValue()) {
        // 原来的芝麻粒领取逻辑
      }

      if (promise.getValue()) {
        // 原来的生活记录逻辑
      }

      if (KuaiDiFuLiJia.getValue() || antInsurance.getValue()) {
        RecommendTask();
      }

      if (signinCalendar.getValue()) {
        signinCalendar(); // 消费金签到
      }

      if (enableGoldTicket.getValue()) {
        // 原黄金票签到逻辑
      }

      if (enableGameCenter.getValue()) {
        // 原游戏中心签到逻辑
      }

      if (merchantSignIn.getValue() || merchantKMDK.getValue()) {
        // 原商家服务签到/开门打卡逻辑
      }

      OrdinaryTask(); // 普通任务

    } catch (Throwable t) {
      Log.error(TAG, "AntMember run() error:");
      Log.printStackTrace(TAG, t);
    } finally {
      Log.record(TAG, "执行结束-" + getName());
    }
  }




  @Override
  public Boolean check() {
    if (TaskCommon.IS_ENERGY_TIME) {
      Log.other("任务暂停⏸️蚂蚁会员:当前为仅收能量时间");
    }
    return false;
  }

  // 我的快递任务
  private void RecommendTask() {
    try {
      // 调用 AntMemberRpcCall.queryRecommendTask() 获取 JSON 数据
      String response = AntMemberRpcCall.queryRecommendTask();
      JSONObject jsonResponse = new JSONObject(response);
      // 获取 taskDetailList 数组
      JSONArray taskDetailList = jsonResponse.getJSONArray("taskDetailList");
      // 遍历 taskDetailList
      for (int i = 0; i < taskDetailList.length(); i++) {
        JSONObject taskDetail = taskDetailList.getJSONObject(i);
        // 检查 "canAccess" 的值是否为 true
        boolean canAccess = taskDetail.optBoolean("canAccess", false);
        if (!canAccess) {
          // 如果 "canAccess" 不为 true，跳过
          continue;
        }
        // 获取 taskMaterial 对象
        JSONObject taskMaterial = taskDetail.optJSONObject("taskMaterial");
        // 获取 taskBaseInfo 对象
        JSONObject taskBaseInfo = taskDetail.optJSONObject("taskBaseInfo");
        // 获取 taskCode
        String taskCode = taskMaterial.optString("taskCode", "");
        // 根据 taskCode 执行不同的操作
        if ("WELFARE_PLUS_ANT_FOREST".equals(taskCode) || "WELFARE_PLUS_ANT_OCEAN".equals(taskCode)) {
          if ("WELFARE_PLUS_ANT_FOREST".equals(taskCode)) {
            //String forestHomePageResponse = AntMemberRpcCall.queryforestHomePage();
            //TimeUtil.sleep(2000);
            String forestTaskResponse = AntMemberRpcCall.forestTask();
            TimeUtil.sleep(500);
            String forestreceiveTaskAward = AntMemberRpcCall.forestreceiveTaskAward();
          } else if ("WELFARE_PLUS_ANT_OCEAN".equals(taskCode)) {
            //String oceanHomePageResponse = AntMemberRpcCall.queryoceanHomePage();
            //TimeUtil.sleep(2000);
            String oceanTaskResponse = AntMemberRpcCall.oceanTask();
            TimeUtil.sleep(500);
            String oceanreceiveTaskAward = AntMemberRpcCall.oceanreceiveTaskAward();
          }
          if (taskBaseInfo != null) {
            String appletName = taskBaseInfo.optString("appletName", "Unknown Applet");
            Log.other("我的快递💌完成[" + appletName + "]");
          }
        }
        if (taskMaterial == null || !taskMaterial.has("taskId")) {
          // 如果 taskMaterial 为 null 或者不包含 taskId，跳过
          continue;
        }
        // 获取 taskId
        String taskId = taskMaterial.getString("taskId");
        // 调用 trigger 方法
        String triggerResponse = AntMemberRpcCall.trigger(taskId);
        JSONObject triggerResult = new JSONObject(triggerResponse);
        // 检查 success 字段
        boolean success = triggerResult.getBoolean("success");
        if (success) {
          // 从 triggerResponse 中获取 prizeSendInfo 数组
          JSONArray prizeSendInfo = triggerResult.getJSONArray("prizeSendInfo");
          if (prizeSendInfo.length() > 0) {
            JSONObject prizeInfo = prizeSendInfo.getJSONObject(0);
            JSONObject extInfo = prizeInfo.getJSONObject("extInfo");
            // 获取 promoCampName
            String promoCampName = extInfo.optString("promoCampName", "Unknown Promo Campaign");
            // 输出日志信息
            Log.other("我的快递💌完成[" + promoCampName + "]");
          }
        }
      }
    } catch (Throwable th) {
      Log.error(TAG, "RecommendTask err:");
      Log.printStackTrace(TAG, th);
    }
  }

  private void OrdinaryTask() {
    try {
      // 调用 AntMemberRpcCall.queryOrdinaryTask() 获取 JSON 数据
      String response = AntMemberRpcCall.queryOrdinaryTask();
      JSONObject jsonResponse = new JSONObject(response);
      // 检查是否请求成功
      if (jsonResponse.getBoolean("success")) {
        // 获取任务详细列表
        JSONArray taskDetailList = jsonResponse.getJSONArray("taskDetailList");
        // 遍历任务详细列表
        for (int i = 0; i < taskDetailList.length(); i++) {
          // 获取当前任务对象
          JSONObject task = taskDetailList.getJSONObject(i);
          // 提取任务 ID、处理状态和触发类型
          String taskId = task.optString("taskId");
          String taskProcessStatus = task.optString("taskProcessStatus");
          String sendCampTriggerType = task.optString("sendCampTriggerType");
          // 检查任务状态和触发类型，执行触发操作
          if (!"RECEIVE_SUCCESS".equals(taskProcessStatus) && !"EVENT_TRIGGER".equals(sendCampTriggerType)) {
            // 调用 signuptrigger 方法
            String signuptriggerResponse = AntMemberRpcCall.signuptrigger(taskId);
            // 调用 sendtrigger 方法
            String sendtriggerResponse = AntMemberRpcCall.sendtrigger(taskId);
            // 解析 sendtriggerResponse
            JSONObject sendTriggerJson = new JSONObject(sendtriggerResponse);
            // 判断任务是否成功
            if (sendTriggerJson.getBoolean("success")) {
              // 从 sendtriggerResponse 中获取 prizeSendInfo 数组
              JSONArray prizeSendInfo = sendTriggerJson.getJSONArray("prizeSendInfo");
              // 获取 prizeName
              String prizeName = prizeSendInfo.getJSONObject(0).getString("prizeName");
              Log.other("我的快递💌完成[" + prizeName + "]");
            } else {
              Log.other(TAG, "sendtrigger failed for taskId: " + taskId);
            }
            TimeUtil.sleep(1000);
          }
        }
      }
    } catch (Throwable th) {
      Log.error(TAG, "OrdinaryTask err:");
      Log.printStackTrace(TAG, th);
    }
  }

  // 消费金签到
  private void signinCalendar() {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.signinCalendar());
      if (!MessageUtil.checkSuccess(TAG, jo)) {
        return;
      }
      boolean signed = jo.optBoolean("isSignInToday");
      if (!signed) {
        jo = new JSONObject(AntMemberRpcCall.openBoxAward());
        if (MessageUtil.checkSuccess(TAG, jo)) {
          int amount = jo.getInt("amount");
          int consecutiveSignInDays = jo.getInt("consecutiveSignInDays");
          Log.other("攒消费金💰签到[坚持" + consecutiveSignInDays + "天]#获得[" + amount + "消费金]");
        }
      }
    } catch (Throwable t) {
      Log.error(TAG, "signinCalendar err:");
      Log.printStackTrace(TAG, t);
    }
  }
  private void beanSignIn() {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.querySignInProcess("AP16242232", "INS_BLUE_BEAN_SIGN"));
      if (!jo.optBoolean("success")) {
        Log.runtime(jo.toString());
        return;
      }
      if (jo.getJSONObject("result").getBoolean("canPush")) {
        jo = new JSONObject(AntMemberRpcCall.signInTrigger("AP16242232", "INS_BLUE_BEAN_SIGN"));
        if (jo.optBoolean("success")) {
          String prizeName = jo.getJSONObject("result").getJSONArray("prizeSendOrderDTOList").getJSONObject(0).getString("prizeName");
          Log.record(TAG,"安心豆🫘[" + prizeName + "]");
        } else {
          Log.runtime(jo.toString());
        }
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "beanSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }
  private void beanExchangeBubbleBoost() {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.queryUserAccountInfo("INS_BLUE_BEAN"));
      if (!jo.optBoolean("success")) {
        Log.runtime(jo.toString());
        return;
      }
      int userCurrentPoint = jo.getJSONObject("result").getInt("userCurrentPoint");
      jo = new JSONObject(AntMemberRpcCall.beanExchangeDetail("IT20230214000700069722"));
      if (!jo.optBoolean("success")) {
        Log.runtime(jo.toString());
        return;
      }
      jo = jo.getJSONObject("result").getJSONObject("rspContext").getJSONObject("params").getJSONObject("exchangeDetail");
      String itemId = jo.getString("itemId");
      String itemName = jo.getString("itemName");
      jo = jo.getJSONObject("itemExchangeConsultDTO");
      int realConsumePointAmount = jo.getInt("realConsumePointAmount");
      if (!jo.getBoolean("canExchange") || realConsumePointAmount > userCurrentPoint) {
        return;
      }
      jo = new JSONObject(AntMemberRpcCall.beanExchange(itemId, realConsumePointAmount));
      if (jo.optBoolean("success")) {
        Log.record(TAG,"安心豆🫘[兑换:" + itemName + "]");
      } else {
        Log.runtime(jo.toString());
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "beanExchangeBubbleBoost err:");
      Log.printStackTrace(TAG, t);
    }
  }
}