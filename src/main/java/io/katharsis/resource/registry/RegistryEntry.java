package io.katharsis.resource.registry;

import io.katharsis.repository.RelationshipRepository;
import io.katharsis.repository.ResourceRepository;
import io.katharsis.repository.exception.RelationshipRepositoryNotFoundException;
import io.katharsis.resource.information.ResourceInformation;
import net.jodah.typetools.TypeResolver;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Holds information about a resource of type <i>T</i> and its repositories.
 * It includes the following information:
 * - ResourceInformation instance with information about the resource,
 * - ResourceRepository instance,
 * - List of all repositories for relationships defined in resource class.
 * - Parent RegistryEntry if a resource inherits from another resource
 *
 * @param <T> resource type
 */
public class RegistryEntry<T> {
    private final ResourceInformation resourceInformation;
    private final ResourceRepository<T, ?> resourceRepository;
    private final List<RelationshipRepository<T, ?, ?, ?>> relationshipRepositories;
    private RegistryEntry parentRegistryEntry = null;

    public RegistryEntry(ResourceInformation resourceInformation,
                         @SuppressWarnings("SameParameterValue") ResourceRepository<T, ?> resourceRepository) {
        this(resourceInformation, resourceRepository, new LinkedList<>());
    }

    public RegistryEntry(ResourceInformation resourceInformation,
                         ResourceRepository<T, ?> resourceRepository,
                         List<RelationshipRepository<T, ?, ?, ?>> relationshipRepositories) {
        this.resourceInformation = resourceInformation;
        this.resourceRepository = resourceRepository;
        this.relationshipRepositories = relationshipRepositories;
    }

    public ResourceRepository<T, ?> getResourceRepository() {
        return resourceRepository;
    }

    public List<RelationshipRepository<T, ?, ?, ?>> getRelationshipRepositories() {
        return relationshipRepositories;
    }

    public RelationshipRepository<T, ?, ?, ?> getRelationshipRepositoryForClass(Class clazz) {
        RelationshipRepository<T, ?, ?, ?> foundRelationshipRepository = null;
        for (RelationshipRepository<T, ?, ?, ?> relationshipRepository : relationshipRepositories) {
            Class<?>[] typeArgs = TypeResolver
                    .resolveRawArguments(RelationshipRepository.class, relationshipRepository.getClass());

            if (clazz == typeArgs[RelationshipRepository.TARGET_TYPE_GENERIC_PARAMETER_IDX]) {
                foundRelationshipRepository = relationshipRepository;
            }
        }
        if (foundRelationshipRepository == null) {
            throw new RelationshipRepositoryNotFoundException(resourceInformation.getResourceClass(), clazz);
        }

        return foundRelationshipRepository;
    }

    public ResourceInformation getResourceInformation() {
        return resourceInformation;
    }

    public RegistryEntry getParentRegistryEntry() {
        return parentRegistryEntry;
    }

    /**
     * To be used only by ResourceRegistryBuilder
     * @param parentRegistryEntry parent resource
     */
    void setParentRegistryEntry(RegistryEntry parentRegistryEntry) {
        this.parentRegistryEntry = parentRegistryEntry;
    }

    /**
     * Check the parameter is a parent of <b>this</b> {@link RegistryEntry} instance
     * @param registryEntry parent to check
     * @return true if the parameter is a parent
     */
    public boolean isParent(RegistryEntry registryEntry) {
        RegistryEntry parentRegistryEntry = getParentRegistryEntry();
        while (parentRegistryEntry != null) {
            if (parentRegistryEntry.equals(registryEntry)) {
                return true;
            }
            parentRegistryEntry = parentRegistryEntry.getParentRegistryEntry();
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegistryEntry<?> that = (RegistryEntry<?>) o;
        return Objects.equals(resourceInformation, that.resourceInformation) &&
            Objects.equals(resourceRepository, that.resourceRepository) &&
            Objects.equals(relationshipRepositories, that.relationshipRepositories) &&
            Objects.equals(parentRegistryEntry, that.parentRegistryEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceInformation, resourceRepository, relationshipRepositories, parentRegistryEntry);
    }
}
