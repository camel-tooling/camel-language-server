/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.completion;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static com.github.cameltooling.lsp.internal.util.RouteTextBuilder.createXMLBlueprintRoute;
import static org.assertj.core.api.Assertions.assertThat;

class TwitterTimelineCompletionTest extends AbstractCamelLanguageServerTest {

    @Test
    void testTwitterTimelineCompletionForLang() throws Exception {
        CamelLanguageServer languageServer =
                initializeLanguageServer(createXMLBlueprintRoute("twitter-timeline:HOME?lang="), ".xml");
		List<CompletionItem> completions =
                getCompletionFor(languageServer, new Position(0, 38)).get().getLeft();

        // It should return the full list of ISO_639-1
        // which is the two-letter codes for languages
		assertThat(completions)
				.isNotEmpty()
                //We will just rely on Java to be updated for this
                .hasSize(Locale.getISOLanguages().length)
                .allMatch(item -> item.getLabel().length() == 2);
    }
}
