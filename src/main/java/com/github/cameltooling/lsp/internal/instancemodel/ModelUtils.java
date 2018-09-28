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

/**
 * @author lheinema
 *
 */
public class ModelUtils {

	private static final List<String> POSSIBLE_REFERENCES = Arrays.asList("direct","direct-vm", "seda", "vm");

	private ModelUtils() {
		// util class
	}
	
	public static boolean isReferenceComponentKind(CamelUriElementInstance camelURIInstanceToSearchReference) {
		return POSSIBLE_REFERENCES.contains(camelURIInstanceToSearchReference.getComponentName());
	}
}
