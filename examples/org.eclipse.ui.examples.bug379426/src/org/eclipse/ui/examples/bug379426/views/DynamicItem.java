package org.eclipse.ui.examples.bug379426.views;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

public class DynamicItem extends CompoundContributionItem implements
		IWorkbenchContribution {

	private IServiceLocator locator;

	public DynamicItem() {
	}

	public DynamicItem(String id) {
		super(id);
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		IContributionItem[] items = new IContributionItem[2];
		MenuManager m1 = new MenuManager("Name 1");
		items[0] = m1;
		m1.add(getCommand(IWorkbenchCommandConstants.HELP_ABOUT, 1));
		m1.add(getCommand(IWorkbenchCommandConstants.HELP_ABOUT, 2));
		MenuManager m2 = new MenuManager("Name 2");
		items[1] = m2;
		m2.add(getCommand(IWorkbenchCommandConstants.HELP_ABOUT, 3));
		m2.add(getCommand(IWorkbenchCommandConstants.HELP_ABOUT, 4));
		return items;
	}

	private IContributionItem getCommand(String commandId, int idNum) {
		CommandContributionItemParameter parm = new CommandContributionItemParameter(
				locator, "com.example." + idNum, commandId,
				CommandContributionItem.STYLE_PUSH);
		return new CommandContributionItem(parm);
	}

	@Override
	public void initialize(IServiceLocator serviceLocator) {
		locator = serviceLocator;
	}

}
