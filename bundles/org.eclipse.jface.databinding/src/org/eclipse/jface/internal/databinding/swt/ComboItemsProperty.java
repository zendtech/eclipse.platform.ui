/*******************************************************************************
 * Copyright (c) 2008 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 ******************************************************************************/

package org.eclipse.jface.internal.databinding.swt;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;

/**
 * @since 3.3
 * 
 */
public class ComboItemsProperty extends ControlStringListProperty {
	protected void doSetStringList(Control control, String[] list) {
		((Combo) control).setItems(list);
	}

	public String[] doGetStringList(Control control) {
		return ((Combo) control).getItems();
	}

	public String toString() {
		return "Combo.items[] <String>"; //$NON-NLS-1$
	}
}
