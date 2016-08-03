package io.katharsis.jackson.serializer;

import io.katharsis.queryParams.include.Inclusion;
import io.katharsis.request.path.ResourcePath;
import io.katharsis.resource.annotations.JsonApiIncludeByDefault;
import io.katharsis.resource.field.ResourceField;
import io.katharsis.resource.information.ResourceInformation;
import io.katharsis.resource.registry.RegistryEntry;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.response.BaseResponse;
import io.katharsis.response.Container;
import io.katharsis.utils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts inclusions from a resource.
 */
public class IncludedRelationshipExtractor {
    private static final Logger logger = LoggerFactory.getLogger(IncludedRelationshipExtractor.class);
    private final ResourceRegistry resourceRegistry;

    public IncludedRelationshipExtractor(ResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry;
    }

    public Set<?> extractIncludedResources(Object resource, BaseResponse response) {
        Set includedResources = new HashSet<>();
        //noinspection unchecked
        includedResources.addAll(extractDefaultIncludedFields(resource, response));
        try {
            //noinspection unchecked
            includedResources.addAll(extractIncludedRelationships(resource, response));
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
            logger.info("Exception while extracting included fields", e);
        }

        return includedResources;
    }

    private List<?> extractDefaultIncludedFields(Object resource, BaseResponse response) {
        List<?> includedResources = getIncludedByDefaultResources(resource, 1);

        return includedResources
                .stream()
                .map(includedResource -> new Container(includedResource, response.getRequestParams()))
                .collect(Collectors.toList());
    }


    private List<?> getIncludedByDefaultResources(Object resource, int recurrenceLevel) {
        if (recurrenceLevel >= 42 || resource == null) {
            return Collections.emptyList();
        }

        Set<ResourceField> relationshipFields = getRelationshipFields(resource);
        List includedFields = new LinkedList<>();

        //noinspection unchecked
        for (ResourceField resourceField : relationshipFields) {
            if (resourceField.isAnnotationPresent(JsonApiIncludeByDefault.class)) {

                Object targetDataObj = PropertyUtils.getProperty(resource, resourceField.getName());

                if (targetDataObj != null) {
                    recurrenceLevel++;

                    if (targetDataObj instanceof Iterable) {
                        for (Object objectItem : (Iterable) targetDataObj) {
                            //noinspection unchecked
                            includedFields.add(objectItem);
                            //noinspection unchecked
                            includedFields.addAll(getIncludedByDefaultResources(objectItem, recurrenceLevel));
                        }
                    } else {
                        //noinspection unchecked
                        includedFields.add(targetDataObj);
                        //noinspection unchecked
                        includedFields.addAll(getIncludedByDefaultResources(targetDataObj, recurrenceLevel));
                    }
                }
            }
        }

        return includedFields;
    }

    private List<?> extractIncludedRelationships(Object resource, BaseResponse response)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        List<?> includedResources = new LinkedList<>();
        List<Inclusion> includedRelations = response.getRequestParams().getIncludedRelations();
        if (includedRelations != null) {
            for (Inclusion inclusion : includedRelations) {
                //noinspection unchecked
                includedResources.addAll(extractIncludedRelationship(resource, inclusion, response));
            }
        }
        return includedResources;
    }

    private Set extractIncludedRelationship(Object resource, Inclusion inclusion, BaseResponse response)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        List<String> pathList = inclusion.getPathList();
        if (resource == null || pathList.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        return getElements(resource, pathList, response);
    }

    private Set getElements(Object resource, List<String> pathList, BaseResponse response)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        Set elements = new HashSet();
        if (pathList.isEmpty()) {
            if (resource != null) {
                return Collections.singleton(new Container(resource, response.getRequestParams()));
            } else {
                return Collections.emptySet();
            }
        }
        Object property = PropertyUtils.getProperty(resource, pathList.get(0));
        if (property != null) {
            List<String> subPathList = pathList.subList(1, pathList.size());
            if (Iterable.class.isAssignableFrom(property.getClass())) {
                for (Object o : ((Iterable) property)) {
                    //noinspection unchecked
                    elements.addAll(getElements(o, subPathList, response));
                }
            } else {
                //noinspection unchecked
                elements.addAll(getElements(property, subPathList, response));
            }
        } else {
            return Collections.emptySet();
        }
        return elements;
    }

    private Set<ResourceField> getRelationshipFields(Object resource) {
        Class<?> dataClass = resource.getClass();
        RegistryEntry entry = resourceRegistry.getEntry(dataClass);
        ResourceInformation resourceInformation = entry.getResourceInformation();
        return resourceInformation.getRelationshipFields();
    }
}
