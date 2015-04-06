/*******************************************************************************
 * Copyright (c) 2015 Fabio Zadrozny and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.css.swt.dom;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

public class TreeElement extends ControlElement {

	private final static String TREE_ARROWS_FOREGROUND_COLOR = "org.eclipse.e4.ui.css.swt.treeArrowsForegroundColor"; //$NON-NLS-1$
	private final static String TREE_ARROWS_MODE = "org.eclipse.e4.ui.css.swt.treeArrowsMode"; //$NON-NLS-1$

	private final static String TREE_ARROWS_MODE_TRIANGLE = "triangle"; //$NON-NLS-1$
	private final static String TREE_ARROWS_MODE_SQUARE = "square"; //$NON-NLS-1$

	private static abstract class TreeItemPaintListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			Widget item = event.item;
			if (!(item instanceof TreeItem)) {
				return;
			}

			TreeItem treeItem = (TreeItem) item;
			if (treeItem.getItemCount() == 0) {
				return;
			}

			Tree parent = treeItem.getParent();
			boolean isCheckTree = (parent.getStyle() & SWT.CHECK) != 0;
			Object data = parent.getData(TREE_ARROWS_FOREGROUND_COLOR);
			if (!(data instanceof Color)) {
				// If color is not set, bail out.
				return;
			}

			Color foreground = (Color) data;
			Color background = parent.getBackground();

			GC gc = event.gc;

			gc.setForeground(foreground);
			if (background != null) {
				gc.setBackground(background);
			}

			// If it's a check tree, we should consider the space that the
			// checkbox takes.
			int baseX = isCheckTree ? (event.x - (16 * 2)) : (event.x - 16);

			// Many windows-only magic numbers here to erase the previous
			// arrow drawing.
			gc.fillRectangle(baseX, event.y + 4, 10, 11);

			// Draw the new arrow for the proper mode.
			draw(treeItem, foreground, background, event, baseX);
		}

		protected abstract void draw(TreeItem treeItem, Color foreground, Color background, Event event, int baseX);
	}

	/**
	 * A painter to draw squares as tree arrows.
	 */
	private static final TreeItemPaintListener treeItemSquaresPaintListener = new TreeItemPaintListener() {

		@Override
		protected void draw(TreeItem treeItem, Color foreground, Color background, Event event, int baseX) {
			GC gc = event.gc;

			// Many windows-only magic numbers here (the code below creates a
			// square with + or - depending on whether the item is collapsed or
			// expanded).
			int w = 9;
			int h = 9;
			int x = baseX;
			int y = event.y + 4;

			int halfH = h / 2;

			gc.drawRectangle(x + 1, y + 1, w - 1, h - 1);

			gc.drawLine(x + 3, y + halfH + 1, x + w - 2, y + halfH + 1);
			if (!treeItem.getExpanded()) {
				int halfW = w / 2;
				gc.drawLine(x + halfW + 1, y + 3, x + halfW + 1, y + h - 2);
			}
			event.detail &= ~SWT.BACKGROUND;
		}
	};

	/**
	 * A painter to draw triangles as tree arrows.
	 */
	private static final TreeItemPaintListener treeItemArrowsPaintListener = new TreeItemPaintListener() {

		@Override
		protected void draw(TreeItem treeItem, Color foreground, Color background, Event event, int baseX) {
			GC gc = event.gc;

			int w = 9;
			int h = 9;
			int x = baseX;
			int y = event.y + 4;

			int halfH = h / 2;
			// Many windows-only magic numbers here (the code draws a triangle
			// with the same coordinates used by windows -- with different
			// rotations depending on the expanded state).

			if (!treeItem.getExpanded()) {
				// Draws an open triangle if closed
				int px0 = x + 1;
				int py0 = y + 1;

				int py1 = y + halfH + 1;
				int px1 = x + (w / 2) + 1;
				int py2 = y + h;

				gc.drawLine(px0, py0, px0, py2);
				gc.drawLine(px0, py0, px1, py1);
				gc.drawLine(px0, py2, px1, py1);
			} else {
				// Draws a closed triangle if closed
				int px0 = x;
				int py0 = y;
				int px1 = x + w - 2;
				int py2 = y + h - 2;

				gc.setBackground(foreground);
				gc.fillPolygon(new int[] { px1, py0, px1, py2, px0, py2, px1, py0 });
				gc.setBackground(background);
			}
			event.detail &= ~SWT.BACKGROUND;
		}
	};

	public TreeElement(Tree composite, CSSEngine engine) {
		super(composite, engine);
	}

	public Tree getTree() {
		return (Tree) getNativeWidget();
	}

	@Override
	public void reset() {
		setTreeArrowsForegroundColor(null);
		super.reset();
	}

	/**
	 * Actually sets the paint listener.
	 *
	 * @param color
	 *            the foreground color to be used (if null removes the paint
	 *            listener).
	 */
	private void setPaintListener(Color color) {
		if (Constants.OS_WIN32.equals(Platform.getOS())) {
			Tree tree = getTree();
			// This code is currently windows-only (the custom painter would
			// need to be tweaked for other platforms).

			// Make sure we don't add the listener twice.
			tree.removeListener(SWT.PaintItem, treeItemSquaresPaintListener);
			tree.removeListener(SWT.PaintItem, treeItemArrowsPaintListener);

			if (color != null) {
				String treeArrowsMode = getTreeArrowsMode();
				if (TREE_ARROWS_MODE_TRIANGLE.equals(treeArrowsMode)) {
					tree.addListener(SWT.PaintItem, treeItemArrowsPaintListener);

				} else if (TREE_ARROWS_MODE_SQUARE.equals(treeArrowsMode)) {
					tree.addListener(SWT.PaintItem, treeItemSquaresPaintListener);

				} else {
					throw new AssertionError("Should not get here"); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Adds a custom paint listener which replaces the original tree arrows and
	 * draws new ones (based on the state of the TreeItem) if a color is passed
	 * (if null is passed, returns to the standard behavior).
	 *
	 * Note: the current implementation is windows-only (done for Bug 434201)
	 *
	 * @param color
	 *            The foreground color to be used to paint the items. May be
	 *            null (in which case we stop our custom painter from painting
	 *            the tree items).
	 */
	public void setTreeArrowsForegroundColor(Color color) {
		Tree tree = getTree();
		tree.setData(TREE_ARROWS_FOREGROUND_COLOR, color);
		setPaintListener(color);
	}

	/**
	 * @return the color to be used to draw the tree arrows foreground.
	 */
	public Color getTreeArrowsForegroundColor() {
		Tree tree = getTree();
		Object data = tree.getData(TREE_ARROWS_FOREGROUND_COLOR);
		if (data instanceof Color) {
			return (Color) data;
		}
		return null;
	}

	/**
	 * Sets the way to draw the tree arrows.
	 *
	 * @see TREE_ARROWS_MODE_ARROW
	 * @see TREE_ARROWS_MODE_SQUARE
	 */
	public void setTreeArrowsMode(String arrowsMode) {
		Tree tree = getTree();
		if (arrowsMode == null) {
			tree.setData(TREE_ARROWS_MODE, null);
			setPaintListener(getTreeArrowsForegroundColor());
			return;
		}
		Assert.isTrue(TREE_ARROWS_MODE_TRIANGLE.equals(arrowsMode) || TREE_ARROWS_MODE_SQUARE.equals(arrowsMode));
		tree.setData(TREE_ARROWS_MODE, arrowsMode);
	}

	/**
	 * @return the way to draw the tree arrows.
	 *
	 * @see TREE_ARROWS_MODE_ARROW
	 * @see TREE_ARROWS_MODE_SQUARE
	 */
	public String getTreeArrowsMode() {
		Tree tree = getTree();
		Object data = tree.getData(TREE_ARROWS_MODE);
		if (TREE_ARROWS_MODE_TRIANGLE.equals(data) || TREE_ARROWS_MODE_SQUARE.equals(data)) {
			return (String) data;
		}
		// Default is arrows
		return TREE_ARROWS_MODE_TRIANGLE;
	}
}