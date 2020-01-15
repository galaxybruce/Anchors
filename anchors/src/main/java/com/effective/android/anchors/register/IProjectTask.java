package com.effective.android.anchors.register;

import com.effective.android.anchors.Project;
import com.effective.android.anchors.Task;
import com.effective.android.anchors.TaskFactory;

import java.util.Map;

/**
 * @author bruce.zhang
 * @date 2020-01-14 17:36
 * @description 按module组织task，注解生成子类，插件收集子类
 * <p>
 * modification history:
 */
public interface IProjectTask {

    Map<String, Task> loadInto();

    Project buildProject(TaskFactory taskFactory);
}
