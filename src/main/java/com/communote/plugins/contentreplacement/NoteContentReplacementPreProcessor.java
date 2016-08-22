package com.communote.plugins.contentreplacement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.communote.common.util.Orderable;
import com.communote.plugins.core.services.PluginPropertyService;
import com.communote.plugins.core.services.PluginPropertyServiceException;
import com.communote.server.api.core.note.processor.NoteContentRenderingPreProcessor;
import com.communote.server.api.util.JsonHelper;
import com.communote.server.core.security.SecurityHelper;
import com.communote.server.api.core.note.NoteData;
import com.communote.server.api.core.note.NoteRenderContext;
import com.communote.server.api.core.note.NoteRenderMode;

/**
 * Replaces a specified string in the note.
 * 
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
@Component
@Instantiate(name = "NoteContentReplacementPreProcessor")
@Provides
public class NoteContentReplacementPreProcessor implements NoteContentRenderingPreProcessor, Orderable {

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NoteContentReplacementPreProcessor.class);

    @Requires
    private PluginPropertyService pluginPropertyService;

    /** Key for the content replacement definition. */
    public final static String CONTENT_REPLACEMENT_DEFINITION = "contentReplacementDefinition";

    /**
     * Embed the external content in a note.
     * 
     * @param item
     *            the list item containing the note details
     * @param replacementDefinitions
     *            the external content definition
     */
    private void embedExternalContent(NoteData item,
            List<ContentReplacementDefinition> replacementDefinitions) {
        for (ContentReplacementDefinition replacementDefinition : replacementDefinitions) {
            String expression = replacementDefinition.getExpression();
            Pattern pattern = null;
            try {
                pattern = Pattern.compile(expression);
            } catch (PatternSyntaxException e) {
                LOGGER.error(e.getMessage(), e);
                continue;
            }
            String replacement = replaceUsernamePlaceholder(replacementDefinition.getReplacement());
            item.setContent(replaceInContent(item.getContent(), pattern, replacement,
                    replacementDefinition.isInline()));
            item.setShortContent(replaceInContent(item.getShortContent(), pattern, replacement,
                    replacementDefinition.isInline()));
        }
    }

    /**
     * Does the actual replacement inside the content string.
     * 
     * @param content
     *            the content string where some parts should be replaced. Can be null.
     * @param pattern
     *            the pattern to use for matching
     * @param replacement
     *            the replacement to apply when a match is found
     * @param inline
     *            whether to replace directly inside the content or append the match after applying
     *            the replacement to the end
     * @return the possibly modified content or null if content was null
     */
    private String replaceInContent(String content, Pattern pattern, String replacement,
            boolean inline) {
        if (content == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(content);
        if (inline) {
            return matcher.replaceAll(replacement);
        } else {
            String newContent = content;
            while (matcher.find()) {
                newContent = newContent + " "
                        + pattern.matcher(matcher.group()).replaceAll(replacement);
            }
            return newContent;
        }
    }

    @Override
    public boolean isCachable() {
        // must return false here because of the $username replacement feature :(
        // TODO could make it cachable if we cache the definitions and check for the
        // username-placeholder. If not contained the result would be cachable. Would also need an
        // event to inform the PreProcessor manager that the cached content needs to be flushed if
        // the definitions were modified so that the result becomes cachable/uncachable.
        return false;
    }

    /**
     * Get the existing list of content replacement definitions.
     * 
     * @return the existing list of content replacement definitions
     * @throws PluginPropertyServiceException
     *             the exception
     */
    private List<ContentReplacementDefinition> getContentReplacementDefinitionList()
            throws PluginPropertyServiceException {
        // get the existing list from the cache or db in a JSON string representation
        String jsonRepresantation = pluginPropertyService
                .getClientProperty(NoteContentReplacementPreProcessor.CONTENT_REPLACEMENT_DEFINITION);
        ObjectMapper mapper = JsonHelper.getSharedObjectMapper();
        List<ContentReplacementDefinition> definitionList = new ArrayList<ContentReplacementDefinition>();
        try {
            // convert the JSON string representation to a list of content replacement definitions
            if (jsonRepresantation != null) {
                definitionList = mapper.readValue(jsonRepresantation,
                        new TypeReference<List<ContentReplacementDefinition>>() {
                        });
            }
        } catch (JsonParseException e) {
            LOGGER.debug(e.getMessage(), e);
        } catch (JsonMappingException e) {
            LOGGER.debug(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return definitionList;
    }

    /**
     * @return {@link NoteRenderingPreProcessor#DEFAULT_ORDER} + 1 to process the node before the
     *         atlr plugin
     */
    @Override
    public int getOrder() {
        return DEFAULT_ORDER + 1;
    }

    @Override
    public boolean processNoteContent(NoteRenderContext context, NoteData item) {
        List<ContentReplacementDefinition> replacementDefinitions;
        try {
            replacementDefinitions = getContentReplacementDefinitionList();
            if (replacementDefinitions != null) {
                embedExternalContent(item, replacementDefinitions);
            }

        } catch (PluginPropertyServiceException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * @return true.
     */
    @Override
    public boolean replacesContent() {
        return false;
    }

    /**
     * Replace the special username placeholder in the content
     * 
     * @param replacement
     *            the replacement text
     * @return the content with the replaced username placeholder
     */
    private String replaceUsernamePlaceholder(String replacement) {
        String currentUserAlias = SecurityHelper.getCurrentUserAlias();
        if (currentUserAlias == null) {
            currentUserAlias = SecurityHelper.isPublicUser() ? "Anonymous" : "unknown";
        }
        String newContent = replacement.replace("$username", currentUserAlias);
        return newContent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(NoteRenderMode mode, NoteData note) {
        return NoteRenderMode.HTML.equals(mode) || NoteRenderMode.PORTAL.equals(mode);
    }
}
