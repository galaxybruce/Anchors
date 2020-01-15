package com.effective.android.anchors;

import com.effective.android.anchors.register.IProjectTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bruce.zhang
 * @date 2020-01-15 14:52
 * @description (亲 ， 我是做什么的)
 * <p>
 * modification history:
 */
public class TaskFactory {

    private Map<String, Task> mCacheTask;
    private List<IProjectTask> mProjectTasks;
    private TaskCreator mTaskCreator;

    public TaskFactory() {
        this(null);
    }

    public TaskFactory(TaskCreator creator) {
        mTaskCreator = creator;
        mCacheTask = new HashMap<>();
        mProjectTasks = new ArrayList<>();
        loadTasks();
    }

    public synchronized Task getTask(String taskId) {
        Task task = mCacheTask.get(taskId);

        if (task != null) {
            return task;
        }
        if(mTaskCreator != null) {
            task = mTaskCreator.createTask(taskId);
        }

        if (task == null) {
            throw new IllegalArgumentException("Create task fail. Make sure TaskCreator can create a task with only taskId");
        }
        mCacheTask.put(taskId, task);
        return task;
    }

    private void loadTasks() {
        // register(new ProjectTaskDemo());
    }

    private void register(IProjectTask projectTask) {
        Map<String, Task> taskMap = projectTask.loadInto();
        if(taskMap != null && !taskMap.isEmpty()) {
            mCacheTask.putAll(projectTask.loadInto());
        }
        mProjectTasks.add(projectTask);
    }

    public List<Project> initProjects() {
        List<Project> projects = new ArrayList<>();
        for (IProjectTask projectTask : mProjectTasks) {
            projects.add(projectTask.buildProject(this));
        }
        mProjectTasks.clear();
        return projects;
    }
}
