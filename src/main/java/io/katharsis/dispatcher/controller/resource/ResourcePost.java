package io.katharsis.dispatcher.controller.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.katharsis.dispatcher.controller.HttpMethod;
import io.katharsis.queryParams.RequestParams;
import io.katharsis.repository.ResourceRepository;
import io.katharsis.request.dto.DataBody;
import io.katharsis.request.dto.LinkageData;
import io.katharsis.request.dto.RequestBody;
import io.katharsis.request.path.JsonPath;
import io.katharsis.request.path.ResourcePath;
import io.katharsis.resource.exception.RequestBodyException;
import io.katharsis.resource.exception.RequestBodyNotFoundException;
import io.katharsis.resource.exception.ResourceNotFoundException;
import io.katharsis.resource.field.ResourceField;
import io.katharsis.resource.registry.RegistryEntry;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.response.LinksInformation;
import io.katharsis.response.MetaInformation;
import io.katharsis.response.ResourceResponse;
import io.katharsis.utils.Generics;
import io.katharsis.utils.PropertyUtils;
import io.katharsis.utils.parser.TypeParser;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ResourcePost extends ResourceUpsert {

    public ResourcePost(ResourceRegistry resourceRegistry, TypeParser typeParser, ObjectMapper objectMapper) {
        super(resourceRegistry, typeParser, objectMapper);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Check if it is a POST request for a resource.
     */
    @Override
    public boolean isAcceptable(JsonPath jsonPath, String requestType) {
        return jsonPath.isCollection() &&
                jsonPath instanceof ResourcePath &&
                HttpMethod.POST.name().equals(requestType);
    }

    @Override
    public ResourceResponse handle(JsonPath jsonPath, RequestParams requestParams, RequestBody requestBody)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
        IOException {
        String resourceEndpointName = jsonPath.getResourceName();
        RegistryEntry endpointRegistryEntry = resourceRegistry.getEntry(resourceEndpointName);
        if (endpointRegistryEntry == null) {
            throw new ResourceNotFoundException(resourceEndpointName);
        }
        if (requestBody == null) {
            throw new RequestBodyNotFoundException(HttpMethod.POST, resourceEndpointName);
        }
        if (requestBody.isMultiple()) {
            throw new RequestBodyException(HttpMethod.POST, resourceEndpointName, "Multiple data in body");
        }

        DataBody dataBody = requestBody.getSingleData();
        if (dataBody == null) {
            throw new RequestBodyException(HttpMethod.POST, resourceEndpointName, "No data field in the body.");
        }
        RegistryEntry bodyRegistryEntry = resourceRegistry.getEntry(dataBody.getType());
        verifyTypes(HttpMethod.POST, resourceEndpointName, endpointRegistryEntry, bodyRegistryEntry);
        Object newResource = bodyRegistryEntry.getResourceInformation().getResourceClass().newInstance();

        setAttributes(dataBody, newResource, bodyRegistryEntry.getResourceInformation());
        ResourceRepository resourceRepository = endpointRegistryEntry.getResourceRepository();
        setRelations(newResource, bodyRegistryEntry, dataBody, requestParams);
        Object savedResource = resourceRepository.save(newResource);

        Serializable resourceId = (Serializable) PropertyUtils
            .getProperty(savedResource, bodyRegistryEntry.getResourceInformation().getIdField().getName());

        @SuppressWarnings("unchecked")
        Object savedResourceWithRelations = resourceRepository.findOne(resourceId, requestParams);
        MetaInformation metaInformation =
            getMetaInformation(resourceRepository, Collections.singletonList(savedResourceWithRelations), requestParams);
        LinksInformation linksInformation =
            getLinksInformation(resourceRepository, Collections.singletonList(savedResourceWithRelations), requestParams);

        return new ResourceResponse(savedResourceWithRelations, jsonPath, requestParams, metaInformation, linksInformation);
    }

    void setRelations(Object newResource, RegistryEntry registryEntry, DataBody dataBody, RequestParams requestParams)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (dataBody.getRelationships() != null) {
            Map<String, Object> additionalProperties = dataBody.getRelationships().getAdditionalProperties();
            for (Map.Entry<String, Object> property : additionalProperties.entrySet()) {
                if (Iterable.class.isAssignableFrom(property.getValue().getClass())) {
                    //noinspection unchecked
                    setRelationsField(newResource, registryEntry, (Map.Entry) property, requestParams);
                } else {
                    //noinspection unchecked
                    setRelationField(newResource, registryEntry, (Map.Entry) property, requestParams);
                }

            }
        }
    }

    private void setRelationsField(Object newResource, RegistryEntry registryEntry,
                                   Map.Entry<String, Iterable<LinkageData>> property, RequestParams requestParams) {
        String propertyName = property.getKey();
        ResourceField relationshipField = registryEntry.getResourceInformation().findRelationshipFieldByName(propertyName);
        Class<?> relationshipFieldClass = Generics.getResourceClass(relationshipField.getGenericType(), relationshipField.getType());
        RegistryEntry entry = resourceRegistry.getEntry(relationshipFieldClass);
        Class idFieldType = entry.getResourceInformation().getIdField().getType();

        List<Serializable> relationshipIds = new LinkedList<>();
        for (LinkageData linkageData : property.getValue()) {
            Serializable castedRelationshipId = typeParser.parse(linkageData.getId(), idFieldType);
            relationshipIds.add(castedRelationshipId);
        }
        Iterable relationObjects = entry.getResourceRepository().findAll(relationshipIds, requestParams);
        PropertyUtils.setProperty(newResource, propertyName, relationObjects);
    }

    private void setRelationField(Object newResource, RegistryEntry registryEntry,
                                  Map.Entry<String, LinkageData> property, RequestParams requestParams) {
        String propertyName = property.getKey();
        ResourceField relationshipFieldByName = registryEntry.getResourceInformation().findRelationshipFieldByName(propertyName);
        RegistryEntry entry = resourceRegistry.getEntry(relationshipFieldByName.getType());
        Class idFieldType = entry.getResourceInformation().getIdField().getType();
        Serializable castedRelationshipId = typeParser.parse(property.getValue().getId(), idFieldType);

        Object relationObject = entry.getResourceRepository().findOne(castedRelationshipId, requestParams);
        PropertyUtils.setProperty(newResource, propertyName, relationObject);
    }
}
