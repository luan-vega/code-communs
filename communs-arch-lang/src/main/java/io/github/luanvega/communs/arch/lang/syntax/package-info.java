/**
 * DSL fluente para declarar regras sobre classes:
 *
 * <pre>{@code
 * ArchRule regra = classes().that().resideInAPackage("..controller..")
 *         .should().onlyDependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "java..")
 *         .because("controllers só falam com services");
 * regra.check(classes);
 * }</pre>
 */
package io.github.luanvega.communs.arch.lang.syntax;
