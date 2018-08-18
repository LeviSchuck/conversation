package com.levischuck.conversation.processor;

import com.google.auto.service.AutoService;
import com.levischuck.conversation.annotations.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.util.*;

@SupportedAnnotationTypes({
        "com.levischuck.conversation.annotations.DialogGen",
        "com.levischuck.conversation.annotations.RootDialog"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class DialogProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Map<String, DialogGroup> groups = new TreeMap<>();
        boolean fatal = false;
        try {
            Collection<? extends Element> roots = roundEnvironment.getElementsAnnotatedWith(RootDialog.class);
            Collection<? extends Element> dialogs = roundEnvironment.getElementsAnnotatedWith(DialogGen.class);

            for (Element rootAnnotated : roots) {
                RootDialog rootDialog = rootAnnotated.getAnnotation(RootDialog.class);
                if (rootDialog == null) {
                    System.err.println("RootDialog annotation broke: " + rootAnnotated);
                    fatal = true;
                    continue;
                }
                DialogGen dialogGen = rootAnnotated.getAnnotation(DialogGen.class);
                if (dialogGen == null) {
                    System.err.println("Root annotated dialog must also have a !DialogGen annotation: " + rootAnnotated);
                    fatal = true;
                    continue;
                }
                String group = rootDialog.value();
                if (groups.containsKey(group)) {
                    System.err.println("Group '" + group + "' already has root dialog: " + groups.get(group));
                } else {
                    groups.put(group, new DialogGroup(group, rootAnnotated));
                }
            }

            if (fatal) {
                System.err.println("Ending annotation processor early");
                return true;
            }

            for (Element dialogAnnotated : dialogs) {
                DialogGen dialogGen = dialogAnnotated.getAnnotation(DialogGen.class);
                if (dialogGen == null) {
                    System.err.println("DialogGen annotation broke: " + dialogAnnotated);
                    fatal = true;
                    continue;
                }
                String group = dialogGen.group();
                String rootStep = dialogGen.root();
                boolean foundRoot = false;

                DialogGroup dialogGroup = groups.get(group);
                if (dialogGroup == null) {
                    System.err.println("Dialog group " + group + " does not have a root: " + dialogAnnotated);
                    fatal = true;
                    continue;
                }
                DialogDescription dialogDescription = new DialogDescription(dialogAnnotated, dialogGen);
                if (dialogGroup.dialogs.containsKey(dialogAnnotated.asType())) {
                    System.err.println("Multiple DialogGen annotations on a single class is not supported");
                    continue;
                }

                for (Element dialogMethod : dialogAnnotated.getEnclosedElements()) {
                    DialogStep dialogStep = dialogMethod.getAnnotation(DialogStep.class);
                    if (dialogStep == null) {
                        continue;
                    }
                    if (dialogMethod.getKind() == ElementKind.METHOD) {
                        ExecutableElement executableStepMethod = (ExecutableElement)dialogMethod;
                        String methodName = executableStepMethod.getSimpleName().toString();
                        StepDescription stepDescription = new StepDescription(methodName, executableStepMethod, dialogStep);

                        // TODO more validation

                        if (dialogDescription.stepMethods.containsKey(methodName)) {
                            System.err.println("Cannot have multiple with the same name, " + dialogAnnotated + "::" + methodName);
                            fatal = true;
                            continue;
                        }

                        boolean fatalStep = false;
                        int index = 0;
                        for (VariableElement param : executableStepMethod.getParameters()) {
                            int thisIndex = index;
                            index++;
                            StepContext stepContext = param.getAnnotation(StepContext.class);
                            StepMessage stepMessage = param.getAnnotation(StepMessage.class);
                            if (stepContext == null && stepMessage == null) {
                                System.err.print("Parameter " + dialogAnnotated + "::" + methodName + "[" + param + "]");
                                System.err.println(" cannot be injected as it does not have StepContext or StepMessage");
                                fatalStep = true;
                                continue;
                            }
                            if (stepContext != null && stepMessage != null) {
                                System.err.print("Parameter " + dialogAnnotated + "::" + methodName + "[" + param + "]");
                                System.err.println(" cannot be both StepContext and StepMessage");
                                fatalStep = true;
                                continue;
                            }

                            if (stepContext != null) {
                                stepDescription.contextIndex = thisIndex;
                                stepDescription.contextElement = param;
                                stepDescription.contextType = param.asType();

                                if (dialogGroup.context == null) {
                                    dialogGroup.context = param.asType();
                                } else if (!dialogGroup.context.equals(param.asType())) {
                                    System.err.print("Context parameter " + dialogAnnotated + "::" + methodName + "[" + param + "]");
                                    System.err.print(" has type " + param.asType() + " but the dialog already has a type of ");
                                    System.err.println(dialogGroup.context);
                                    fatalStep = true;
                                }
                            }

                            if (stepMessage != null) {
                                stepDescription.messageIndex = thisIndex;
                                stepDescription.messageElement = param;
                                stepDescription.messageType = param.asType();

                                if (dialogGroup.message == null) {
                                    dialogGroup.message = param.asType();
                                } else if (!dialogGroup.message.equals(param.asType())) {
                                    System.err.print("Message parameter " + dialogAnnotated + "::" + methodName + "[" + param + "]");
                                    System.err.print(" has type " + param.asType() + " but the dialog already has a type of ");
                                    System.err.println(dialogGroup.context);
                                    fatalStep = true;
                                }
                            }
                        }

                        // TODO validate the return type is not void--unless it has a default next step
                        // TODO validate return type is GenResult and specifies the context type

                        if (fatalStep) {
                            fatal = true;
                            continue;
                        }

                        if (methodName.equals(rootStep)) {
                            foundRoot = true;
                        }

                        dialogDescription.stepMethods.put(methodName, stepDescription);
                    }
                }

                if (!foundRoot) {
                    System.err.println("Dialog " + dialogAnnotated + " root step " + rootStep + " was not found");
                    fatal = true;
                    continue;
                }

                dialogGroup.dialogs.put(dialogAnnotated.asType(), dialogDescription);
            }
            if (fatal) {
                System.err.println("Ending annotation processor early");
                return true;
            }

            for (DialogGroup group : groups.values()) {
                System.out.println("--------------------------------------------");
                System.out.println("Group: " + group.groupName);
                System.out.println("Root: " + group.rootElement);
                System.out.println("Context Type: " + group.context);
                System.out.println("Message Type:" + group.message);
                System.out.println("Dialogs: ");
                for (DialogDescription dialog : group.dialogs.values()) {
                    System.out.println("\tClass: " + dialog.element);
                    System.out.println("\tRoot: " + dialog.annotation.root());
                    System.out.println("\tSteps:");
                    for (StepDescription step : dialog.stepMethods.values()) {
                        System.out.println("\t\tName: " + step.name);

                        if (step.contextIndex >= 0) {
                            System.out.println("\t\tContext Index: " + step.contextIndex);
                        }

                        if (step.messageIndex >= 0) {
                            System.out.println("\t\tMessage Index: " + step.messageIndex);
                        }

                        try {
                            if (step.annotation.dialog() != Object.class) {
                                System.out.println("\t\tDefault Next Dialog: " + step.annotation.dialog());
                            }
                        } catch (MirroredTypeException m) {
                            // This is how to actually get the type in preprocessing
                            if (!m.getTypeMirror().toString().equals(Object.class.getCanonicalName())) {
                                System.out.println("\t\tDefault Next Dialog: " + m.getTypeMirror());
                            }
                        }

                        if (step.annotation.step().length() > 0) {
                            System.out.println("\t\tDefault Next Step: " + step.annotation.step());
                        }

                        System.out.println();
                    }
                    System.out.println();
                }
            }
            System.out.println("--------------------------------------------");

            // TODO validate all the defaults

            // TODO write the codes

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }

        return false;
    }

    private static class DialogGroup {
        final String groupName;
        final Element rootElement;
        final Map<TypeMirror, DialogDescription> dialogs;
        TypeMirror context;
        TypeMirror message;

        DialogGroup(String groupName, Element rootElement) {
            this.groupName = groupName;
            this.rootElement = rootElement;
            this.dialogs = new HashMap<>();
        }

        @Override
        public String toString() {
            return "DialogGroup{" +
                    "rootElement=" + rootElement +
                    '}';
        }
    }

    private static class DialogDescription {
        final Element element;
        final DialogGen annotation;
        final Map<String, StepDescription> stepMethods;

        DialogDescription(Element element, DialogGen annotation) {
            this.element = element;
            this.annotation = annotation;
            this.stepMethods = new TreeMap<>();
        }
    }

    private static class StepDescription {
        final String name;
        final ExecutableElement element;
        final DialogStep annotation;
        int contextIndex = -1;
        VariableElement contextElement;
        TypeMirror contextType;
        int messageIndex = -1;
        VariableElement messageElement;
        TypeMirror messageType;

        StepDescription(String name, ExecutableElement element, DialogStep annotation) {
            this.name = name;
            this.element = element;
            this.annotation = annotation;
        }
    }
}
