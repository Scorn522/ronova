package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface At {
    String value();
    String target() default "";
    int ordinal() default -1;
    Shift shift() default Shift.NONE;
    boolean remap() default true;

    enum Shift { BEFORE, AFTER, BY, NONE }
}
