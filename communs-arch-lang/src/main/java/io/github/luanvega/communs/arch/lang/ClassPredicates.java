package io.github.luanvega.communs.arch.lang;

import static io.github.luanvega.communs.arch.lang.DescribedPredicate.describe;

import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.core.domain.JavaClassKind;
import io.github.luanvega.communs.arch.core.domain.JavaModifier;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Predicados prontos sobre {@link JavaClass}. As descrições compõem o texto das regras,
 * ex.: {@code classes that reside in a package '..service..'}.
 */
public final class ClassPredicates {

    private ClassPredicates() {
    }

    public static DescribedPredicate<JavaClass> resideInAPackage(String packagePattern) {
        PackageMatcher matcher = PackageMatcher.of(packagePattern);
        return describe("reside in a package " + matcher, javaClass -> matcher.matches(javaClass.packageName()));
    }

    public static DescribedPredicate<JavaClass> resideInAnyPackage(String... packagePatterns) {
        List<PackageMatcher> matchers = Arrays.stream(packagePatterns).map(PackageMatcher::of).toList();
        String patterns = matchers.stream().map(PackageMatcher::toString).collect(Collectors.joining(", "));
        return describe("reside in any package [" + patterns + "]",
                javaClass -> matchers.stream().anyMatch(matcher -> matcher.matches(javaClass.packageName())));
    }

    public static DescribedPredicate<JavaClass> resideOutsideOfPackage(String packagePattern) {
        PackageMatcher matcher = PackageMatcher.of(packagePattern);
        return describe("reside outside of package " + matcher, javaClass -> !matcher.matches(javaClass.packageName()));
    }

    public static DescribedPredicate<JavaClass> haveFullyQualifiedName(String name) {
        return describe("have fully qualified name '" + name + "'", javaClass -> javaClass.name().equals(name));
    }

    public static DescribedPredicate<JavaClass> haveSimpleName(String simpleName) {
        return describe("have simple name '" + simpleName + "'", javaClass -> javaClass.simpleName().equals(simpleName));
    }

    public static DescribedPredicate<JavaClass> haveSimpleNameStartingWith(String prefix) {
        return describe("have simple name starting with '" + prefix + "'", javaClass -> javaClass.simpleName().startsWith(prefix));
    }

    public static DescribedPredicate<JavaClass> haveSimpleNameEndingWith(String suffix) {
        return describe("have simple name ending with '" + suffix + "'", javaClass -> javaClass.simpleName().endsWith(suffix));
    }

    public static DescribedPredicate<JavaClass> haveSimpleNameContaining(String infix) {
        return describe("have simple name containing '" + infix + "'", javaClass -> javaClass.simpleName().contains(infix));
    }

    public static DescribedPredicate<JavaClass> haveNameMatching(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return describe("have name matching '" + regex + "'", javaClass -> pattern.matcher(javaClass.name()).matches());
    }

    public static DescribedPredicate<JavaClass> areAnnotatedWith(String annotationTypeName) {
        return describe("are annotated with @" + simpleName(annotationTypeName), javaClass -> javaClass.isAnnotatedWith(annotationTypeName));
    }

    public static DescribedPredicate<JavaClass> areAnnotatedWith(Class<?> annotationType) {
        return areAnnotatedWith(annotationType.getName());
    }

    public static DescribedPredicate<JavaClass> areAssignableTo(String typeName) {
        return describe("are assignable to " + typeName, javaClass -> javaClass.isAssignableTo(typeName));
    }

    public static DescribedPredicate<JavaClass> areAssignableTo(Class<?> type) {
        return areAssignableTo(type.getName());
    }

    public static DescribedPredicate<JavaClass> areInterfaces() {
        return ofKind(JavaClassKind.INTERFACE, "are interfaces");
    }

    public static DescribedPredicate<JavaClass> areEnums() {
        return ofKind(JavaClassKind.ENUM, "are enums");
    }

    public static DescribedPredicate<JavaClass> areRecords() {
        return ofKind(JavaClassKind.RECORD, "are records");
    }

    public static DescribedPredicate<JavaClass> areAnnotations() {
        return ofKind(JavaClassKind.ANNOTATION, "are annotations");
    }

    public static DescribedPredicate<JavaClass> arePublic() {
        return haveModifier(JavaModifier.PUBLIC, "are public");
    }

    public static DescribedPredicate<JavaClass> areFinal() {
        return haveModifier(JavaModifier.FINAL, "are final");
    }

    public static DescribedPredicate<JavaClass> areNested() {
        return describe("are nested", JavaClass::isNested);
    }

    public static DescribedPredicate<JavaClass> areImported() {
        return describe("are imported", JavaClass::isImported);
    }

    public static DescribedPredicate<JavaClass> haveModifier(JavaModifier modifier, String description) {
        return describe(description, javaClass -> javaClass.modifiers().contains(modifier));
    }

    private static DescribedPredicate<JavaClass> ofKind(JavaClassKind kind, String description) {
        return describe(description, javaClass -> javaClass.kind() == kind);
    }

    static String simpleName(String typeName) {
        String withoutPackage = typeName.substring(typeName.lastIndexOf('.') + 1);
        return withoutPackage.substring(withoutPackage.lastIndexOf('$') + 1);
    }
}
