/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.lsp.eclipse.xml.completion;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.lsp4e.operations.completion.LSContentAssistProcessor;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;

public class CamelURICompletionProposalComputer implements ICompletionProposalComputer {

	@Override
	public List<ICompletionProposal> computeCompletionProposals(CompletionProposalInvocationContext context, IProgressMonitor monitor) {
		return Arrays.asList(new LSContentAssistProcessor().computeCompletionProposals(context.getViewer(), context.getInvocationOffset()));
	}

	@Override
	public List<IContextInformation> computeContextInformation(CompletionProposalInvocationContext context, IProgressMonitor monitor) {
		return Arrays.asList(new LSContentAssistProcessor().computeContextInformation(context.getViewer(), context.getInvocationOffset()));
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionEnded() {
		// nothing to do
	}

	@Override
	public void sessionStarted() {
		// nothing to do
	}

}
