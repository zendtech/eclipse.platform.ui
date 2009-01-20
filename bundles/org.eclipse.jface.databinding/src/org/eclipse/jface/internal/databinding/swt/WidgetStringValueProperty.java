/*******************************************************************************
 * Copyright (c) 2008 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 *     Matthew Hall - bug 195222
 ******************************************************************************/

package org.eclipse.jface.internal.databinding.swt;

/**
 * @since 3.3
 * 
 */
public abstract class WidgetStringValueProperty extends WidgetValueProperty {
	WidgetStringValueProperty() {
		super();
	}

	WidgetStringValueProperty(int event) {
		super(event);
	}

	public Object getValueType() {
		return String.class;
	}

	protected Object doGetValue(Object source) {
		return doGetStringValue(source);
	}

	protected void doSetValue(Object source, Object value) {
		doSetStringValue(source, (String) value);
	}

	abstract String doGetStringValue(Object source);

	abstract void doSetStringValue(Object source, String value);
}
