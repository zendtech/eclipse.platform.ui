/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.keys;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.BindingManager;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.misc.Policy;
import org.eclipse.ui.keys.IBindingService;

/**
 * <p>
 * A static class for accessing the registry and the preference store.
 * </p>
 * <p>
 * <em>EXPERIMENTAL</em>. The commands architecture is currently under
 * development for Eclipse 3.1. This class -- its existence, its name and its
 * methods -- are in flux. Do not use this class yet.
 * </p>
 * <p>
 * TODO Add methods for reading the extension registry and the preference store.
 * </p>
 * 
 * @since 3.1
 */
public final class BindingPersistence {

	/**
	 * The name of the attribute storing the command id for a binding.
	 */
	private static final String ATTRIBUTE_COMMAND_ID = "commandId"; //$NON-NLS-1$

	/**
	 * The name of the attribute storing the context id for a binding.
	 */
	private static final String ATTRIBUTE_CONTEXT_ID = "contextId"; //$NON-NLS-1$

	/**
	 * The name of the description attribute, which appears on a scheme
	 * definition.
	 */
	private static final String ATTRIBUTE_DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * The name of the id attribute, which is used on scheme definitions.
	 */
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	/**
	 * The name of the attribute storing the identifier for the active key
	 * configuration identifier. This provides legacy support for the
	 * <code>activeKeyConfiguration</code> element in the commands extension
	 * point.
	 */
	private static final String ATTRIBUTE_KEY_CONFIGURATION_ID = "keyConfigurationId"; //$NON-NLS-1$

	/**
	 * The name of the attribute storing the trigger sequence for a binding.
	 * This is called a 'keySequence' for legacy reasons.
	 */
	private static final String ATTRIBUTE_KEY_SEQUENCE = "keySequence"; //$NON-NLS-1$

	/**
	 * The name of the attribute storing the locale for a binding.
	 */
	private static final String ATTRIBUTE_LOCALE = "locale"; //$NON-NLS-1$

	/**
	 * The name of the name attribute, which appears on scheme definitions.
	 */
	private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$

	/**
	 * The name of the deprecated parent attribute, which appears on scheme
	 * definitions.
	 */
	private static final String ATTRIBUTE_PARENT = "parent"; //$NON-NLS-1$

	/**
	 * The name of the parent id attribute, which appears on scheme definitions.
	 */
	private static final String ATTRIBUTE_PARENT_ID = "parentId"; //$NON-NLS-1$

	/**
	 * The name of the attribute storing the platform for a binding.
	 */
	private static final String ATTRIBUTE_PLATFORM = "platform"; //$NON-NLS-1$

	/**
	 * The name of the attribute storing the identifier for the active scheme.
	 * This is called a 'keyConfigurationId' for legacy reasons.
	 */
	private static final String ATTRIBUTE_SCHEME_ID = ATTRIBUTE_KEY_CONFIGURATION_ID; //$NON-NLS-1$

	/**
	 * The name of the deprecated attribute of the deprecated
	 * <code>activeKeyConfiguration</code> element in the commands extension
	 * point.
	 */
	private static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$

	/**
	 * Whether this class should print out debugging information when it reads
	 * in data, or writes to the preference store.
	 */
	private static final boolean DEBUG = Policy.DEBUG_KEY_BINDINGS;

	/**
	 * The name of the deprecated accelerator configuration element. This
	 * element was used in 2.1.x and earlier to defined groups of what are now
	 * called schemes.
	 */
	private static final String ELEMENT_ACCELERATOR_CONFIGURATION = "acceleratorConfiguration"; //$NON-NLS-1$

	/**
	 * The name of the element storing the active key configuration from the
	 * commands extension point.
	 */
	private static final String ELEMENT_ACTIVE_KEY_CONFIGURATION = "activeKeyConfiguration"; //$NON-NLS-1$

	/**
	 * The name of the element storing the active scheme. This is called a
	 * 'keyConfiguration' for legacy reasons.
	 */
	private static final String ELEMENT_ACTIVE_SCHEME = ELEMENT_ACTIVE_KEY_CONFIGURATION; //$NON-NLS-1$

	/**
	 * The name of the element storing the binding. This is called a
	 * 'keyBinding' for legacy reasons.
	 */
	private static final String ELEMENT_BINDING = "keyBinding"; //$NON-NLS-1$

	/**
	 * The name of the deprecated key configuration element in the commands
	 * extension point. This element has been replaced with the scheme element
	 * in the bindings extension point.
	 */
	private static final String ELEMENT_KEY_CONFIGURATION = "keyConfiguration"; //$NON-NLS-1$

	/**
	 * The name of the deprecated accelerator configurations extension point.
	 */
	private static final String EXTENSION_ACCELERATOR_CONFIGURATIONS = "org.eclipse.ui.acceleratorConfigurations"; //$NON-NLS-1$

	/**
	 * The name of the commands extension point, and the name of the key for the
	 * commands preferences.
	 */
	private static final String EXTENSION_COMMANDS = "org.eclipse.ui.commands"; //$NON-NLS-1$

	/**
	 * The index of the active scheme configuration elements in the indexed
	 * array.
	 * 
	 * @see BindingPersistence#read(BindingManager)
	 */
	private static final int INDEX_ACTIVE_SCHEME = 0;

	/**
	 * The index of the scheme definition configuration elements in the indexed
	 * array.
	 * 
	 * @see BindingPersistence#read(BindingManager)
	 */
	private static final int INDEX_SCHEME_DEFINITIONS = 1;

	/**
	 * Inserts the given element into the indexed two-dimensional array in the
	 * array at the index. The array is grown as necessary.
	 * 
	 * @param elementToAdd
	 *            The element to add to the indexed array; may be
	 *            <code>null</code>
	 * @param indexedArray
	 *            The two-dimensional array that is indexed by element type;
	 *            must not be <code>null</code>.
	 * @param index
	 *            The index at which the element should be added; must be a
	 *            valid index.
	 * @param currentCount
	 *            The current number of items in the array at the index.
	 */
	private static final void addElementToIndexedArray(
			final IConfigurationElement elementToAdd,
			final IConfigurationElement[][] indexedArray, final int index,
			final int currentCount) {
		final IConfigurationElement[] elements;
		if (currentCount == 0) {
			elements = new IConfigurationElement[1];
			indexedArray[index] = elements;
		} else {
			if (currentCount >= indexedArray[index].length) {
				final IConfigurationElement[] copy = new IConfigurationElement[indexedArray[index].length * 2];
				System.arraycopy(indexedArray[index], 0, copy, 0, currentCount);
				elements = copy;
				indexedArray[index] = elements;
			} else {
				elements = indexedArray[index];
			}
		}
		elements[currentCount] = elementToAdd;
	}

	/**
	 * Returns the default scheme identifier for the currently running
	 * application.
	 * 
	 * @return The default scheme identifier (<code>String</code>); never
	 *         <code>null</code>, but may be empty or point to an undefined
	 *         scheme.
	 */
	public static final String getDefaultSchemeId() {
		final IPreferenceStore store = PlatformUI.getPreferenceStore();
		return store
				.getDefaultString(IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID);
	}

	/**
	 * Writes the given active scheme and bindings to the preference store. Only
	 * bindings that are of the <code>Binding.USER</code> type will be
	 * written; the others will be ignored.
	 * 
	 * @param activeScheme
	 *            The scheme which should be persisted; may be <code>null</code>.
	 * @param bindings
	 *            The bindings which should be persisted; may be
	 *            <code>null</code>
	 * @throws IOException
	 *             If something happens while trying to write to the workbench
	 *             preference store.
	 */
	public static final void persist(final Scheme activeScheme,
			final Binding[] bindings) throws IOException {
		// Print out debugging information, if requested.
		if (DEBUG) {
			System.out.println("BINDINGS >> Persisting active scheme '" //$NON-NLS-1$
					+ activeScheme.getId() + "'"); //$NON-NLS-1$
			System.out.println("BINDINGS >> Persisting bindings"); //$NON-NLS-1$
		}

		// Write the simple preference key to the UI preference store.
		writeActiveScheme(activeScheme);

		// Build the XML block for writing the bindings and active scheme.
		final XMLMemento xmlMemento = XMLMemento
				.createWriteRoot(EXTENSION_COMMANDS);
		if (activeScheme != null) {
			writeActiveScheme(xmlMemento, activeScheme);
		}
		if (bindings != null) {
			final int bindingsLength = bindings.length;
			for (int i = 0; i < bindingsLength; i++) {
				final Binding binding = bindings[i];
				if (binding.getType() == Binding.USER) {
					writeBinding(xmlMemento, binding);
				}
			}
		}

		// Write the XML block to the workbench preference store.
		final IPreferenceStore preferenceStore = WorkbenchPlugin.getDefault()
				.getPreferenceStore();
		final Writer writer = new StringWriter();
		try {
			xmlMemento.save(writer);
			preferenceStore.setValue(EXTENSION_COMMANDS, writer.toString());
		} finally {
			writer.close();
		}
	}

	/**
	 * Reads all of the binding information from the registry and from the
	 * preference store.
	 * 
	 * @param bindingManager
	 *            The binding manager which should be populated with the values
	 *            from the registry and preference store; must not be
	 *            <code>null</code>.
	 */
	public static final void read(final BindingManager bindingManager) {
		// Create the extension registry mementos.
		final IExtensionRegistry registry = Platform.getExtensionRegistry();
		int activeSchemeElementCount = 0;
		int schemeDefinitionCount = 0;
		final IConfigurationElement[][] indexedConfigurationElements = new IConfigurationElement[2][];

		// Sort the commands extension point based on element name.
		final IConfigurationElement[] commandsExtensionPoint = registry
				.getConfigurationElementsFor(EXTENSION_COMMANDS);
		for (int i = 0; i < commandsExtensionPoint.length; i++) {
			final IConfigurationElement configurationElement = commandsExtensionPoint[i];
			final String name = configurationElement.getName();

			// Check if it is an active scheme identifier.
			if (ELEMENT_ACTIVE_KEY_CONFIGURATION.equals(name)) {
				addElementToIndexedArray(configurationElement,
						indexedConfigurationElements, INDEX_ACTIVE_SCHEME,
						activeSchemeElementCount++);

				// Check if it is a scheme defintion.
			} else if (ELEMENT_KEY_CONFIGURATION.equals(name)) {
				addElementToIndexedArray(configurationElement,
						indexedConfigurationElements, INDEX_SCHEME_DEFINITIONS,
						schemeDefinitionCount++);
			}
		}

		/*
		 * Sort the accelerator configuration extension point into the scheme
		 * definitions.
		 */
		final IConfigurationElement[] acceleratorConfigurationsExtensionPoint = registry
				.getConfigurationElementsFor(EXTENSION_ACCELERATOR_CONFIGURATIONS);
		for (int i = 0; i < acceleratorConfigurationsExtensionPoint.length; i++) {
			final IConfigurationElement configurationElement = acceleratorConfigurationsExtensionPoint[i];
			final String name = configurationElement.getName();

			// Check if the name matches the accelerator configuration element
			if (ELEMENT_ACCELERATOR_CONFIGURATION.equals(name)) {
				addElementToIndexedArray(configurationElement,
						indexedConfigurationElements, INDEX_SCHEME_DEFINITIONS,
						schemeDefinitionCount++);
			}
		}

		// Create the preference memento.
		final IPreferenceStore store = WorkbenchPlugin.getDefault()
				.getPreferenceStore();
		final String preferenceString = store.getString(EXTENSION_COMMANDS);
		IMemento preferenceMemento = null;
		if ((preferenceString != null) && (preferenceString.length() > 0)) {
			final Reader reader = new StringReader(preferenceString);
			try {
				preferenceMemento = XMLMemento.createReadRoot(reader);
			} catch (final WorkbenchException e) {
				// Could not initialize the preference memento.
			}
		}

		// Read the scheme definitions.
		readSchemes(indexedConfigurationElements[INDEX_SCHEME_DEFINITIONS],
				schemeDefinitionCount, bindingManager);
		readActiveScheme(indexedConfigurationElements[INDEX_ACTIVE_SCHEME],
				activeSchemeElementCount, preferenceMemento, bindingManager);
	}

	/**
	 * <p>
	 * Reads the registry and the preference store, and determines the
	 * identifier for the scheme that should be active. There is a complicated
	 * order of priorities for this. The registry will only be read if there is
	 * no user preference, and the default active scheme id is different than
	 * the default default active scheme id.
	 * </p>
	 * <ol>
	 * <li>A non-default preference.</li>
	 * <li>The legacy preference XML memento.</li>
	 * <li>A default preference value that is different than the default
	 * default active scheme id.</li>
	 * <li>The registry.</li>
	 * <li>The default default active scheme id.</li>
	 * </ol>
	 * 
	 * @param configurationElements
	 *            The configuration elements from the commands extension point;
	 *            must not be <code>null</code>.
	 * @param configurationElementCount
	 *            The number of configuration elements that are really in the
	 *            array.
	 * @param preferences
	 *            The memento wrapping the commands preference key; may be
	 *            <code>null</code>.
	 * @param bindingManager
	 *            The binding manager that should be updated with the active
	 *            scheme. This binding manager must already have its schemes
	 *            defined. This value must not be <code>null</code>.
	 */
	private static final void readActiveScheme(
			final IConfigurationElement[] configurationElements,
			final int configurationElementCount, final IMemento preferences,
			final BindingManager bindingManager) {
		// A non-default preference.
		final IPreferenceStore store = PlatformUI.getPreferenceStore();
		final String defaultActiveSchemeId = store
				.getDefaultString(IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID);
		final String preferenceActiveSchemeId = store
				.getString(IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID);
		if ((preferenceActiveSchemeId != null)
				&& (!preferenceActiveSchemeId.equals(defaultActiveSchemeId))) {
			try {
				bindingManager.setActiveScheme(bindingManager
						.getScheme(preferenceActiveSchemeId));
				return;
			} catch (final NotDefinedException e) {
				// Let's keep looking....
			}
		}

		// A legacy preference XML memento.
		if (preferences != null) {
			final IMemento[] preferenceMementos = preferences
					.getChildren(ELEMENT_ACTIVE_KEY_CONFIGURATION);
			int preferenceMementoCount = preferenceMementos.length;
			for (int i = preferenceMementoCount - 1; i >= 0; i--) {
				final IMemento memento = preferenceMementos[i];
				String id = memento.getString(ATTRIBUTE_KEY_CONFIGURATION_ID);
				if (id != null) {
					try {
						bindingManager.setActiveScheme(bindingManager
								.getScheme(id));
						return;
					} catch (final NotDefinedException e) {
						// Let's keep looking....
					}
				}
			}
		}

		// A default preference value that is different than the default.
		if ((defaultActiveSchemeId != null)
				&& (!defaultActiveSchemeId
						.equals(IBindingService.DEFAULT_DEFAULT_ACTIVE_SCHEME_ID))) {
			try {
				bindingManager.setActiveScheme(bindingManager
						.getScheme(defaultActiveSchemeId));
				return;
			} catch (final NotDefinedException e) {
				// Let's keep looking....
			}
		}

		// The registry.
		for (int i = configurationElementCount - 1; i >= 0; i--) {
			final IConfigurationElement configurationElement = configurationElements[i];

			String id = configurationElement
					.getAttribute(ATTRIBUTE_KEY_CONFIGURATION_ID);
			if (id != null) {
				try {
					bindingManager
							.setActiveScheme(bindingManager.getScheme(id));
					return;
				} catch (final NotDefinedException e) {
					// Let's keep looking....
				}
			}

			id = configurationElement.getAttribute(ATTRIBUTE_VALUE);
			if (id != null) {
				try {
					bindingManager
							.setActiveScheme(bindingManager.getScheme(id));
					return;
				} catch (final NotDefinedException e) {
					// Let's keep looking....
				}
			}
		}

		// The default default active scheme id.
		try {
			bindingManager
					.setActiveScheme(bindingManager
							.getScheme(IBindingService.DEFAULT_DEFAULT_ACTIVE_SCHEME_ID));
		} catch (final NotDefinedException e) {
			// Damn, we're fucked.
			throw new Error("You cannot make something from nothing"); //$NON-NLS-1$
		}
	}

	/**
	 * Reads all of the scheme definitions from the registry.
	 * 
	 * @param configurationElements
	 *            The configuration elements in the commands extension point;
	 *            must not be <code>null</code>, but may be empty.
	 * @param configurationElementCount
	 *            The number of configuration elements that are really in the
	 *            array.
	 * @param bindingManager
	 *            The binding manager to which the schemes should be added.
	 */
	private static final void readSchemes(
			final IConfigurationElement[] configurationElements,
			final int configurationElementCount,
			final BindingManager bindingManager) {
		for (int i = 0; i < configurationElementCount; i++) {
			final IConfigurationElement configurationElement = configurationElements[i];

			// Read out the attributes.
			final String id = configurationElement.getAttribute(ATTRIBUTE_ID);
			final String name = configurationElement
					.getAttribute(ATTRIBUTE_NAME);
			final String description = configurationElement
					.getAttribute(ATTRIBUTE_DESCRIPTION);
			String parentId = configurationElement
					.getAttribute(ATTRIBUTE_PARENT_ID);
			if (parentId == null) {
				parentId = configurationElement.getAttribute(ATTRIBUTE_PARENT);
			}

			// Define the scheme.
			final Scheme scheme = bindingManager.getScheme(id);
			scheme.define(name, description, parentId);
		}
	}

	/**
	 * Writes the active scheme to the memento. If the scheme is
	 * <code>null</code>, then all schemes in the memento are removed.
	 * 
	 * @param memento
	 *            The memento to which the scheme should be written; must not be
	 *            <code>null</code>.
	 * @param scheme
	 *            The scheme that should be written; must not be
	 *            <code>null</code>.
	 */
	private static final void writeActiveScheme(final IMemento memento,
			final Scheme scheme) {
		// Add this active scheme, if it is not the default.
		final IPreferenceStore store = PlatformUI.getPreferenceStore();
		final String schemeId = scheme.getId();
		final String defaultSchemeId = store
				.getDefaultString(IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID);
		if ((defaultSchemeId == null) ? (schemeId != null) : (!defaultSchemeId
				.equals(schemeId))) {
			final IMemento child = memento.createChild(ELEMENT_ACTIVE_SCHEME);
			child.putString(ATTRIBUTE_SCHEME_ID, schemeId);
		}
	}

	/**
	 * Writes the active scheme to its own preference key. This key is used by
	 * RCP applications as part of their plug-in customization.
	 * 
	 * @param scheme
	 *            The scheme to write to the preference store. If the scheme is
	 *            <code>null</code>, then it is removed.
	 */
	private static final void writeActiveScheme(final Scheme scheme) {
		final IPreferenceStore store = PlatformUI.getPreferenceStore();
		final String schemeId = (scheme == null) ? null : scheme.getId();
		final String defaultSchemeId = store
				.getDefaultString(IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID);
		if ((defaultSchemeId == null) ? (scheme != null) : (!defaultSchemeId
				.equals(schemeId))) {
			store.setValue(IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID,
					scheme.getId());
		} else {
			store
					.setToDefault(IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID);
		}
	}

	/**
	 * Writes the binding to the memento. This creates a new child element on
	 * the memento, and places the properties of the binding as its attributes.
	 * 
	 * @param parent
	 *            The parent memento for the binding element; must not be
	 *            <code>null</code>.
	 * @param binding
	 *            The binding to write; must not be <code>null</code>.
	 */
	private static final void writeBinding(final IMemento parent,
			final Binding binding) {
		final IMemento element = parent.createChild(ELEMENT_BINDING);
		element.putString(ATTRIBUTE_CONTEXT_ID, binding.getContextId());
		element.putString(ATTRIBUTE_COMMAND_ID, binding.getCommandId());
		element.putString(ATTRIBUTE_SCHEME_ID, binding.getSchemeId());
		element.putString(ATTRIBUTE_KEY_SEQUENCE, binding.getTriggerSequence()
				.toString());
		element.putString(ATTRIBUTE_LOCALE, binding.getLocale());
		element.putString(ATTRIBUTE_PLATFORM, binding.getPlatform());
	}

	/**
	 * This class should not be constructed.
	 */
	private BindingPersistence() {
		// Should not be called.
	}
}
