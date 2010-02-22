/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.workbench.ui.renderers.swt;

import javax.inject.Inject;
import org.eclipse.e4.core.services.annotations.PostConstruct;
import org.eclipse.e4.core.services.annotations.PreDestroy;
import org.eclipse.e4.core.services.context.ContextChangeEvent;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.ui.model.application.MDirtyable;
import org.eclipse.e4.ui.model.application.MElementContainer;
import org.eclipse.e4.ui.model.application.MMenu;
import org.eclipse.e4.ui.model.application.MPart;
import org.eclipse.e4.ui.model.application.MPartStack;
import org.eclipse.e4.ui.model.application.MToolBar;
import org.eclipse.e4.ui.model.application.MUIElement;
import org.eclipse.e4.ui.model.application.MUILabel;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.e4.ui.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.swt.internal.AbstractPartRenderer;
import org.eclipse.e4.workbench.ui.IPresentationEngine;
import org.eclipse.e4.workbench.ui.UIEvents;
import org.eclipse.e4.workbench.ui.internal.IValueFunction;
import org.eclipse.e4.workbench.ui.internal.Trackable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class StackRenderer extends LazyStackRenderer {

	private static final String FOLDER_DISPOSED = "folderDisposed"; //$NON-NLS-1$

	Image viewMenuImage;

	@Inject
	IStylingEngine stylingEngine;

	@Inject
	IEventBroker eventBroker;

	@Inject
	IPresentationEngine renderer;

	private EventHandler itemUpdater;

	private EventHandler dirtyUpdater;

	public StackRenderer() {
		super();
	}

	@PostConstruct
	public void init() {
		super.init(eventBroker);

		itemUpdater = new EventHandler() {
			public void handleEvent(Event event) {
				Object objElement = event
						.getProperty(UIEvents.EventTags.ELEMENT);
				// Ensure that this event is for a MMenuItem
				if (!(objElement instanceof MUILabel)
						|| !(objElement instanceof MUIElement))
					return;

				// Extract the data bits
				MUIElement uiElement = (MUIElement) objElement;
				MUILabel modelItem = (MUILabel) objElement;

				// This listener only updates stacks -it- rendered
				MElementContainer<MUIElement> parent = uiElement.getParent();
				if (!(parent.getRenderer() == StackRenderer.this))
					return;

				// Is this Item visible
				MElementContainer<MUIElement> stack = uiElement.getParent();

				CTabItem item = findItemForPart(stack, uiElement);
				if (item == null)
					return;

				String attName = (String) event
						.getProperty(UIEvents.EventTags.ATTNAME);

				if (UIEvents.UILabel.LABEL.equals(attName)) {
					String newName = (String) event
							.getProperty(UIEvents.EventTags.NEW_VALUE);
					item.setText(getLabel((MPart) uiElement, newName));
				} else if (UIEvents.UILabel.ICONURI.equals(attName)) {
					item.setImage(getImage(modelItem));
				} else if (UIEvents.UILabel.TOOLTIP.equals(attName)) {
					String newTTip = (String) event
							.getProperty(UIEvents.EventTags.NEW_VALUE);
					item.setToolTipText(newTTip);
				}
			}
		};

		eventBroker.subscribe(UIEvents.buildTopic(UIEvents.UILabel.TOPIC),
				itemUpdater);

		dirtyUpdater = new EventHandler() {
			public void handleEvent(Event event) {
				Object objElement = event
						.getProperty(UIEvents.EventTags.ELEMENT);

				// Ensure that this event is for a MMenuItem
				if (!(objElement instanceof MPart)) {
					return;
				}

				// Extract the data bits
				MPart uiElement = (MPart) objElement;

				// This listener only updates stacks -it- rendered
				MElementContainer<MUIElement> parent = uiElement.getParent();
				if (!(parent.getRenderer() == StackRenderer.this)) {
					return;
				}

				// Is this Item visible
				MElementContainer<MUIElement> stack = uiElement.getParent();

				CTabItem item = findItemForPart(stack, uiElement);
				if (item == null) {
					return;
				}

				Boolean dirtyState = (Boolean) event
						.getProperty(UIEvents.EventTags.NEW_VALUE);
				String text = item.getText();
				boolean hasAsterisk = text.charAt(0) == '*';
				if (dirtyState.booleanValue()) {
					if (!hasAsterisk) {
						item.setText('*' + text);
					}
				} else if (hasAsterisk) {
					item.setText(text.substring(1));
				}
			}
		};

		eventBroker.subscribe(UIEvents.buildTopic(UIEvents.Dirtyable.TOPIC,
				UIEvents.Dirtyable.DIRTY), dirtyUpdater);
	}

	@PreDestroy
	public void contextDisposed() {
		super.contextDisposed(eventBroker);

		eventBroker.unsubscribe(itemUpdater);
		eventBroker.unsubscribe(dirtyUpdater);
	}

	private String getLabel(MUILabel itemPart, String newName) {
		if (newName == null) {
			newName = ""; //$NON-NLS-1$
		}
		if (itemPart instanceof MDirtyable && ((MDirtyable) itemPart).isDirty()) {
			newName = '*' + newName;
		}
		return newName;
	}

	public Object createWidget(MUIElement element, Object parent) {
		Widget newWidget = null;

		if (!(element instanceof MPartStack) || !(parent instanceof Composite))
			return null;

		Composite parentComposite = (Composite) parent;

		// TODO see bug #267434, SWT.BORDER should be determined from CSS
		// TODO see bug #282901 - [UI] Need better support for switching
		// renderer to use

		// TBD: need to define attributes to handle this
		int styleModifier = 0; // SWT.CLOSE
		final CTabFolder ctf = new CTabFolder(parentComposite, SWT.BORDER
				| styleModifier);

		// configureForStyling(ctf);

		// TBD: need to handle this
		// boolean showCloseAlways = element instanceof MEditorStack;
		ctf.setUnselectedCloseVisible(false);

		newWidget = ctf;

		final IEclipseContext folderContext = getContext(element);
		folderContext.set("canCloseFunc", new IValueFunction() { //$NON-NLS-1$
					public Object getValue() {
						return true;
					}
				});

		folderContext.set(FOLDER_DISPOSED, Boolean.FALSE);
		final IEclipseContext toplevelContext = getToplevelContext(element);
		final Trackable updateActiveTab = new Trackable(folderContext) {
			public boolean notify(ContextChangeEvent event) {
				if (event.getEventType() == ContextChangeEvent.DISPOSE)
					return false;
				if (!participating) {
					return true;
				}
				trackingContext.get(FOLDER_DISPOSED);
				IEclipseContext currentActive = toplevelContext;
				IEclipseContext child;
				while (currentActive != trackingContext
						&& (child = (IEclipseContext) currentActive
								.get("activeChild")) != null && child != currentActive) { //$NON-NLS-1$
					currentActive = child;
				}
				// System.out.println(cti.getText() + " is now " + ((currentActive == tabItemContext) ? "active" : "inactive"));   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

				String cssClassName = (currentActive == trackingContext) ? "active" //$NON-NLS-1$
						: "inactive"; //$NON-NLS-1$
				stylingEngine.setClassname(ctf, cssClassName);

				// TODO HACK Bug 283073 [CSS] CTabFolder.getTopRight()
				// should get same background color
				if (ctf.getTopRight() != null)
					stylingEngine.setClassname(ctf.getTopRight(), cssClassName);

				// TODO HACK: see Bug 283585 [CSS] Specificity fails with
				// descendents
				CTabItem[] items = ctf.getItems();
				for (int i = 0; i < items.length; i++) {
					stylingEngine.setClassname(items[i], cssClassName);
				}
				return true;
			}
		};
		ctf.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				updateActiveTab.participating = false;
				folderContext.set(FOLDER_DISPOSED, Boolean.TRUE);
			}
		});
		folderContext.runAndTrack(updateActiveTab, null);

		return newWidget;
	}

	public void postProcess(MUIElement element) {
		super.postProcess(element);

		if (!(element instanceof MElementContainer<?>))
			return;

		MElementContainer<MUIElement> container = (MElementContainer<MUIElement>) element;
		CTabFolder ctf = (CTabFolder) element.getWidget();
		MPart selPart = ((MPartStack) element).getSelectedElement();
		if (selPart == null)
			return;

		// Find the tab associated with the part and set it as the selection
		CTabItem selItem = findItemForPart(container, selPart);
		if (selItem != null) {
			if (selPart.getWidget() == null) {
				IPresentationEngine renderer = (IPresentationEngine) getContext(
						selPart).get(IPresentationEngine.class.getName());
				renderer.createGui(selPart);
			}
			ctf.setSelection(selItem);
			showTab(container, selPart);
			activate(selPart);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.e4.workbench.ui.renderers.swt.LazyStackFactory#internalChildAdded
	 * (org.eclipse.e4.ui.model.application.MPart,
	 * org.eclipse.e4.ui.model.application.MPart)
	 */
	@Override
	protected void showChild(MElementContainer<MUIElement> stack,
			MUIElement part) {
		// TODO Auto-generated method stub
		super.showChild(stack, part);

		MUILabel itemPart = (MUILabel) part;
		CTabFolder ctf = (CTabFolder) stack.getWidget();
		int createFlags = 0;

		CTabItem cti = findItemForPart(stack, part);
		if (cti == null) {
			int index = calcIndexFor(stack, part);
			// TODO see bug 282901 - [UI] Need better support for switching
			// renderer to use
			cti = new CTabItem(ctf, createFlags, index);

			cti.setData(OWNING_ME, part);
			cti.setText(getLabel(itemPart, itemPart.getLabel()));
			cti.setImage(getImage(itemPart));
			cti.setToolTipText(itemPart.getTooltip());

			Control ctrl = (Control) part.getWidget();
			if (ctrl != null) {
				if (ctrl.getParent() != ctf)
					ctrl.setParent(ctf);
				cti.setControl(ctrl);
			}

			// TODO HACK: see Bug 283585 [CSS] Specificity fails with
			// descendents
			String cssClassName = (String) ctf
					.getData("org.eclipse.e4.ui.css.CssClassName"); //$NON-NLS-1$
			stylingEngine.setClassname(cti, cssClassName);
		}

		// Hook up special logic to synch up the Tab Items
		hookTabControllerLogic(stack, part, cti);

		// Re-ensure that the activeChild == the selected tab
		if (stack.getSelectedElement() == null)
			stack.setSelectedElement(part);

		if (stack.getSelectedElement() != null) {
			CTabItem selCTI = findItemForPart(stack, stack.getSelectedElement());
			if (selCTI != null && selCTI != ctf.getSelection())
				ctf.setSelection(selCTI);
		}
	}

	private int calcIndexFor(MElementContainer<MUIElement> stack,
			final MUIElement part) {
		int index = 0;

		// Find the -visible- part before this element
		for (MUIElement mPart : stack.getChildren()) {
			if (mPart == part)
				return index;
			if (mPart.isToBeRendered())
				index++;
		}
		return index;
	}

	@Override
	public void childRendered(
			final MElementContainer<MUIElement> parentElement,
			MUIElement element) {
		super.childRendered(parentElement, element);

		if (!(((MUIElement) parentElement) instanceof MPartStack)
				|| !(element instanceof MPart))
			return;

		showChild(parentElement, element);

		// Lazy Loading: On the first pass through this method the
		// part's control will be null (we're just creating the tabs
		Control ctrl = (Control) element.getWidget();
		if (ctrl != null) {
			// showTab(parentElement, element);
			stylingEngine.style(ctrl);
		}
	}

	private CTabItem findItemForPart(MElementContainer<MUIElement> stack,
			MUIElement part) {
		CTabFolder ctf = (CTabFolder) stack.getWidget();
		if (ctf == null)
			return null;

		CTabItem[] items = ctf.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getData(OWNING_ME) == part)
				return items[i];
		}
		return null;
	}

	private void hookTabControllerLogic(
			final MElementContainer<MUIElement> stack, final MUIElement part,
			final CTabItem cti) {
	}

	@Override
	public void hideChild(MElementContainer<MUIElement> parentElement,
			MUIElement child) {
		super.hideChild(parentElement, child);

		if (!(((MUIElement) parentElement) instanceof MPartStack)
				|| !(child instanceof MPart))
			return;

		CTabItem oldItem = findItemForPart(parentElement, child);
		if (oldItem != null) {
			oldItem.setControl(null); // prevent the widget from being disposed
			oldItem.dispose();
		}

		// Check if we have to reset the currently active child for the stack
		if (parentElement.getSelectedElement() == child) {
			// HACK!! we'll reset to the first element for now but really should
			// be based on the activation chain
			if (parentElement.getChildren().size() == 0) {
				parentElement.setSelectedElement(null);
			} else {
				parentElement.setSelectedElement(parentElement.getChildren()
						.get(0));
			}
		}
	}

	@Override
	public void hookControllerLogic(final MUIElement me) {
		super.hookControllerLogic(me);

		if (!(me instanceof MElementContainer<?>))
			return;

		final MElementContainer<MUIElement> stack = (MElementContainer<MUIElement>) me;

		// Match the selected TabItem to its Part
		CTabFolder ctf = (CTabFolder) me.getWidget();
		ctf.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				MPart newPart = (MPart) e.item.getData(OWNING_ME);
				if (stack.getSelectedElement() != newPart) {
					activate(newPart);
				}

				showTab(stack, newPart);
			}
		});

		CTabFolder2Adapter closeListener = new CTabFolder2Adapter() {
			public void close(CTabFolderEvent event) {
				MPart part = (MPart) event.item
						.getData(AbstractPartRenderer.OWNING_ME);

				// Allow closes to be 'canceled'
				IEclipseContext partContext = part.getContext();
				IValueFunction closeFunc = (IValueFunction) partContext
						.get("canCloseFunc"); //$NON-NLS-1$
				boolean canClose = closeFunc == null
						|| (Boolean) closeFunc.getValue();
				if (!canClose) {
					event.doit = false;
					return;
				}
				part.setToBeRendered(false);
			}
		};
		ctf.addCTabFolder2Listener(closeListener);

		// Detect activation...picks up cases where the user clicks on the
		// (already active) tab
		ctf.addListener(SWT.Activate, new org.eclipse.swt.widgets.Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				CTabFolder ctf = (CTabFolder) event.widget;
				MPartStack stack = (MPartStack) ctf.getData(OWNING_ME);
				MPart part = stack.getSelectedElement();
				if (part != null)
					activate(part);
			}
		});
	}

	private void showTab(MElementContainer<MUIElement> parentElement,
			MUIElement element) {
		CTabFolder ctf = (CTabFolder) getParentWidget(element);
		Control ctrl = (Control) element.getWidget();
		CTabItem cti = findItemForPart(parentElement, element);
		cti.setControl(ctrl);

		ToolBar tb = getToolbar(element);
		if (tb != null) {
			Control curTR = ctf.getTopRight();
			if (curTR != null)
				curTR.dispose();

			if (tb.getSize().y > ctf.getTabHeight())
				ctf.setTabHeight(tb.getSize().y);

			// TODO HACK: see Bug 283073 [CSS] CTabFolder.getTopRight() should
			// get same background color
			String cssClassName = (String) ctf
					.getData("org.eclipse.e4.ui.css.CssClassName"); //$NON-NLS-1$
			stylingEngine.setClassname(tb, cssClassName);

			ctf.setTopRight(tb, SWT.RIGHT);
			ctf.layout(true);

			// TBD In 3.x views listening on the "parent" get an intermediary
			// composite parented of the CTabFolder, but in E4 they get
			// the CTabFolder itself.
			// The layout() call above generates resize messages for children,
			// but not for the CTabFlder itself. Hence, children listening for
			// this message on the parent don't receive notifications in E4.
			// For now, send an explicit Resize message to the CTabFolder
			// listeners.
			// The enhancement request 279263 suggests a more general solution.
			ctf.notifyListeners(SWT.Resize, null);
		}

	}

	private ToolBar getToolbar(MUIElement element) {
		if (!(element instanceof MPart))
			return null;

		MPart part = (MPart) element;
		boolean hasMenu = part.getMenus() != null && part.getMenus().size() > 0;
		boolean hasTB = part.getToolbar() != null;
		if (!hasMenu && !hasTB)
			return null;

		CTabFolder ctf = (CTabFolder) getParentWidget(part);

		ToolBar tb;
		MToolBar tbModel = part.getToolbar();
		if (tbModel != null) {
			tb = (ToolBar) renderer.createGui(tbModel, ctf);
		} else {
			tb = new ToolBar(ctf, SWT.FLAT | SWT.HORIZONTAL);
		}

		// View menu (if any)
		if (hasMenu) {
			addMenuButton(part, tb, part.getMenus().get(0));
		}

		tb.pack();
		return tb;
	}

	/**
	 * @param tb
	 */
	private void addMenuButton(MPart part, ToolBar tb, MMenu menu) {
		ToolItem ti = new ToolItem(tb, SWT.PUSH);
		ti.setImage(getViewMenuImage());
		ti.setHotImage(null);
		ti.setToolTipText("View Menu"); //$NON-NLS-1$
		ti.setData("theMenu", menu); //$NON-NLS-1$
		ti.setData("thePart", part); //$NON-NLS-1$

		ti.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				showMenu((ToolItem) e.widget);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				showMenu((ToolItem) e.widget);
			}
		});
	}

	/**
	 * @param item
	 */
	protected void showMenu(ToolItem item) {
		// Create the UI for the menu
		final MMenu menuModel = (MMenu) item.getData("theMenu"); //$NON-NLS-1$
		MPart part = (MPart) item.getData("thePart"); //$NON-NLS-1$
		Control ctrl = (Control) part.getWidget();
		Menu menu = (Menu) renderer.createGui(menuModel, ctrl.getShell());

		// ...and Show it...
		Rectangle ib = item.getBounds();
		Point displayAt = item.getParent().toDisplay(ib.x, ib.y + ib.height);
		menu.setLocation(displayAt);
		menu.setVisible(true);

		Display display = Display.getCurrent();
		while (!menu.isDisposed() && menu.isVisible()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		menu.dispose();
	}

	private Image getViewMenuImage() {
		if (viewMenuImage == null) {
			Display d = Display.getCurrent();

			Image viewMenu = new Image(d, 16, 16);
			Image viewMenuMask = new Image(d, 16, 16);

			Display display = Display.getCurrent();
			GC gc = new GC(viewMenu);
			GC maskgc = new GC(viewMenuMask);
			gc.setForeground(display
					.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));

			int[] shapeArray = new int[] { 6, 1, 15, 1, 11, 5, 10, 5 };
			gc.fillPolygon(shapeArray);
			gc.drawPolygon(shapeArray);

			Color black = display.getSystemColor(SWT.COLOR_BLACK);
			Color white = display.getSystemColor(SWT.COLOR_WHITE);

			maskgc.setBackground(black);
			maskgc.fillRectangle(0, 0, 16, 16);

			maskgc.setBackground(white);
			maskgc.setForeground(white);
			maskgc.fillPolygon(shapeArray);
			maskgc.drawPolygon(shapeArray);
			gc.dispose();
			maskgc.dispose();

			ImageData data = viewMenu.getImageData();
			data.transparentPixel = data.getPixel(0, 0);

			viewMenuImage = new Image(d, viewMenu.getImageData(), viewMenuMask
					.getImageData());
			viewMenu.dispose();
			viewMenuMask.dispose();
		}
		return viewMenuImage;
	}
}