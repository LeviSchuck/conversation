package com.levischuck.conversation.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes({
        "com.levischuck.conversation.annotations.DialogGen",
        "com.levischuck.conversation.annotations.RootDialog"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class DialogProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        for (TypeElement typeElement : annotations) {
            for (Element element : roundEnvironment.getElementsAnnotatedWith(typeElement)) {
                System.out.println("TODO " + typeElement + " - " + element);
            }
        }
        return false;
    }
}
