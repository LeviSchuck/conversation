package com.levischuck.conversation.processor;

import com.google.auto.service.AutoService;
import com.levischuck.conversation.annotations.*;
import com.levischuck.conversation.core.*;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Type;
import java.util.*;

@SupportedAnnotationTypes({
        "com.levischuck.conversation.annotations.DialogGen",
        "com.levischuck.conversation.annotations.RootDialog"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class DialogProcessor extends AbstractProcessor {
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
    }

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
                String group = dialogGen.group();
                if (groups.containsKey(group)) {
                    System.err.println("Group '" + group + "' already has root dialog: " + groups.get(group));
                } else {
                    groups.put(group, new DialogGroup(group, rootAnnotated, rootDialog));
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
                if (dialogGroup.dialogs.containsKey(dialogAnnotated.asType().toString())) {
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
                        TypeMirror returnType = executableStepMethod.getReturnType();
                        StepDescription stepDescription = new StepDescription(methodName, executableStepMethod, dialogStep, returnType);

                        // TODO more validation

                        if (dialogDescription.stepMethods.containsKey(methodName)) {
                            System.err.println("Cannot have multiple with the same name, " + dialogAnnotated + "::" + methodName);
                            fatal = true;
                            continue;
                        }

                        if (TypeKind.DECLARED == returnType.getKind()) {
                            DeclaredType declaredReturnType = (DeclaredType)returnType;
                            TypeElement declaredReturnTypeElement = (TypeElement)declaredReturnType.asElement();
                            if (!declaredReturnTypeElement.getQualifiedName().toString().equals(GenResult.class.getTypeName())) {
                                System.err.print("DialogStep " + dialogAnnotated + "::" + methodName);
                                System.err.println(" has an unsupported return type: " + returnType);
                                fatal = true;
                                continue;
                            }

                            if (!declaredReturnType.getTypeArguments().isEmpty()) {
                                TypeMirror contextType = declaredReturnType.getTypeArguments().get(0);
                                if (dialogGroup.context == null) {
                                    dialogGroup.context = contextType;
                                } else if (!dialogGroup.context.equals(contextType)) {
                                    System.err.print("GenResult<C> " + dialogAnnotated + "::" + methodName);
                                    System.err.print(" has type " + contextType + " but the dialog already has a type of ");
                                    System.err.println(dialogGroup.context);
                                    fatal = true;
                                    continue;
                                }
                            }
                        } else if (TypeKind.VOID != returnType.getKind()) {
                            System.err.print("DialogStep " + dialogAnnotated + "::" + methodName);
                            System.err.println(" has an unsupported return type: " + returnType);
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

                dialogGroup.dialogs.put(dialogAnnotated.asType().toString(), dialogDescription);
            }
            if (fatal) {
                System.err.println("Ending annotation processor early");
                return true;
            }

            for (DialogGroup group : groups.values()) {
                Map<String, Set<String>> reachableDialogs = new TreeMap<>();
                System.out.println("--------------------------------------------");
                System.out.println("Group: " + group.groupName);
                System.out.println("Root: " + group.rootElement);
                System.out.println("Context Type: " + group.context);
                System.out.println("Message Type:" + group.message);
                System.out.println("Dialogs: ");
                for (DialogDescription dialog : group.dialogs.values()) {
                    Set<String> reachableSteps = new TreeSet<>();
                    System.out.println("\tClass: " + dialog.element);
                    System.out.println("\tRoot: " + dialog.annotation.root());
                    System.out.println("\tSteps:");
                    for (StepDescription step : dialog.stepMethods.values()) {
                        if (!step.annotation.callOnly()) {
                            System.out.println("\t\tName: " + step.name);
                            reachableSteps.add(step.name);
                        } else {
                            System.out.println("\t\tName: " + step.name + " (Call Only)");
                        }

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
                    reachableDialogs.put(dialog.element.toString(), reachableSteps);
                }

                // Validate that all defaults are reachable
                for (DialogDescription dialog : group.dialogs.values()) {
                    for (StepDescription step : dialog.stepMethods.values()) {
                        String nextStep = null;
                        if (step.annotation.step() != null && step.annotation.step().length() > 0) {
                            nextStep = step.annotation.step();
                        }

                        String nextDialog = dialog.element.toString();
                        try {
                            if (step.annotation.dialog() != Object.class) {
                                nextDialog = step.annotation.dialog().getCanonicalName();
                            }
                        } catch (MirroredTypeException m) {
                            // This is how to actually get the type in preprocessing
                            if (!m.getTypeMirror().toString().equals(Object.class.getCanonicalName())) {
                                nextDialog = m.getTypeMirror().toString();
                            }
                        }

                        Set<String> withinDialog = reachableDialogs.get(nextDialog);
                        if (withinDialog == null) {
                            System.err.print("Default next dialog is unreachable for ");
                            System.err.print(dialog.element + "::" + step.name);
                            System.err.println(": " + nextDialog);
                            fatal = true;
                            continue;
                        }
                        if (nextStep != null && !withinDialog.contains(nextStep)) {
                            System.err.print("Default next step is unreachable for ");
                            System.err.print(dialog.element + "::" + step.name);
                            System.err.println(": " + nextDialog + "::" + nextStep);
                            fatal = true;
                            continue;
                        }

                        // Looks like this is reachable
                    }
                }
                if (group.root == null) {
                    System.err.println("Group " + group + " does not have a RootDialog annotated dialog");
                    fatal = true;
                    continue;
                }
            }

            if (fatal) {
                System.err.println("Ending annotation processor early");
                return true;
            }

            TypeName stringType = TypeName.get(String.class);
            TypeName helperClass = ClassName.get(Helper.class);
            TypeName classType = ClassName.get(Class.class);

            for (DialogGroup group : groups.values()) {
                TypeName dialogType = ParameterizedTypeName.get(ClassName.get(Dialog.class), TypeName.get(group.context), TypeName.get(group.message), stringType, stringType);
                TypeName annotatedDialogType =  ParameterizedTypeName.get(ClassName.get(AnnotatedDialog.class), TypeName.get(group.context), TypeName.get(group.message));
                TypeName stepType = ParameterizedTypeName.get(ClassName.get(Step.class), TypeName.get(group.context), TypeName.get(group.message), stringType, stringType);
                TypeName callType = ParameterizedTypeName.get(ClassName.get(AnnotatedCall.class), TypeName.get(group.context), TypeName.get(group.message));

                TypeName botInterface = ParameterizedTypeName.get(ClassName.get(AnnotatedBot.class), TypeName.get(group.context), TypeName.get(group.message));
                TypeName dialogMapType = ParameterizedTypeName.get(ClassName.get(Map.class), stringType, dialogType);
                TypeName annotatedDialogMapType = ParameterizedTypeName.get(ClassName.get(Map.class), classType, annotatedDialogType);
                TypeName stepMapType = ParameterizedTypeName.get(ClassName.get(Map.class), stringType, stepType);
                TypeName callMapType = ParameterizedTypeName.get(ClassName.get(Map.class), stringType, callType);
                TypeSpec.Builder groupBuilder = TypeSpec.classBuilder(group.root.classPrefix() + "Bot")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addSuperinterface(botInterface)
                        .addAnnotation(AnnotationSpec.builder(GroupImpl.class).addMember("value", "$S", group.groupName).build())
                        .addField(FieldSpec.builder(dialogMapType, "dialogs", Modifier.PRIVATE, Modifier.FINAL).build())
                        .addField(FieldSpec.builder(annotatedDialogMapType, "annotatedDialogs", Modifier.PRIVATE, Modifier.FINAL).build())
                        .addField(FieldSpec.builder(stringType, "root", Modifier.PRIVATE, Modifier.FINAL).build())
                        .addMethod(MethodSpec.methodBuilder("rootDialog")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Override.class)
                                .addStatement("return $N", "root")
                                .returns(ClassName.get(String.class))
                                .build())
                        .addMethod(MethodSpec.methodBuilder("rootDialogClass")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Override.class)
                                .addStatement("return $T.class", group.rootElement.asType())
                                .returns(ClassName.get(Class.class))
                                .build())
                        .addMethod(MethodSpec.methodBuilder("getDialogs")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Override.class)
                                .addStatement("return $N", "dialogs")
                                .returns(dialogMapType)
                                .build())
                        .addMethod(MethodSpec.methodBuilder("getAnnotatedDialogs")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Override.class)
                                .addStatement("return $N", "annotatedDialogs")
                                .returns(annotatedDialogMapType)
                                .build());

                MethodSpec.Builder groupConstructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("$N = new $T()", "dialogs", ParameterizedTypeName.get(ClassName.get(TreeMap.class), stringType, dialogType))
                        .addStatement("$N = new $T()", "annotatedDialogs", ParameterizedTypeName.get(ClassName.get(HashMap.class), classType, annotatedDialogType))
                        .addStatement("$N = $T.class.getCanonicalName()", "root", group.rootElement.asType());

                // TODO maybe use better than dialog1, dialog2..
                int dialogCount = 0;
                for (DialogDescription dialog : group.dialogs.values()) {
                    dialogCount++;
                    String dialogImplClassName = group.root.classPrefix() + dialog.element.getSimpleName();
                    TypeName dialogImplClassType = ClassName.get(group.root.packageName(), dialogImplClassName);
                    TypeName dialogClassName = ClassName.get((TypeElement) dialog.element);
                    TypeSpec.Builder dialogImplBuilder = TypeSpec.classBuilder(dialogImplClassName)
                            .addAnnotation(AnnotationSpec.builder(DialogImpl.class).addMember("value", "$T.class", dialogClassName).build())
                            .addField(FieldSpec.builder(callMapType, "calls", Modifier.PRIVATE, Modifier.FINAL).build())
                            .addField(FieldSpec.builder(stepMapType, "steps", Modifier.PRIVATE, Modifier.FINAL).build())
                            .addField(FieldSpec.builder(botInterface, "bot", Modifier.PRIVATE, Modifier.FINAL).build())
                            .addField(FieldSpec.builder(dialogClassName, "inner", Modifier.PRIVATE, Modifier.FINAL).build())
                            .addSuperinterface(annotatedDialogType)
                            .addMethod(MethodSpec.methodBuilder("rootStep")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addAnnotation(Override.class)
                                    .addStatement("return $S", dialog.annotation.root())
                                    .returns(stringType)
                                    .build())
                            .addMethod(MethodSpec.methodBuilder("getSteps")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addAnnotation(Override.class)
                                    .addStatement("return this.$N", "steps")
                                    .returns(stepMapType)
                                    .build())
                            .addMethod(MethodSpec.methodBuilder("getCalls")
                                    .addModifiers(Modifier.PUBLIC)
                                    .addAnnotation(Override.class)
                                    .addStatement("return this.$N", "calls")
                                    .returns(callMapType)
                                    .build());

                    MethodSpec.Builder dialogConstructor = MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(botInterface, "bot")
                            .addParameter(dialogClassName, "inner")
                            .addStatement("this.$N = $N", "bot", "bot")
                            .addStatement("this.$N = $N", "inner", "inner")
                            .addStatement("this.$N = new $T()", "steps", ParameterizedTypeName.get(ClassName.get(TreeMap.class), stringType, stepType))
                            .addStatement("this.$N = new $T()", "calls", ParameterizedTypeName.get(ClassName.get(HashMap.class), stringType, callType));

                    int stepCount = 0;
                    for (StepDescription step : dialog.stepMethods.values()) {
                        stepCount++;
                        TypeName defaultNextDialogClass = dialogClassName;
                        try {
                            Class nextDialog = step.annotation.dialog();
                            if (nextDialog != Object.class) {
                                defaultNextDialogClass = TypeName.get(nextDialog);
                            }
                        } catch (MirroredTypeException m) {
                            TypeMirror nextDialog = m.getTypeMirror();
                            if (!nextDialog.toString().equals(Object.class.getCanonicalName())) {
                                defaultNextDialogClass = TypeName.get(nextDialog);
                            }
                        }
                        String defaultNextStep = step.annotation.step();
                        if (defaultNextStep.length() == 0) {
                            defaultNextStep = dialog.annotation.root();
                        }

                        String contextRef = "c";
                        String messageRef = "m";

                        final CodeBlock stepCall;
                        if (step.messageIndex == -1 && step.contextIndex == -1) {
                            // No context or step
                            stepCall = CodeBlock.of("$N.$N()", "inner", step.name);
                        } else if (step.messageIndex == 0) {
                            if (step.contextIndex == 1) {
                                stepCall = CodeBlock.of("$N.$N($N, $N)", "inner", step.name, messageRef, contextRef);
                            } else {
                                stepCall = CodeBlock.of("$N.$N($N)", "inner", step.name, messageRef);
                            }
                        } else if (step.contextIndex == 0) {
                            if (step.messageIndex == 1) {
                                stepCall = CodeBlock.of("$N.$N($N, $N)", "inner", step.name, contextRef, messageRef);
                            } else {
                                stepCall = CodeBlock.of("$N.$N($N)", "inner", step.name, contextRef);
                            }
                        } else {
                            throw new RuntimeException("Unexpected scenario: context and message indexes are not 0 or 1");
                        }

                        final CodeBlock innerCall;
                        if (step.returnType.getKind() == TypeKind.VOID) {
                            innerCall = CodeBlock.of("$T.toCall(() -> $L, $T.class, $S)", helperClass, stepCall, defaultNextDialogClass, defaultNextStep);
                        } else {
                            innerCall = CodeBlock.of("($N, $N) -> $L", contextRef, messageRef, stepCall);
                        }

                        dialogConstructor.addComment("$N", step.name);
                        dialogConstructor.addStatement("final $T $N = $L", callType, "call" + stepCount, innerCall);
                        dialogConstructor.addStatement("this.$N.put($S, $N)", "calls", step.name, "call" + stepCount);
                        if (!step.annotation.callOnly()) {
                            dialogConstructor.addStatement(
                                    "this.$N.put($S, ($N, $N) -> $T.wrap($N, $N, $N, $N, $T.class, $T.class, $S))",
                                    "steps",
                                    step.name,
                                    contextRef,
                                    messageRef,
                                    helperClass,
                                    "bot",
                                    contextRef,
                                    messageRef,
                                    "call" + stepCount,
                                    dialogClassName,
                                    defaultNextDialogClass,
                                    defaultNextStep
                            );
                        }
                    }

                    dialogImplBuilder.addMethod(dialogConstructor.build());

                    // Write class
                    JavaFile.builder(group.root.packageName(), dialogImplBuilder.build())
                            .build()
                            .writeTo(filer);

                    groupConstructor
                            .addParameter(ParameterSpec.builder(dialogClassName, "dialog" + dialogCount).build())
                            .addComment("$T", dialogClassName)
                            .addStatement("final $T $N = new $T(this, $N)", dialogImplClassType, "dialogImpl" + dialogCount, dialogImplClassType, "dialog" + dialogCount)
                            .addStatement("$N.put($T.class, $N)", "annotatedDialogs", dialogClassName, "dialogImpl" + dialogCount)
                            .addStatement("$N.put($T.class.getCanonicalName(), $N)", "dialogs", dialogClassName, "dialogImpl" + dialogCount);
                }

                groupBuilder.addMethod(groupConstructor.build());
                // TDOO wrap up with try?
                JavaFile.builder(group.root.packageName(), groupBuilder.build())
                        .build()
                        .writeTo(filer);
            }

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
        final Map<String, DialogDescription> dialogs;
        final RootDialog root;
        TypeMirror context;
        TypeMirror message;

        DialogGroup(String groupName, Element rootElement, RootDialog root) {
            this.groupName = groupName;
            this.rootElement = rootElement;
            this.root = root;
            this.dialogs = new TreeMap<>();
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
        final TypeMirror returnType;
        int contextIndex = -1;
        VariableElement contextElement;
        TypeMirror contextType;
        int messageIndex = -1;
        VariableElement messageElement;
        TypeMirror messageType;

        StepDescription(String name, ExecutableElement element, DialogStep annotation, TypeMirror returnType) {
            this.name = name;
            this.element = element;
            this.annotation = annotation;
            this.returnType = returnType;
        }
    }
}
