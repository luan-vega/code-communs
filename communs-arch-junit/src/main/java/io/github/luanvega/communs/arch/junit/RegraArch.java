package io.github.luanvega.communs.arch.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca uma regra a executar numa classe com {@link AnalisarClasses}:
 * <ul>
 *     <li>um campo {@code static} do tipo {@code ArchRule}; ou</li>
 *     <li>um método {@code void} que recebe {@code JavaClasses} (estático ou de instância, com construtor sem argumentos).</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface RegraArch {
}
