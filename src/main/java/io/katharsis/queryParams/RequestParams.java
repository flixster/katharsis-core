package io.katharsis.queryParams;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.katharsis.queryParams.include.Inclusion;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Contains a set of parameters passed along with the request.
 */
public class RequestParams {
    private Map<String, Object> filters;
    private Map<String, SortingValues> sorting;
    private List<String> grouping;
    private Map<PaginationKeys, Integer> pagination;
    private List<String> includedFields;
    private List<Inclusion> includedRelations;

    private final ObjectMapper objectMapper;

    private static final TypeReference SORTING_TYPE_REFERENCE;
    private static final TypeReference GROUPING_TYPE_REFERENCE;
    private static final TypeReference PAGINATION_TYPE_REFERENCE;
    private static final TypeReference INCLUDED_FIELDS_TYPE_REFERENCE;
    private static final TypeReference INCLUDED_RELATIONS_TYPE_REFERENCE;

    static {
        SORTING_TYPE_REFERENCE = new TypeReference<Map<String, SortingValues>>() {};
        GROUPING_TYPE_REFERENCE = new TypeReference<List<String>>() {};
        PAGINATION_TYPE_REFERENCE = new TypeReference<Map<PaginationKeys, Integer>>() {};
        INCLUDED_FIELDS_TYPE_REFERENCE = new TypeReference<List<String>>() {};
        INCLUDED_RELATIONS_TYPE_REFERENCE = new TypeReference<List<String>>() {};
    }

    public RequestParams(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Contains a set of filters assigned to a request. <a href="http://jsonapi.org/format/#fetching-filtering">Filtering</a>
     *
     * @return set of filters sent along with the request
     */
    public Map<String, Object> getFilters() {
    	return filters;
    }

    void setFilters(String filters) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(filters);
        Map<String, Object> result = objectMapper.convertValue(jsonNode, Map.class);
        this.filters = result;
    }

    /**
     * Contains a map of sorting values. <a href="http://jsonapi.org/format/#fetching-sorting">Sorting</a>
     * @return set of sorting fields assigned to a request
     */
    public Map<String, SortingValues> getSorting() {
        return sorting;
    }

    void setSorting(String sorting) throws IOException {
        this.sorting = Collections.unmodifiableMap(
                objectMapper.readValue(sorting, SORTING_TYPE_REFERENCE)
        );
    }

    public List getGrouping() {
        return grouping;
    }

    void setGrouping(String grouping) throws IOException {
        this.grouping = Collections.unmodifiableList(
                objectMapper.readValue(grouping, GROUPING_TYPE_REFERENCE)
        );
    }

    public Map<PaginationKeys, Integer> getPagination() {
        return pagination;
    }

    void setPagination(String pagination) throws IOException {
        this.pagination = Collections.unmodifiableMap(
                objectMapper.readValue(pagination, PAGINATION_TYPE_REFERENCE)
        );
    }

    public List<String> getIncludedFields() {
        return includedFields;
    }

    void setIncludedFields(String includedFields) throws IOException {
        this.includedFields = Collections.unmodifiableList(
                objectMapper.readValue(includedFields, INCLUDED_FIELDS_TYPE_REFERENCE)
        );
    }

    /**
     * Get a set of included fields which should be included in the resource
     * @return included relationships
     */
    public List<Inclusion> getIncludedRelations() {
        return includedRelations;
    }

    void setIncludedRelations(String includedRelations) throws IOException {
        List<? extends String> list = objectMapper.readValue(includedRelations, INCLUDED_RELATIONS_TYPE_REFERENCE);
        List<Inclusion> inclusions = list
                .stream()
                .map(Inclusion::new)
                .collect(Collectors.toList());
        this.includedRelations = Collections.unmodifiableList(inclusions);
    }

}
