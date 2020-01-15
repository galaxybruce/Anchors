package com.galaxybruce.android.compiler.util;


public class Consts {
    public static final String KEY_MODULE_NAME = "moduleName";
    public static final String PROJECT = "Anchors";
    public static final String TAG = PROJECT + "::";
    public static final String SEPARATOR = "$$";
    public static final String PREFIX_OF_LOGGER = PROJECT + "::Compiler ";
    public static final String PACKAGE_OF_GENERATE_FILE = "com.galaxybruce.android.anchor";

    public static final String TASK_SUPER_CLASS = "com.effective.android.anchors.Task";
    public static final String PROJECT_CLASS = "com.effective.android.anchors.Project";
    public static final String TASK_FACTORY_CLASS = "com.effective.android.anchors.TaskFactory";
    public static final String PROJECT_TASK_INTERFACE = "com.effective.android.anchors.register.IProjectTask";


    public static final String NO_MODULE_NAME_TIPS = "These no module name, at 'build.gradle', like :\n" +
            "android {\n" +
            "    defaultConfig {\n" +
            "        ...\n" +
            "        javaCompileOptions {\n" +
            "            annotationProcessorOptions {\n" +
            "                arguments = [moduleName: project.getName()]\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

}