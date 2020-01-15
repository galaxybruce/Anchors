package com.galaxybruce.android.compiler;

import com.galaxybruce.android.compiler.util.Consts;
import com.galaxybruce.android.compiler.util.Logger;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;


public abstract class BaseProcessor extends AbstractProcessor {
    Filer mFiler;
    Logger logger;
    Types types;
    Elements elementUtils;
//    TypeUtils typeUtils;
    // Module name, maybe its 'app' or others
    String moduleName = null;
    String mClassJavaDoc;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mClassJavaDoc = "Generated by " + this.getClass().getSimpleName() + ". Do not edit it!\n";

        mFiler = processingEnv.getFiler();
        types = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
//        typeUtils = new TypeUtils(types, elementUtils);
        logger = new Logger(processingEnv.getMessager());

        // Attempt to get user configuration [moduleName]
        Map<String, String> options = processingEnv.getOptions();
        if (MapUtils.isNotEmpty(options)) {
            moduleName = options.get(Consts.KEY_MODULE_NAME);
        }

        if (StringUtils.isNotEmpty(moduleName)) {
            moduleName = moduleName.replaceAll("[^0-9a-zA-Z_]+", "");
            logger.info("The user has configuration the module name, it was [" + moduleName + "]");
        } else {
            logger.error(Consts.NO_MODULE_NAME_TIPS);
            throw new RuntimeException("ARouter::Compiler >>> No module name, for more information, look at gradle log.");
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions() {
        return new HashSet<String>() {{
            this.add(Consts.KEY_MODULE_NAME);
        }};
    }

    protected TypeName getTypeName(String canonicalName) {
        return ClassName.get(elementUtils.getTypeElement(canonicalName));
    }

    protected void generateClass(String packageName, TypeSpec typeSpec) {
        try {
            JavaFile.builder(packageName, typeSpec).build().writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 校验注解类是否正确用在某个类上
     */
    protected abstract boolean validateChildAnnotatedElement(Element annotatedElement, String annotationSimpleName);

    protected boolean validateClassImplements(Element element, String interfaceCanonicalName, String annotationSimpleName) {
        boolean valid = element.getKind().isClass() &&
                        types.isAssignable(element.asType(), elementUtils.getTypeElement(interfaceCanonicalName).asType());
        if (valid) {
            return true;
        } else {
            logger.error(String.format("%s注解的类%s必须派生于「%s」", annotationSimpleName, element.getSimpleName(), interfaceCanonicalName));
            return false;
        }
    }
}
