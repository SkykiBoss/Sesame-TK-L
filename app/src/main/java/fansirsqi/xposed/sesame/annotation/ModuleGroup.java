package fansirsqi.xposed.sesame.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 用于标记模块所属功能组字段（来自 BaseModel）
 * 比如：@ModuleGroup("enableMemberGroup")
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleGroup {
    String value();
}
