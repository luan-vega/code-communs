package io.github.luanvega.communs.arch.core.importer;

import io.github.luanvega.communs.arch.core.domain.Dependency;
import io.github.luanvega.communs.arch.core.domain.DependencyType;
import io.github.luanvega.communs.arch.core.domain.JavaAnnotation;
import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.core.domain.JavaClassKind;
import io.github.luanvega.communs.arch.core.domain.JavaField;
import io.github.luanvega.communs.arch.core.domain.JavaMethod;
import io.github.luanvega.communs.arch.core.domain.JavaModifier;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Lê um arquivo {@code .class} com ASM e monta o {@link JavaClass} correspondente.
 */
final class JavaClassReader extends ClassVisitor {

    private static final int ASM_API = Opcodes.ASM9;

    private String name;
    private int access;
    private String superclassName;
    private final List<String> interfaceNames = new ArrayList<>();
    private final List<JavaAnnotation> annotations = new ArrayList<>();
    private final List<JavaField> fields = new ArrayList<>();
    private final List<JavaMethod> methods = new ArrayList<>();
    private final Set<Dependency> dependencies = new LinkedHashSet<>();
    private String sourceFileName;

    private JavaClassReader() {
        super(ASM_API);
    }

    static JavaClass read(InputStream bytecode) throws IOException {
        JavaClassReader reader = new JavaClassReader();
        new ClassReader(bytecode).accept(reader, ClassReader.SKIP_FRAMES);
        return reader.build();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.name = className(name);
        this.access = access;
        if (superName != null) {
            superclassName = className(superName);
            addDependency(superclassName, DependencyType.EXTENDS, 0);
        }
        for (String interfaceName : interfaces) {
            String className = className(interfaceName);
            interfaceNames.add(className);
            addDependency(className, DependencyType.IMPLEMENTS, 0);
        }
    }

    @Override
    public void visitInnerClass(String innerName, String outerName, String simpleName, int innerAccess) {
        // Para classes aninhadas, os modificadores reais (static, private...) só aparecem no atributo InnerClasses.
        if (className(innerName).equals(name)) {
            access = innerAccess | (access & (Opcodes.ACC_RECORD | Opcodes.ACC_SUPER));
        }
    }

    @Override
    public void visitSource(String source, String debug) {
        sourceFileName = source;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        annotations.add(annotation(descriptor));
        return null;
    }

    @Override
    public FieldVisitor visitField(int access, String fieldName, String descriptor, String signature, Object value) {
        String typeName = Type.getType(descriptor).getClassName();
        addTypeDependency(Type.getType(descriptor), DependencyType.FIELD_TYPE, 0);
        List<JavaAnnotation> fieldAnnotations = new ArrayList<>();
        fields.add(null);
        int index = fields.size() - 1;
        return new FieldVisitor(ASM_API) {
            @Override
            public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                fieldAnnotations.add(annotation(annotationDescriptor));
                return null;
            }

            @Override
            public void visitEnd() {
                fields.set(index, new JavaField(fieldName, typeName, JavaModifier.ofField(access), fieldAnnotations));
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
        Type methodType = Type.getMethodType(descriptor);
        List<String> parameterTypeNames = new ArrayList<>();
        for (Type parameterType : methodType.getArgumentTypes()) {
            parameterTypeNames.add(parameterType.getClassName());
            addTypeDependency(parameterType, DependencyType.METHOD_PARAMETER, 0);
        }
        addTypeDependency(methodType.getReturnType(), DependencyType.METHOD_RETURN, 0);
        List<String> throwsTypeNames = new ArrayList<>();
        if (exceptions != null) {
            for (String exception : exceptions) {
                String className = className(exception);
                throwsTypeNames.add(className);
                addDependency(className, DependencyType.THROWS, 0);
            }
        }
        List<JavaAnnotation> methodAnnotations = new ArrayList<>();
        methods.add(null);
        int index = methods.size() - 1;
        return new MethodVisitor(ASM_API) {
            private int currentLine;

            @Override
            public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                methodAnnotations.add(annotation(annotationDescriptor));
                return null;
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String annotationDescriptor, boolean visible) {
                addTypeDependency(Type.getType(annotationDescriptor), DependencyType.ANNOTATION, 0);
                return null;
            }

            @Override
            public void visitLineNumber(int line, Label start) {
                currentLine = line;
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String calledName, String calledDescriptor, boolean isInterface) {
                DependencyType type = JavaMethod.CONSTRUCTOR_NAME.equals(calledName)
                        ? DependencyType.CONSTRUCTOR_CALL
                        : DependencyType.METHOD_CALL;
                addTypeDependency(Type.getObjectType(owner), type, currentLine, calledName);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                addTypeDependency(Type.getObjectType(owner), DependencyType.FIELD_ACCESS, currentLine, fieldName);
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                // NEW já é seguido da chamada ao construtor, que gera CONSTRUCTOR_CALL.
                if (opcode != Opcodes.NEW) {
                    addTypeDependency(Type.getObjectType(type), DependencyType.TYPE_REFERENCE, currentLine);
                }
            }

            @Override
            public void visitMultiANewArrayInsn(String arrayDescriptor, int numDimensions) {
                addTypeDependency(Type.getType(arrayDescriptor), DependencyType.TYPE_REFERENCE, currentLine);
            }

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof Type type && type.getSort() != Type.METHOD) {
                    addTypeDependency(type, DependencyType.TYPE_REFERENCE, currentLine);
                }
            }

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                if (type != null) {
                    addTypeDependency(Type.getObjectType(type), DependencyType.TYPE_REFERENCE, currentLine);
                }
            }

            @Override
            public void visitInvokeDynamicInsn(String dynamicName, String dynamicDescriptor, Handle bootstrap, Object... arguments) {
                // Referências de método (Pedido::id) e lambdas apontam para o método alvo num Handle.
                for (Object argument : arguments) {
                    if (argument instanceof Handle handle) {
                        DependencyType type = JavaMethod.CONSTRUCTOR_NAME.equals(handle.getName())
                                ? DependencyType.CONSTRUCTOR_CALL
                                : DependencyType.METHOD_CALL;
                        addTypeDependency(Type.getObjectType(handle.getOwner()), type, currentLine, handle.getName());
                    }
                }
            }

            @Override
            public void visitEnd() {
                methods.set(index, new JavaMethod(
                        methodName,
                        methodType.getReturnType().getClassName(),
                        parameterTypeNames,
                        throwsTypeNames,
                        JavaModifier.ofMethod(access),
                        methodAnnotations));
            }
        };
    }

    private JavaAnnotation annotation(String descriptor) {
        Type type = Type.getType(descriptor);
        addTypeDependency(type, DependencyType.ANNOTATION, 0);
        return new JavaAnnotation(type.getClassName());
    }

    private void addTypeDependency(Type type, DependencyType dependencyType, int lineNumber) {
        addTypeDependency(type, dependencyType, lineNumber, "");
    }

    private void addTypeDependency(Type type, DependencyType dependencyType, int lineNumber, String memberName) {
        Type elementType = type.getSort() == Type.ARRAY ? type.getElementType() : type;
        if (elementType.getSort() == Type.OBJECT) {
            addDependency(elementType.getClassName(), dependencyType, lineNumber, memberName);
        }
    }

    private void addDependency(String targetClassName, DependencyType type, int lineNumber) {
        addDependency(targetClassName, type, lineNumber, "");
    }

    private void addDependency(String targetClassName, DependencyType type, int lineNumber, String memberName) {
        if (!targetClassName.equals(name)) {
            dependencies.add(new Dependency(name, targetClassName, type, lineNumber, memberName));
        }
    }

    private JavaClass build() {
        return new JavaClass(
                name,
                kind(),
                JavaModifier.ofClass(access),
                superclassName,
                interfaceNames,
                annotations,
                fields,
                methods,
                dependencies,
                sourceFileName);
    }

    private JavaClassKind kind() {
        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            return JavaClassKind.ANNOTATION;
        }
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            return JavaClassKind.INTERFACE;
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            return JavaClassKind.ENUM;
        }
        if ((access & Opcodes.ACC_RECORD) != 0) {
            return JavaClassKind.RECORD;
        }
        return JavaClassKind.CLASS;
    }

    private static String className(String internalName) {
        return Type.getObjectType(internalName).getClassName();
    }
}
