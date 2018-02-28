package com.jflyfox.util.task.job;



import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Created by linzuk on 2018/2/26.
 */
@Service
public class SpiderJobCallableImpl implements SpiderJobCallable {

//    private ContentApiAct contentApiAct;
    @Override
    public boolean callback(String id, String title, List<String> pictures) {
        // 打印数据
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("title", title);
        data.put("pictures", pictures);
        System.out.println(JSON.toJSONString(data));


        // TODO: 将数据录入系统就好
        return false; // true: 之前已经录入过了
    }
}

