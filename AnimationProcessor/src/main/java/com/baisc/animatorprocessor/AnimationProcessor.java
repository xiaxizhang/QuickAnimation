package com.baisc.animatorprocessor;

import com.baisc.animationannotation.Alpha;
import com.baisc.animationannotation.AnimationParams;
import com.baisc.animationannotation.Interpolator;
import com.baisc.animationannotation.Animator;
import com.baisc.animationannotation.Animators;
import com.baisc.animationannotation.ResourceAnimation;
import com.baisc.animationannotation.Rotate;
import com.baisc.animationannotation.Scale;
import com.baisc.animationannotation.Translate;
import com.baisc.animationannotation.TypeEvaluator;
import com.baisc.animatorprocessor.core.AnnotationConverter;
import com.baisc.animatorprocessor.core.GenerateCode;
import com.baisc.animatorprocessor.core.GenerateProxy;
import com.baisc.animatorprocessor.core.JavaPoetGenerator;
import com.google.auto.service.AutoService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;

/**
 * 动画注解器.
 */

@AutoService(Processor.class)
public class AnimationProcessor extends AbstractProcessor {
    private ProcessingEnvironment mEnvironment;
    private Messager mMessager;

    private Map<String, Map<String, List<Annotation>>> mAnnotationMap = new HashMap<>();

    //用于过滤已经遍历过的一个元素.
    private Map<String, Element> filterElements = new HashMap<>();

    private GenerateCode mGenerator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mEnvironment = processingEnvironment;
        mMessager = mEnvironment.getMessager();
        mGenerator = new GenerateProxy(new JavaPoetGenerator());
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set == null || set.isEmpty()) {
            return false;
        }
        for (TypeElement annotationsType : set) {
            Set<Element> alphaElements = (Set<Element>) roundEnvironment.getElementsAnnotatedWith(annotationsType);
            if (alphaElements != null && !alphaElements.isEmpty()) {
                for (Element element : alphaElements) {
                    if (element.getKind() == ElementKind.FIELD) {
                        VariableElement variableElement = (VariableElement) element;
                        TypeElement parent = (TypeElement) variableElement.getEnclosingElement();
                        //获取到一个类里面需要保持的变量的注解
                        String clazz = parent.getQualifiedName().toString();
                        String variableName = variableElement.getSimpleName().toString();
                        if (clazz.startsWith("android") || clazz.startsWith("java")){
                            continue;
                        }
                        if (filterElements.containsKey(clazz + variableName)) {
                            continue;
                        }
                        findVariableAnnotationsByClass(clazz);
                        //获取到当前指定变量所保存的注解
                        List<Annotation> annotations = findAnnotations(clazz, variableName);
                        List<Annotation> annotation = AnnotationConverter.convert(mEnvironment.getMessager(), variableElement);
                        annotations.addAll(annotation);
                        filterElements.put(clazz + variableName, element);
                    }
                }
            }
        }
        print();

        if (mAnnotationMap != null && !mAnnotationMap.isEmpty()){
            Iterator<Entry<String, Map<String, List<Annotation>>>> iterator = mAnnotationMap.entrySet().iterator();
            while (iterator.hasNext()){
                Entry<String, Map<String, List<Annotation>>> entry = iterator.next();
                mGenerator.generate(entry.getKey(), entry.getValue(), mEnvironment);
            }
        }
        return true;
    }

    private void print(){
       Iterator<Entry<String, Map<String, List<Annotation>>>>   iterator  = mAnnotationMap
               .entrySet().iterator();
       while (iterator.hasNext()){
           Entry<String, Map<String, List<Annotation>>> entry = iterator.next();
           printMessage("class: " + entry.getKey());
           Map<String, List<Annotation>> values = entry.getValue();
           Iterator<Entry<String, List<Annotation>>> views = values.entrySet().iterator();
           while (views.hasNext()){
               Entry<String, List<Annotation>> entry1 = views.next();
               printMessage("view: " + entry1.getKey());
               List<Annotation> annotations = entry1.getValue();
               for (Annotation annotation : annotations){
                   if (annotation != null){
                       printMessage("ann: " + annotation.mAnnotationClass);
                       if (annotation instanceof com.baisc.animatorprocessor.Alpha){
                            printMessage("alpha: " + ((com.baisc.animatorprocessor.Alpha)
                                    annotation).fromAlpha);
                       }

                       if (annotation.mEvaluatorClass != null){
                           printMessage(annotation.mEvaluatorClass);
                       }

                       if (annotation.mInterpolatorClass != null){
                           printMessage(annotation.mInterpolatorClass);
                       }

                   }
               }
           }
           printMessage("--------------------------");
       }
    }

    //获取到指定类的所有变量的注解集。
    private Map<String, List<Annotation>> findVariableAnnotationsByClass(String clazz) {
        Map<String, List<Annotation>> clazzMap = mAnnotationMap.get(clazz);
        if (clazzMap == null) {
            clazzMap = new HashMap<>();
            mAnnotationMap.put(clazz, clazzMap);
        }
        return clazzMap;
    }

    private List<Annotation> findAnnotations(String clazz, String variable) {
        Map<String, List<Annotation>> map = findVariableAnnotationsByClass(clazz);
        List<Annotation> annotations = map.get(variable);
        if (annotations == null) {
            annotations = new ArrayList<>();
            map.put(variable, annotations);
        }
        return annotations;

    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotationTypes = new HashSet<>();
        annotationTypes.add(Alpha.class.getCanonicalName());
        annotationTypes.add(AnimationParams.class.getCanonicalName());
        annotationTypes.add(Interpolator.class.getCanonicalName());
        annotationTypes.add(Animator.class.getCanonicalName());
        annotationTypes.add(ResourceAnimation.class.getCanonicalName());
        annotationTypes.add(Rotate.class.getCanonicalName());
        annotationTypes.add(Scale.class.getCanonicalName());
        annotationTypes.add(Translate.class.getCanonicalName());
        annotationTypes.add(TypeEvaluator.class.getCanonicalName());
        annotationTypes.add(Animators.class.getCanonicalName());
        return annotationTypes;

    }

    @Override
    public SourceVersion getSupportedSourceVersion() {

        return mEnvironment.getSourceVersion();
    }

    private void printMessage(String message) {
        mMessager.printMessage(Kind.NOTE, message);
    }
}
