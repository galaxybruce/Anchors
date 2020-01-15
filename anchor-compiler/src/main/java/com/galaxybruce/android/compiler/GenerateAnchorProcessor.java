package com.galaxybruce.android.compiler;

import com.galaxybruce.android.anchors.annotation.TaskAnchor;
import com.galaxybruce.android.compiler.util.Consts;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static javax.lang.model.element.Modifier.PUBLIC;


public class GenerateAnchorProcessor extends BaseProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> annotationTypes = new LinkedHashSet<>();
        annotationTypes.add(TaskAnchor.class.getCanonicalName());
        return annotationTypes;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (CollectionUtils.isEmpty(annotations)) {
            return false;
        }

        Set<? extends Element> anchorElements = roundEnv.getElementsAnnotatedWith(TaskAnchor.class);
        try {
            logger.info(">>> Found anchors, start... <<<");
            this.parseAnnotations(anchorElements);
            logger.info(">>> Found anchors, end... <<<");
        } catch (Exception e) {
            logger.error(e);
        }
        return true;
    }

    private void parseAnnotations(Set<? extends Element> anchorElements) throws IOException {
        if (CollectionUtils.isEmpty(anchorElements)) {
            return;
        }

        logger.info("anchors size: " + anchorElements.size());

        // loadInto method return type
        final ParameterizedTypeName loadIntoReturnTypeName = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                getTypeName(Consts.TASK_SUPER_CLASS)
        );

        // loadInto method
        final MethodSpec.Builder loadIntoMethodBuilder = MethodSpec.methodBuilder("loadInto")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(loadIntoReturnTypeName);

        // buildProject input param
        ParameterSpec buildProjectParamSpec = ParameterSpec.builder(getTypeName(Consts.TASK_FACTORY_CLASS), "taskFactory").build();

        // buildProject method
        MethodSpec.Builder buildProjectMethodBuilder = MethodSpec.methodBuilder("buildProject")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(buildProjectParamSpec)
                .returns(getTypeName(Consts.PROJECT_CLASS));

        // build method content start...
        loadIntoMethodBuilder.addStatement("$T<String, Task> taskMap = new $T<>()", Map.class, HashMap.class);
        buildProjectMethodBuilder.addStatement("$T.Builder builder = new $T.Builder($S, taskFactory)",
                getTypeName(Consts.PROJECT_CLASS), getTypeName(Consts.PROJECT_CLASS), moduleName);

        for (Element element : anchorElements) {
            if (!validateChildAnnotatedElement(element, TaskAnchor.class.getSimpleName())) {
                continue;
            }
            TaskAnchor taskAnchor = element.getAnnotation(TaskAnchor.class);

            // loadInto method content
            loadIntoMethodBuilder.addStatement("taskMap.put($S, new $T())",
                    element.getSimpleName(),
                    element);

            // buildProject method content
            buildProjectMethodBuilder.addCode("\n");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("builder.add(\"" + element.getSimpleName() + "\")");
            String[] depends = taskAnchor.depends();
            if(depends.length > 0) {
                for (String depend : depends) {
                    if(StringUtils.isNotEmpty(depend)) {
                        stringBuilder.append(".dependOn(\"" + depend + "\")");
                    }
                }
            }
            buildProjectMethodBuilder.addStatement(stringBuilder.toString());
        }

        loadIntoMethodBuilder.addStatement("return taskMap");
        buildProjectMethodBuilder.addStatement("return builder.build()");

        // build method content end...

        String fileName = Consts.PROJECT + Consts.SEPARATOR + moduleName;
        TypeSpec type = TypeSpec.classBuilder(fileName)
                .addJavadoc(mClassJavaDoc)
//                .superclass(getChildTableSuperClassTypeName())
                .addSuperinterface(getTypeName(Consts.PROJECT_TASK_INTERFACE))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(loadIntoMethodBuilder.build())
                .addMethod(buildProjectMethodBuilder.build())
                .build();

        generateClass(Consts.PACKAGE_OF_GENERATE_FILE, type);
    }

    @Override
    protected boolean validateChildAnnotatedElement(Element annotatedElement, String annotationSimpleName) {
        return validateClassImplements(annotatedElement, Consts.TASK_SUPER_CLASS, annotationSimpleName);
    }


}