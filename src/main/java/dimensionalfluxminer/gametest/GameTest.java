package dimensionalfluxminer.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A GameTest of the mod: a {@code public static void name(GameTestHelper)} method of one of the classes listed in
 * {@link DfmGameTests}, registered as test {@code dimensionalfluxminer:<class>/<method>} (snake case).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {
    /** Structure the test runs in ({@code data/dimensionalfluxminer/structure/<template>.nbt}). */
    String template();

    int timeoutTicks() default 100;

    /** Tests of the same batch run together; each batch has its own test environment. */
    String batch() default "";

    boolean skyAccess() default false;

    boolean required() default true;

    int setupTicks() default 0;
}
