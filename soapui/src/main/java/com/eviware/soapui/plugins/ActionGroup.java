/*
 *  SoapUI Pro, copyright (C) 2007-2014 smartbear.com
 */

package com.eviware.soapui.plugins;

import com.eviware.soapui.model.ModelItem;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides additional Action configuration to action classes in a plugin.
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionGroup {
    ActionMapping[] actions() default {};

    String defaultAction() default "";

    Class defaultTargetType() default ModelItem.class;
}
