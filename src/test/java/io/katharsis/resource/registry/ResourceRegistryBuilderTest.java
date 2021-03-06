package io.katharsis.resource.registry;

import io.katharsis.locator.SampleJsonServiceLocator;
import io.katharsis.repository.NotFoundRepository;
import io.katharsis.repository.exception.RepositoryInstanceNotFoundException;
import io.katharsis.resource.field.ResourceFieldNameTransformer;
import io.katharsis.resource.information.ResourceInformationBuilder;
import io.katharsis.resource.mock.models.*;
import io.katharsis.resource.mock.repository.ResourceWithoutRepositoryToProjectRepository;
import io.katharsis.resource.mock.repository.TaskRepository;
import io.katharsis.resource.mock.repository.TaskToProjectRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static io.katharsis.resource.registry.ResourceRegistryTest.TEST_MODELS_URL;
import static org.assertj.core.api.Assertions.assertThat;

public class ResourceRegistryBuilderTest {

    public static final String TEST_MODELS_PACKAGE = "io.katharsis.resource.mock";
    private ResourceInformationBuilder resourceInformationBuilder;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        resourceInformationBuilder = new ResourceInformationBuilder(new ResourceFieldNameTransformer());
    }

    @Test
    public void onValidPackageShouldBuildRegistry() {
        // GIVEN
        ResourceRegistryBuilder sut = new ResourceRegistryBuilder(new SampleJsonServiceLocator(),
            resourceInformationBuilder);

        // WHEN
        ResourceRegistry resourceRegistry = sut.build(TEST_MODELS_PACKAGE, TEST_MODELS_URL);

        // THEN
        RegistryEntry tasksEntry = resourceRegistry.getEntry("tasks");
        Assert.assertNotNull(tasksEntry);
        Assert.assertEquals("id", tasksEntry.getResourceInformation().getIdField().getName());
        Assert.assertNotNull(tasksEntry.getResourceRepository());
        List tasksRelationshipRepositories = tasksEntry.getRelationshipRepositories();
        Assert.assertEquals(1, tasksRelationshipRepositories.size());
        Assert.assertEquals(TEST_MODELS_URL + "/tasks", resourceRegistry.getResourceUrl(Task.class));

        RegistryEntry projectsEntry = resourceRegistry.getEntry("projects");
        Assert.assertNotNull(projectsEntry);
        Assert.assertEquals("id", projectsEntry.getResourceInformation().getIdField().getName());
        Assert.assertNotNull(tasksEntry.getResourceRepository());
        List ProjectRelationshipRepositories = projectsEntry.getRelationshipRepositories();
        Assert.assertEquals(0, ProjectRelationshipRepositories.size());
        Assert.assertEquals(TEST_MODELS_URL + "/projects", resourceRegistry.getResourceUrl(Project.class));
    }

    @Test
    public void onValidPackagesShouldBuildRegistry() {
        // GIVEN
        ResourceRegistryBuilder sut = new ResourceRegistryBuilder(new SampleJsonServiceLocator(),
            resourceInformationBuilder);
        String packageNames = String.format("java.lang,%s,io.katharsis.locator", TEST_MODELS_PACKAGE);

        // WHEN
        ResourceRegistry resourceRegistry = sut.build(packageNames, TEST_MODELS_URL);

        // THEN
        RegistryEntry tasksEntry = resourceRegistry.getEntry("tasks");
        Assert.assertNotNull(tasksEntry);
    }

    @Test
    public void onNoEntityRepositoryInstanceShouldThrowException() {
        // GIVEN
        ResourceRegistryBuilder sut = new ResourceRegistryBuilder(new SampleJsonServiceLocator() {
            public <T> T getInstance(Class<T> clazz) {
                if (clazz == TaskRepository.class) {
                    return null;
                } else {
                    return super.getInstance(clazz);
                }
            }
        }, resourceInformationBuilder);

        // THEN
        expectedException.expect(RepositoryInstanceNotFoundException.class);

        // WHEN
        sut.build(TEST_MODELS_PACKAGE, TEST_MODELS_URL);
    }

    @Test
    public void onNoRelationshipRepositoryInstanceShouldThrowException() {
        // GIVEN
        ResourceRegistryBuilder sut = new ResourceRegistryBuilder(new SampleJsonServiceLocator() {
            public <T> T getInstance(Class<T> clazz) {
                if (clazz == TaskToProjectRepository.class) {
                    return null;
                } else {
                    return super.getInstance(clazz);
                }
            }
        }, resourceInformationBuilder);

        // THEN
        expectedException.expect(RepositoryInstanceNotFoundException.class);

        // WHEN
        sut.build(TEST_MODELS_PACKAGE, TEST_MODELS_URL);
    }

    @Test
    public void onNoRepositoryShouldCreateNotFoundRepository() {
        // GIVEN
        ResourceRegistryBuilder sut =
            new ResourceRegistryBuilder(new SampleJsonServiceLocator(), resourceInformationBuilder);

        // WHEN
        ResourceRegistry result = sut.build(TEST_MODELS_PACKAGE, TEST_MODELS_URL);

        // THEN
        RegistryEntry entry = result.getEntry(ResourceWithoutRepository.class);

        assertThat(entry.getResourceInformation().getResourceClass()).isEqualTo(ResourceWithoutRepository.class);
        assertThat(entry.getResourceRepository()).isExactlyInstanceOf(NotFoundRepository.class);
        assertThat(entry.getRelationshipRepositoryForClass(Project.class))
            .isExactlyInstanceOf(ResourceWithoutRepositoryToProjectRepository.class);
    }

    @Test
    public void onInheritedResourcesShouldAddInformationToEntry() {
        // GIVEN
        ResourceRegistryBuilder sut = new ResourceRegistryBuilder(new SampleJsonServiceLocator(),
            resourceInformationBuilder);
        String packageNames = String.format("java.lang,%s,io.katharsis.locator", TEST_MODELS_PACKAGE);

        // WHEN
        ResourceRegistry resourceRegistry = sut.build(packageNames, TEST_MODELS_URL);

        // THEN
        RegistryEntry memorandaEntry = resourceRegistry.getEntry("memoranda");
        assertThat(memorandaEntry.getParentRegistryEntry()).isNotNull();
        assertThat(memorandaEntry.getParentRegistryEntry().getResourceInformation().getResourceClass()).isEqualTo(Document.class);
        assertThat(memorandaEntry.getParentRegistryEntry().getParentRegistryEntry()).isNotNull();
        assertThat(memorandaEntry.getParentRegistryEntry().getParentRegistryEntry().getResourceInformation().getResourceClass()).isEqualTo(Thing.class);
    }

    @Test
    public void onNonInheritedResourcesShouldNotAddInformationToEntry() {
        // GIVEN
        ResourceRegistryBuilder sut = new ResourceRegistryBuilder(new SampleJsonServiceLocator(),
            resourceInformationBuilder);

        // WHEN
        ResourceRegistry resourceRegistry = sut.build(TEST_MODELS_PACKAGE, TEST_MODELS_URL);

        // THEN
        RegistryEntry tasksEntry = resourceRegistry.getEntry("tasks");
        assertThat(tasksEntry.getParentRegistryEntry()).isNull();
    }
}
