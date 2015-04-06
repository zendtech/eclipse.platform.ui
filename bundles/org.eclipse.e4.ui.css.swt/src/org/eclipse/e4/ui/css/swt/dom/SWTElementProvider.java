/*******************************************************************************
 * Copyright (c) 2008, 2014 Angelo Zerr and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 430639
 *     Fabio Zadrozny <fabiofz@gmail.com> - Bug 434201
 *******************************************************************************/
package org.eclipse.e4.ui.css.swt.dom;

import org.eclipse.e4.ui.css.core.dom.IElementProvider;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.w3c.dom.Element;

/**
 * Returns the CSS class which is responsible for styling a SWT widget
 *
 * Registered via the "org.eclipse.e4.ui.css.core.elementProvider" extension
 * point for the SWT widgets
 *
 *
 *
 * {@link IElementProvider} SWT implementation to retrieve w3c Element
 * {@link SWTElement} linked to SWT widget.
 *
 */
public class SWTElementProvider implements IElementProvider {

	public static final IElementProvider INSTANCE = new SWTElementProvider();

	@Override
	public Element getElement(Object element, CSSEngine engine) {
		if (element instanceof Text) {
			return new TextElement((Text) element, engine);
		}
		if (element instanceof Button) {
			return new ButtonElement((Button) element, engine);
		}
		if (element instanceof Scale) {
			return new ScaleElement((Scale) element, engine);
		}
		if (element instanceof Shell) {
			return new ShellElement((Shell) element, engine);
		}
		if (element instanceof CTabFolder) {
			return new CTabFolderElement((CTabFolder) element, engine);
		}
		if (element instanceof ToolBar) {
			return new ToolBarElement((ToolBar) element, engine);
		}
		if (element instanceof Tree) {
			// Note that the order is important (must appear before Control).
			return new TreeElement((Tree) element, engine);
		}
		if (element instanceof Composite) {
			return new CompositeElement((Composite) element, engine);
		}
		if (element instanceof Control) {
			return new ControlElement((Control) element, engine);
		}
		if (element instanceof CTabItem) {
			return new CTabItemElement((CTabItem) element, engine);
		}
		if (element instanceof TableItem) {
			return new TableItemElement((TableItem) element, engine);
		}
		if (element instanceof ToolItem) {
			return new ToolItemElement((ToolItem) element, engine);
		}
		if (element instanceof Item) {
			return new ItemElement((Item) element, engine);
		}
		if (element instanceof Widget) {
			return new WidgetElement((Widget) element, engine);
		}
		return null;
	}
}
