### Anchors

<img src="https://raw.githubusercontent.com/YummyLau/hexo/master/source/pics/anchors/anchor_logo.png" width = "1300" height = "150" alt="图片名称" align=center />

![](https://travis-ci.org/YummyLau/Anchors.svg?branch=master)
![Language](https://img.shields.io/badge/language-java-orange.svg)
![Version](https://img.shields.io/badge/version-1.0.4-blue.svg)

README: [English](https://github.com/YummyLau/Anchors/blob/master/README.md) | [中文](https://github.com/YummyLau/Anchors/blob/master/README-zh.md)

#### 版本更新
* 1.0.2（2019/06/14） 新增支持直接打开 project 节点
* 1.0.3（2019/12/11） 新增支持节点等待功能
* 1.0.4（2019/12/31） 优化线上反馈多线程同步通知下一节点启动的问题


#### 简介

`Anchors` 是一个基于图结构，支持同异步依赖任务初始化 Android 启动框架。其锚点提供 "勾住" 依赖的功能，能灵活解决初始化过程中复杂的同步问题。参考 `alpha` 并改进其部分细节, 更贴合 Android 启动的场景, 同时支持优化依赖初始化流程, 选择较优的路径进行初始化。

关于 `alpha` 的思考，请查看 [关于Android异步启动框架alpha的思考](http://yummylau.com/2019/03/15/%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90%E4%B9%8B%20alpha/)

较 `alpha` 的优势

* 支持配置 anchors 等待任务链，常用于 application#onCreate 前保证某些初始化任务完成之后再进入 activity 生命周期回调。
* 支持主动请求阻塞等待任务，常用于任务链上的某些初始化任务需要用户逻辑确认。
* 支持同异步任务链

#### 使用需知

> 1. Anchors的设计是为了 app 启动时候做复杂的初始化工作能高效便捷完成，而不是用于业务逻辑中用于初始化某些依赖。
> 2. api 中设置 anchor 会阻塞等待直到 anchor 完成之后才继续走 AnchorsManager#start 后的代码块，application 中 之所以能这么处理是因为没有频繁的 ui 操作。 anchor 之后的任务会在自主由框架调度，同步任务会通过 handler#post 发送到主线程排队处理，异步任务会通过框架内线程池驱动。
> 3. 等待功能在不设置 anchor 的场景下使用。如果设置了 anchor ，则等待任务应该是后置于 anchor 避免 uiThead 阻塞。
> 4. 同异步混合链及 anchor 功能的结合使用，可以灵活处理很多复杂初始化场景，但是要充分理解使用功能时的线程背景。

#### 使用方法
1. 在项目根目路添加 jcenter 仓库

	```
	buildscript {
	    repositories {
	        jcenter()  
	    }
	}
	allprojects {
	    repositories {
	        jcenter()
	    }   
	}
	```

2. 在 **app** 模块下添加依赖

	```
	implementation 'com.effective.android:anchors:1.0.4'
	```

2. 在 `Application` 中添加依赖图

	```
	//anchor 设置
	AnchorsManager.getInstance().debuggable(true)
	        .addAnchors(anchorYouNeed)
	        .start(dependencyGraphHead);
	        
	//or 等待场景 设置
	AnchorsManager anchorsManager = AnchorsManager.getInstance();
   anchorsManager.debuggable(true);
   LockableAnchor lockableAnchor = anchorsManager.requestBlockWhenFinish(waitTaskYouNeed);
   lockableAnchor.setLockListener(...){
   	    //lockableAnchor.unlock 解除等待，继续任务链
   	    //lockableAnchor.smash 破坏等待，终止任务链
   }
   anchorsManager.start(dependencyGraphHead);
  
	```

	其中 *anchorYouNeed* 为你所需要添加的锚点, *waitTaskYouNeed* 为你所需要等待的任务,*dependencyGraphHead* 为依赖图的头部.

#### Sample

代码逻辑请参考 **app** 模块下的 sample。

**debuggale** 模式下能打印不同维度的 log 作为调试信息输出，同时针对每个依赖任务做 `Trace` 追踪, 可以通过 *python systrace.py* 来输出 **trace.html** 进行性能分析。

`Anchors` 定义不同的 **TAG** 用于过滤 log, 需要打开 Debug 模式。

* `Anchors`, 最基础的 TAG
* `TASK_DETAIL`, 过滤依赖任务的详情

	```
	2019-03-18 14:19:45.687 22493-22493/com.effective.android.sample D/TASK_DETAIL: TASK_DETAIL
	======================= task (UITHREAD_TASK_A ) =======================
	| 依赖任务 :
	| 是否是锚点任务 : false
	| 线程信息 : main
	| 开始时刻 : 1552889985401 ms
	| 等待运行耗时 : 85 ms
	| 运行任务耗时 : 200 ms
	| 结束时刻 : 1552889985686
	==============================================
	```
* `ANCHOR_DETAIL`, 过滤输出锚点任务信息

	```
	2019-03-18 14:42:33.354 24719-24719/com.effective.android.sample W/ANCHOR_DETAIL: anchor "TASK_100" no found !
	2019-03-18 14:42:33.354 24719-24719/com.effective.android.sample W/ANCHOR_DETAIL: anchor "TASK_E" no found !
	2019-03-18 14:42:33.355 24719-24719/com.effective.android.sample D/ANCHOR_DETAIL: has some anchors！( "TASK_93" )
	2019-03-18 14:42:34.188 24719-24746/com.effective.android.sample D/ANCHOR_DETAIL: TASK_DETAIL
    ======================= task (TASK_93 ) =======================
    | 依赖任务 : TASK_92
    | 是否是锚点任务 : true
    | 线程信息 : Anchors Thread #7
    | 开始时刻 : 1552891353984 ms
    | 等待运行耗时 : 4 ms
    | 运行任务耗时 : 200 ms
    | 结束时刻 : 1552891354188
    ==============================================
	2019-03-18 14:42:34.194 24719-24719/com.effective.android.sample D/ANCHOR_DETAIL: All anchors were released！
	```
	
* `LOCK_DETAIL`, 过滤输出等待信息

	```
	2019-12-11 14:53:11.784 6183-6437/com.effective.android.sample D/LOCK_DETAIL: Anchors Thread #9- lock( TASK_10 )
	2019-12-11 14:53:13.229 6183-6183/com.effective.android.sample D/LOCK_DETAIL: main- unlock( TASK_10 )
	2019-12-11 14:53:13.229 6183-6183/com.effective.android.sample D/LOCK_DETAIL: Continue the task chain...
	
	```

* `DEPENDENCE_DETAIL`, 过滤依赖图信息

	```
	2019-03-18 14:27:53.724 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_9_start(1552890473721) --> TASK_90 --> TASK_91 --> PROJECT_9_end(1552890473721)
	2019-03-18 14:27:53.724 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_9_start(1552890473721) --> TASK_90 --> TASK_92 --> TASK_93 --> PROJECT_9_end(1552890473721)
	2019-03-18 14:27:53.724 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_8_start(1552890473721) --> TASK_80 --> TASK_81 --> PROJECT_8_end(1552890473721)
	2019-03-18 14:27:53.724 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_8_start(1552890473721) --> TASK_80 --> TASK_82 --> TASK_83 --> PROJECT_8_end(1552890473721)
	2019-03-18 14:27:53.725 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_7_start(1552890473720) --> TASK_70 --> TASK_71 --> PROJECT_7_end(1552890473720)
	2019-03-18 14:27:53.725 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_7_start(1552890473720) --> TASK_70 --> TASK_72 --> TASK_73 --> PROJECT_7_end(1552890473720)
	2019-03-18 14:27:53.725 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_6_start(1552890473720) --> TASK_60 --> TASK_61 --> PROJECT_6_end(1552890473720)
	2019-03-18 14:27:53.725 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_6_start(1552890473720) --> TASK_60 --> TASK_62 --> TASK_63 --> PROJECT_6_end(1552890473720)
	2019-03-18 14:27:53.725 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_5_start(1552890473720) --> TASK_50 --> TASK_51 --> PROJECT_5_end(1552890473720)
	2019-03-18 14:27:53.725 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_5_start(1552890473720) --> TASK_50 --> TASK_52 --> TASK_53 --> PROJECT_5_end(1552890473720)
	2019-03-18 14:27:53.725 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_4_start(1552890473720) --> TASK_40 --> TASK_41 --> TASK_42 --> TASK_43 --> PROJECT_4_end(1552890473720)
	2019-03-18 14:27:53.726 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_3_start(1552890473720) --> TASK_30 --> TASK_31 --> TASK_32 --> TASK_33 --> PROJECT_3_end(1552890473720)
	2019-03-18 14:27:53.726 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_2_start(1552890473719) --> TASK_20 --> TASK_21 --> TASK_22 --> TASK_23 --> PROJECT_2_end(1552890473719)
	2019-03-18 14:27:53.726 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> PROJECT_1_start(1552890473719) --> TASK_10 --> TASK_11 --> TASK_12 --> TASK_13 --> PROJECT_1_end(1552890473719)
	2019-03-18 14:27:53.726 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> UITHREAD_TASK_B
	2019-03-18 14:27:53.726 22843-22843/com.effective.android.sample D/DEPENDENCE_DETAIL: UITHREAD_TASK_A --> UITHREAD_TASK_C
	```

下面是没有使用锚点和使用锚点场景下, **Trace** 给出的执行时间

<img src="https://raw.githubusercontent.com/YummyLau/hexo/master/source/pics/anchors/anchor_1.png" width = "1860" height = "400" alt="图片名称" align=center />

依赖图中有着一条 `UITHREAD_TASK_A -> TASK_90 -> TASK_92 -> Task_93`依赖。假设我们的这条依赖路径是后续业务的前置条件,则我们需要等待该业务完成之后再进行自身的业务代码。如果不是则我们不关系他们的结束时机。在使用锚点功能时，我们勾住 `TASK_93`，则从始端到该锚点的优先级将被提升。从上图可以看到执行该依赖链的时间缩短了。

> 依赖图用于解决任务执行时任务间的依赖关系，而锚点设置则是用于解决执行依赖与代码调用点之间的同步关系。


#### 期望
编写该项目只是希望能提高日常开发的效率，专注于处理业务 。如果更好的做法或者意见建议，欢迎写信到 yummyl.lau@gmail.com, 提出 **Issues** 或发起 **Pull requests** , 任何问题都会第一时间得到处理解决。
