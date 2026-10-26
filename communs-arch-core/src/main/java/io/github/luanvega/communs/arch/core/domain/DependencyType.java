package io.github.luanvega.communs.arch.core.domain;

/**
 * Como uma classe depende de outra.
 */
public enum DependencyType {
    /** A classe estende a outra. */
    EXTENDS,
    /** A classe implementa a interface (ou a interface estende outra). */
    IMPLEMENTS,
    /** A classe, um membro ou um parâmetro é anotado com o tipo. */
    ANNOTATION,
    /** Um campo é do tipo. */
    FIELD_TYPE,
    /** Um método recebe o tipo como parâmetro. */
    METHOD_PARAMETER,
    /** Um método retorna o tipo. */
    METHOD_RETURN,
    /** Um método declara o tipo em {@code throws}. */
    THROWS,
    /** O código chama um método do tipo. */
    METHOD_CALL,
    /** O código chama um construtor do tipo. */
    CONSTRUCTOR_CALL,
    /** O código lê ou escreve um campo do tipo. */
    FIELD_ACCESS,
    /** O código cita o tipo (cast, instanceof, literal de classe, catch, arrays). */
    TYPE_REFERENCE
}
