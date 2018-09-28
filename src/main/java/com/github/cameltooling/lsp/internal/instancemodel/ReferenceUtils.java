/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.github.cameltooling.lsp.internal.instancemodel;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author lheinema
 *
 */
public class ReferenceUtils {

	private static final List<String> POSSIBLE_REFERENCES = Arrays.asList("direct","direct-vm", "seda", "vm");

	private ReferenceUtils() {
		// util class
	}
	
	public static boolean isReferenceComponentKind(CamelUriElementInstance camelURIInstanceToSearchReference) {
		return POSSIBLE_REFERENCES.contains(camelURIInstanceToSearchReference.getComponentName());
	}
	
	/**
	 * @param camelDirectURIInstance
	 * @return the first path parameter which is the reference key for elements listed in com.github.cameltooling.lsp.internal.instancemodel.ReferenceUtils.POSSIBLE_REFERENCES
	 */
	public static String getReferenceKey(CamelURIInstance camelDirectURIInstance) {
		Set<PathParamURIInstance> pathParams = camelDirectURIInstance.getComponentAndPathUriElementInstance().getPathParams();
		if (!pathParams.isEmpty()) {
			return pathParams.iterator().next().getValue();
		}
		return null;
	}
}
