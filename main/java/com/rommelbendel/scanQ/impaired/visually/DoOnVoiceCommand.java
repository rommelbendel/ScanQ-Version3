package com.rommelbendel.scanQ.impaired.visually;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DoOnVoiceCommand {
    String[] commands() default ""; // "" für Methoden-Name
    String description() default "";
}
