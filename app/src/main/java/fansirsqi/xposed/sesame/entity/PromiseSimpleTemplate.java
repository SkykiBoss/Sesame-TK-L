package fansirsqi.xposed.sesame.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fansirsqi.xposed.sesame.util.PromiseSimpleTemplateIdMap;

public class PromiseSimpleTemplate extends MapperEntity {
    public PromiseSimpleTemplate(String i, String n) {
        this.id = i;
        this.name = n;
    }

    public static List<PromiseSimpleTemplate> getList() {
        List<PromiseSimpleTemplate> list = new ArrayList<>();
        Set<Map.Entry<String, String>> idSet = PromiseSimpleTemplateIdMap.getMap().entrySet();
        for (Map.Entry<String, String> entry: idSet) {
            list.add(new PromiseSimpleTemplate(entry.getKey(), entry.getValue()));
        }
        return list;
    }
}