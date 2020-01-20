# note: this project fork from [Anchors](https://github.com/YummyLau/Anchors)
#### bintray-release  [![](./assets/btn_apache_lisence.png)](LICENSE)

模块|Anchors|anchor-annotation|anchor-compiler
:---:|:---:|:---:|:---:
当前最新版本| [![Download](https://img.shields.io/badge/version-1.1.1-blue.svg)](https://bintray.com/galaxybruce/maven/anchors/_latestVersion)| [![Download](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://bintray.com/galaxybruce/maven/anchor-annotation/_latestVersion)|[![Download](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://bintray.com/galaxybruce/maven/anchor-compiler/_latestVersion)


#### 在Anchors基础上增加了如下功能
* 增加Process.java类，task支持在哪个进程中初始化
* 增加注解[anchor-compiler](./anchor-compiler)

#### Use
1. Add jcenter warehouse to project root

	```
	buildscript {
		repositories {
		  jcenter ()
		}
	   dependencies {
        classpath 'com.billy.android:cc-register:1.1.2'
        }
	}
	allprojects {
		repositories {
		jcenter ()
		}
	}
	```

2. Add dependencies under the module needs create init Task

	```
	implementation 'com.galaxybruce.android:anchors:latestversion'
	implementation 'com.galaxybruce.android:anchor-annotation:latestversion'
	implementation 'com.galaxybruce.android:anchor-compiler:latestversion'
	```
	
3. add [CC](https://github.com/luckybilly/CC) register info in ***app*** module
```
ccregister.registerInfo.add([
        //在自动注册组件的基础上增加：自动注册组件B的processor
        'scanInterface'             : 'com.effective.android.anchors.register.IProjectTask'
        , 'codeInsertToClassName'   : 'com.effective.android.anchors.TaskFactory'
        , 'codeInsertToMethodName'  : 'loadTasks'
        , 'registerMethodName'      : 'register'
])
```

4. create init Task class with annotation
```
  @TaskAnchor(depends = {TaskTest.TASK_10, TaskTest.TASK_20})
    public static class ASYNC_TASK_5 extends Task {

        public ASYNC_TASK_5() {
            super(TaskTest.ASYNC_TASK_5,true);
        }

        @Override
        protected void run(String name, Application application) {
            doJob(200);
        }
    }
```

5. Add a dependency graph in `Application`

	```
	final  TestTaskFactory testTaskFactory = new TestTaskFactory();

        Project.Builder builder1 = new Project.Builder(PROJECT_1, testTaskFactory);
        builder1.add(TASK_10);
        builder1.add(TASK_11).dependOn(TASK_10);
        builder1.add(TASK_12).dependOn(TASK_11);
        builder1.add(TASK_13).dependOn(TASK_12);
        Project project1 = builder1.build();

        Project.Builder builder2 = new Project.Builder(PROJECT_2, testTaskFactory);
        builder2.add(TASK_20);
        builder2.add(TASK_21).dependOn(TASK_20);
        builder2.add(TASK_22).dependOn(TASK_21);
        builder2.add(TASK_23).dependOn(TASK_22);
        Project project2 = builder2.build();


        Task UiTaskB = new TestTaskFactory.UITHREAD_TASK_B();
        Task UiTaskC = new TestTaskFactory.UITHREAD_TASK_C();

        AnchorsManager.instance()
                .setApplication(application)
                .debuggable(true)
                .addAnchors(TASK_23,"TASK_E","TASK_10")
                .start(testTaskFactory, project1, project2, UiTaskB, UiTaskC);
	```



