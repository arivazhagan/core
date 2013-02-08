/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.ui;

import java.util.concurrent.Callable;

public interface UISelectMany<VALUETYPE> extends UIInputComponent<UISelectMany<VALUETYPE>, VALUETYPE>
{
   Iterable<VALUETYPE> getValueChoices();

   UISelectMany<VALUETYPE> setValueChoices(Iterable<VALUETYPE> values);

   Iterable<VALUETYPE> getValue();

   UISelectMany<VALUETYPE> setDefaultValue(Iterable<VALUETYPE> value);

   UISelectMany<VALUETYPE> setDefaultValue(Callable<Iterable<VALUETYPE>> callback);

   UISelectMany<VALUETYPE> setValue(Iterable<VALUETYPE> value);
}