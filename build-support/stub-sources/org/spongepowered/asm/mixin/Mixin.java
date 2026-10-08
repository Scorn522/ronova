package org.spongepowered.asm.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Mixin reads this from RuntimeInvisibleAnnotations. Using RUNTIME here makes
// an offline-compiled mixin look unannotated to Mixin 0.8.5 at PREPARE time.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Mixin {
    Class<?>[] value() default {};
    String[] targets() default {};
    boolean remap() default true;
}
