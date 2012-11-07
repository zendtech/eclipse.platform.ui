/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.e4.ui.workbench.modeling;

import java.lang.annotation.Annotation;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.contributions.IContributionFactory;
import org.eclipse.e4.ui.model.application.ui.MLifecycledElement;

/**
 * Utility methods
 */
public class LifecycleHelper {
	/**
	 * Invoke all lifecycle listeners
	 * 
	 * @param modelService
	 *            the model service
	 * @param element
	 *            the lifecycle element
	 * @param annotation
	 *            the annotation to invoke
	 */
	public static void invokeLifecycleHandlers(EModelService modelService,
			MLifecycledElement element, Class<? extends Annotation> annotation) {
		if (!element.getLifecyleURIs().isEmpty()) {
			IEclipseContext modelContext = modelService.getContainingContext(element);
			IContributionFactory contributionFactory = (IContributionFactory) modelContext
					.get(IContributionFactory.class.getName());

			for (String u : element.getLifecyleURIs()) {
				Object o = element.getTransientData().get(u);
				if (o == null) {
					o = contributionFactory.create(u, modelContext);
					element.getTransientData().put(u, o);
				}
				ContextInjectionFactory.invoke(o, annotation, modelContext, null);
			}
		}
	}

	/**
	 * Invoke all lifecycle listeners
	 * 
	 * @param modelService
	 *            the model service
	 * @param element
	 *            the lifecycle element
	 * @param annotation
	 *            the annotation to invoke
	 * @return collected boolean value
	 */
	public static boolean invokeBooleanLifecycleHandlers(EModelService modelService,
			MLifecycledElement element, Class<? extends Annotation> annotation) {
		boolean rv = true;
		if (!element.getLifecyleURIs().isEmpty()) {
			IEclipseContext modelContext = modelService.getContainingContext(element);
			IContributionFactory contributionFactory = (IContributionFactory) modelContext
					.get(IContributionFactory.class.getName());

			for (String u : element.getLifecyleURIs()) {
				Object o = element.getTransientData().get(u);
				if (o == null) {
					o = contributionFactory.create(u, modelContext);
					element.getTransientData().put(u, o);
				}
				rv &= (Boolean) ContextInjectionFactory.invoke(o, annotation, modelContext,
						Boolean.TRUE);
			}
		}
		return rv;
	}
}
