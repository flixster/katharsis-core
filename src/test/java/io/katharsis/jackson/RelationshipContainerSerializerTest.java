package io.katharsis.jackson;

import io.katharsis.resource.mock.models.Project;
import io.katharsis.resource.mock.models.Task;
import io.katharsis.resource.mock.models.User;
import io.katharsis.response.Container;
import org.junit.Test;

import java.util.Collections;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class RelationshipContainerSerializerTest extends BaseSerializerTest {

    @Test
    public void onRelationshipShouldIncludeRelationship() throws Exception {
        // GIVEN
        Project project = new Project();
        project.setId(1L);
        Task task = new Task();
        task.setProject(project);

        // WHEN
        String result = sut.writeValueAsString(new Container<>(task));

        // THEN
        assertThatJson(result).node("links.project").isPresent();
    }

    @Test
    public void onRelationshipShouldIncludeRelationshipSelfLink() throws Exception {
        // GIVEN
        Project project = new Project();
        Task task = new Task();
        task.setId(1L);
        task.setProject(project);

        // WHEN
        String result = sut.writeValueAsString(new Container<>(task));

        // THEN
        assertThatJson(result).node("links.project.self").isEqualTo("https://service.local/tasks/1/links/project");
    }

    @Test
    public void onRelationshipShouldIncludeRelationshipRelatedLink() throws Exception {
        // GIVEN
        Project project = new Project();
        Task task = new Task();
        task.setId(1L);
        task.setProject(project);

        // WHEN
        String result = sut.writeValueAsString(new Container<>(task));

        // THEN
        assertThatJson(result).node("links.project.related").isEqualTo("https://service.local/tasks/1/project");
    }

    @Test
    public void onToOneRelationshipShouldIncludeToOneRelationshipLinkage() throws Exception {
        // GIVEN
        Project project = new Project();
        project.setId(2L);
        Task task = new Task();
        task.setId(1L);
        task.setProject(project);

        // WHEN
        String result = sut.writeValueAsString(new Container<>(task));

        // THEN
        assertThatJson(result).node("links.project.linkage").isPresent();
        assertThatJson(result).node("links.project.linkage.type").isEqualTo("projects");
        assertThatJson(result).node("links.project.linkage.id").isEqualTo("\"2\"");
    }

    @Test
    public void onToOneNullRelationshipShouldIncludeNullToOneRelationshipLinkage() throws Exception {
        // GIVEN
        Task task = new Task();
        task.setId(1L);

        // WHEN
        String result = sut.writeValueAsString(new Container<>(task));

        // THEN
        assertThatJson(result).node("links.project.linkage").isEqualTo("null");
    }

    @Test
    public void onToManyRelationshipShouldIncludeToManyRelationshipLinkage() throws Exception {
        // GIVEN
        User user = new User();
        user.setId(1L);
        Project project = new Project();
        project.setId(2L);
        user.setAssignedProjects(Collections.singletonList(project));

        // WHEN
        String result = sut.writeValueAsString(new Container<>(user));

        // THEN
        assertThatJson(result).node("links.assignedProjects.linkage").isArray().ofLength(1);
        assertThatJson(result).node("links.assignedProjects.linkage[0].type").isEqualTo("projects");
        assertThatJson(result).node("links.assignedProjects.linkage[0].id").isEqualTo("\"2\"");
    }

    @Test
    public void onToManyNullRelationshipShouldIncludeNullToManyRelationshipLinkage() throws Exception {
        // GIVEN
        User user = new User();
        user.setId(1L);

        // WHEN
        String result = sut.writeValueAsString(new Container<>(user));

        // THEN
        assertThatJson(result).node("links.assignedProjects.linkage").isArray().ofLength(0);
    }
}