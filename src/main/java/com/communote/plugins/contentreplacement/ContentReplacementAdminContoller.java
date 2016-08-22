package com.communote.plugins.contentreplacement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.Controller;

import com.communote.plugins.core.services.PluginPropertyService;
import com.communote.plugins.core.services.PluginPropertyServiceException;
import com.communote.plugins.core.views.AdministrationViewController;
import com.communote.plugins.core.views.ViewControllerException;
import com.communote.plugins.core.views.annotations.Page;
import com.communote.plugins.core.views.annotations.UrlMapping;
import com.communote.server.api.util.JsonHelper;
import com.communote.server.web.commons.MessageHelper;


/**
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
@Component
@Provides
@Instantiate(name = "ContentReplacementAdminContoller")
@UrlMapping(value = "/*/admin/application/extensions/content-replacement")
@Page(menu = "extensions", submenu = "content", 
menuMessageKey = "administration.title.submenu.content")
public class ContentReplacementAdminContoller extends AdministrationViewController implements Controller {
    
    @Requires
    private PluginPropertyService pluginPropertyService;

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentReplacementAdminContoller.class);

    /**
     * Constructor.
     * 
     * @param bundleContext
     *            The bundles context.
     */
    public ContentReplacementAdminContoller(BundleContext bundleContext) {
        super(bundleContext);
        LOGGER.info("Content Integration: configuration controller instantiated.");
    }

    /**
     * Add a content replacement definition to the existing list of content replacement definitions.
     * 
     * @param definition
     *            the content replacement definition
     * @return the list of content replacement definitions with the added content replacement
     *         definition
     * @throws PluginPropertyServiceException
     *             the exception
     */
    private List<ContentReplacementDefinition> addContentReplacementDefinition(
            ContentReplacementDefinition definition) throws PluginPropertyServiceException {
        // get the existing list from the cache or db
        List<ContentReplacementDefinition> definitionList = getContentReplacementDefinitionList();
        // add the given definition to the list
        definitionList.add(definition);
        return definitionList;
    }

    /**
     * Convert a list of content replacement definitions to a JSON string representation.
     * 
     * @param definitionList
     *            the list of content replacement definitions
     * @return the JSON string representation of the given list
     */
    private String convertContentReplacementDefinitionToJson(
            List<ContentReplacementDefinition> definitionList) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonRepresentation = null;
        try {
            // convert the given list of content replacement definitions to a JSON string
            // representation
            jsonRepresentation = mapper.writeValueAsString(definitionList);
        } catch (JsonGenerationException e) {
            LOGGER.debug(e.getMessage(), e);
        } catch (JsonMappingException e) {
            LOGGER.debug(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return jsonRepresentation;
    }

    /**
     * Delete the definition with the given position in the list of content replacement definitions.
     * 
     * @param position
     *            the position in the list of content replacement definitions
     * @param request
     *            the request
     * @throws PluginPropertyServiceException
     *             the exception
     */
    private void deleteDefinition(String position, HttpServletRequest request)
            throws PluginPropertyServiceException {
        int index = new Integer(position);
        // get the list of definitions
        List<ContentReplacementDefinition> definitionList = getContentReplacementDefinitionList();
        // remove the definition at the given position
        definitionList.remove(index - 1);
        String jsonRepresentation = convertContentReplacementDefinitionToJson(definitionList);
        // store the manipulated list of definitions
        pluginPropertyService.setClientProperty(
                NoteContentReplacementPreProcessor.CONTENT_REPLACEMENT_DEFINITION, jsonRepresentation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> model) throws ViewControllerException {
        try {
            model.put("replacementDefinitionList", getContentReplacementDefinitionList());
        } catch (PluginPropertyServiceException e) {
            throw new ViewControllerException(500, e.getMessage(), e);
        }
        model.put("allProperties", pluginPropertyService.getAllClientProperties());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> model) throws ViewControllerException {
        try {
            if (StringUtils.isNotBlank(request.getParameter("delete"))) {
                deleteDefinition(request.getParameter("id"), request);
            } else {
                // get the parameter from the request
                String name = request.getParameter("name");
                String expression = request.getParameter("expression");
                String replacement = request.getParameter("replacement");
                String isDisableInline = request.getParameter("disableInline");
                if (StringUtils.isBlank(name) || StringUtils.isBlank(expression)
                        || StringUtils.isBlank(expression)) {
                    MessageHelper.saveErrorMessageFromKey(request, "form.info.required.fields");
                } else {
                    // create a new content replacement definition
                    ContentReplacementDefinition definition = new ContentReplacementDefinition();
                    definition.setName(name);
                    definition.setReplacement(replacement);
                    // TODO validate the RegEx!
                    definition.setExpression(expression);
                    definition.setInline(isDisableInline == null);
                    // store the definitions in the database
                    List<ContentReplacementDefinition> definitionList = addContentReplacementDefinition(definition);
                    String jsonRepresentation = convertContentReplacementDefinitionToJson(definitionList);
                    pluginPropertyService.setClientProperty(
                            NoteContentReplacementPreProcessor.CONTENT_REPLACEMENT_DEFINITION,
                            jsonRepresentation);
                }
            }
        } catch (PluginPropertyServiceException e) {
            throw new ViewControllerException(500, e.getMessage(), e);
        }
        doGet(request, response, model);
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
     * @return "/vm/content-integration/admin-content.html.vm"
     */
    @Override
    public String getContentTemplate() {
        return "/vm/content-integration/admin-content.html.vm";
    }
}
