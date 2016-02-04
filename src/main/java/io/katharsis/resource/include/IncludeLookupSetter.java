package io.katharsis.resource.include;

import io.katharsis.queryParams.RequestParams;
import io.katharsis.queryParams.include.Inclusion;
import io.katharsis.repository.MetaRepository;
import io.katharsis.repository.RelationshipRepository;
import io.katharsis.repository.RepositoryMethodParameterProvider;
import io.katharsis.repository.exception.RelationshipRepositoryNotFoundException;
import io.katharsis.resource.annotations.JsonApiLookupIncludeAutomatically;
import io.katharsis.resource.field.ResourceField;
import io.katharsis.resource.registry.RegistryEntry;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.response.MetaDataEnabledList;
import io.katharsis.response.MetaInformation;
import io.katharsis.utils.ClassUtils;
import io.katharsis.utils.Generics;
import io.katharsis.utils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Created by zachncst on 10/14/15.
 */
public class IncludeLookupSetter {
    private static final transient Logger logger = LoggerFactory.getLogger(IncludeLookupSetter.class);

    private final ResourceRegistry resourceRegistry;

    public IncludeLookupSetter(ResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry;
    }

    public void setIncludedElements(Object resource, RequestParams requestParams,
                                    RepositoryMethodParameterProvider parameterProvider)
            throws InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        if (resource != null && requestParams.getIncludedRelations() != null) {
            if (Iterable.class.isAssignableFrom(resource.getClass())) {
                StreamSupport.stream(((Iterable<?>) resource).spliterator(), true)
                        .forEach((target) -> {
                            try {
                                setIncludedElements(target, requestParams, parameterProvider);
                            } catch (InvocationTargetException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
                                logger.error("Error with spliterator", e);
                            }
                        });
            } else {
                for (Inclusion inclusion : requestParams.getIncludedRelations()) {
                    List<String> pathList = inclusion.getPathList();
                    if (resource != null && !pathList.isEmpty()) {
                        getElements(resource, pathList, requestParams, parameterProvider);
                    }
                }
            }
        }
    }

    private void getElements(Object resource, List<String> pathList, RequestParams requestParams,
                             RepositoryMethodParameterProvider parameterProvider)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        if (!pathList.isEmpty()) {
            Field field = ClassUtils.findClassField(resource.getClass(), pathList.get(0));
            if (field == null) {
                logger.warn("Error loading relationship, couldn't find field " + pathList.get(0));
                return;
            }
            Object property = PropertyUtils.getProperty(resource, field.getName());
            //attempt to load relationship if it's null
            if (property == null && field.isAnnotationPresent(JsonApiLookupIncludeAutomatically.class)) {
                try {
                    property = loadRelationship(resource, field, requestParams, parameterProvider);

                    if(property instanceof MetaDataEnabledList) {
                        ((MetaDataEnabledList) property).setMetaInformation(loadRelationshipMetaData(resource, field, requestParams, parameterProvider));
                    }

                    PropertyUtils.setProperty(resource, field.getName(), property);
                } catch( Exception e ) {
                    logger.error("Error loading relationship, couldn't automatically include", e);
                }
            }

            if (property != null) {
                List<String> subPathList = pathList.subList(1, pathList.size());
                if (Iterable.class.isAssignableFrom(property.getClass())) {
                    for (Object o : ((Iterable) property)) {
                        //noinspection unchecked
                        getElements(o, subPathList, requestParams, parameterProvider);
                    }
                } else {
                    //noinspection unchecked
                    getElements(property, subPathList, requestParams, parameterProvider);
                }
            }
        }
    }

    private MetaInformation loadRelationshipMetaData(Object root, Field relationshipField, RequestParams requestParams,
                                                     RepositoryMethodParameterProvider parameterProvider) {
        Class<?> resourceClass = getClassFromField(relationshipField);
        RegistryEntry<?> rootEntry = resourceRegistry.getEntry(root.getClass());
        RegistryEntry<?> registryEntry = resourceRegistry.getEntry(resourceClass);

        if (rootEntry == null || registryEntry == null) {
            return null;
        }

        ResourceField rootIdField = rootEntry.getResourceInformation().getIdField();
        Serializable castedResourceId = (Serializable) PropertyUtils.getProperty(root, rootIdField.getName());

        Class<?> baseRelationshipFieldClass = relationshipField.getType();
        Class<?> relationshipFieldClass = Generics.getResourceClass(root.getClass(), resourceClass);

        try {
            RelationshipRepository relationshipRepositoryForClass = rootEntry
                    .getRelationshipRepositoryForClass(relationshipFieldClass, parameterProvider);
            if (relationshipRepositoryForClass != null) {
                if (Iterable.class.isAssignableFrom(baseRelationshipFieldClass)) {
                    return ((MetaRepository)relationshipRepositoryForClass).getMetaInformation(Collections.singletonList(relationshipRepositoryForClass), requestParams);
                }
            }
        } catch (RelationshipRepositoryNotFoundException e) {
            logger.debug("Relationship is not defined", e);
        }

        return null;
    }

    private Object loadRelationship(Object root, Field relationshipField, RequestParams requestParams,
                                    RepositoryMethodParameterProvider parameterProvider) {
        Class<?> resourceClass = getClassFromField(relationshipField);
        RegistryEntry<?> rootEntry = resourceRegistry.getEntry(root.getClass());
        RegistryEntry<?> registryEntry = resourceRegistry.getEntry(resourceClass);

        if (rootEntry == null || registryEntry == null) {
            return null;
        }

        ResourceField rootIdField = rootEntry.getResourceInformation().getIdField();
        Serializable castedResourceId = (Serializable) PropertyUtils.getProperty(root, rootIdField.getName());

        Class<?> baseRelationshipFieldClass = relationshipField.getType();
        Class<?> relationshipFieldClass = Generics.getResourceClass(root.getClass(), resourceClass);

        try {
            RelationshipRepository relationshipRepositoryForClass = rootEntry
                .getRelationshipRepositoryForClass(relationshipFieldClass, parameterProvider);
            if (relationshipRepositoryForClass != null) {
                if (Iterable.class.isAssignableFrom(baseRelationshipFieldClass)) {
                    return relationshipRepositoryForClass.findManyTargets(castedResourceId, relationshipField.getName(), requestParams);
                } else {
                    return relationshipRepositoryForClass.findOneTarget(castedResourceId, relationshipField.getName(), requestParams);
                }
            }
        } catch (RelationshipRepositoryNotFoundException e) {
            logger.debug("Relationship is not defined", e);
        }

        return null;
    }

    private Class<?> getClassFromField(Field relationshipField) {
        Class<?> resourceClass = null;
        if (Iterable.class.isAssignableFrom(relationshipField.getType())) {
            ParameterizedType stringListType = (ParameterizedType) relationshipField.getGenericType();
            resourceClass = (Class<?>) stringListType.getActualTypeArguments()[0];
        } else {
            resourceClass = relationshipField.getType();
        }
        return resourceClass;
    }
}
