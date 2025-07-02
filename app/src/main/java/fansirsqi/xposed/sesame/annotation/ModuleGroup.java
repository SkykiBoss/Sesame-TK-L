// fansirsqi.xposed.sesame.model.annotation.ModuleGroupSwitch.java
package fansirsqi.xposed.sesame.model.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleGroupSwitch {
    String value(); // BaseModel 中的字段名
}
