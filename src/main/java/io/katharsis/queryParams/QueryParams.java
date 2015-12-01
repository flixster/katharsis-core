package io.katharsis.queryParams;

import io.katharsis.jackson.exception.ParametersDeserializationException;
import io.katharsis.queryParams.include.Inclusion;
import io.katharsis.queryParams.params.*;
import io.katharsis.resource.RestrictedQueryParamsMembers;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains a set of parameters passed along with the request.
 */
public class QueryParams {
    private TypedParams<FilterParams> filters;
    private TypedParams<SortingParams> sorting;
    private TypedParams<GroupingParams> grouping;
    private TypedParams<IncludedFieldsParams> includedFields;
    private TypedParams<IncludedRelationsParams> includedRelations;
    private Map<RestrictedPaginationKeys, Integer> pagination;


    /**
     * <strong>Important!</strong> Katharsis implementation differs form JSON API
     * <a href="http://jsonapi.org/format/#fetching-filtering">definition of filtering</a>
     * in order to fit standard query parameter serializing strategy and maximize effective processing of data.
     * <p>
     * Filter params can be send with following format (Katharsis does not specify or implement any operators): <br>
     * <strong>filter[ResourceType][property|operator]([property|operator])* = "value"</strong><br>
     * <p>
     * Examples of accepted filtering of resources:
     * <ul>
     * <li>{@code GET /tasks/?filter[Task][name]=Super task}</li>
     * <li>{@code GET /tasks/?filter[Task][name]=Super task&filter[Task][dueDate]=2015-10-01}</li>
     * <li>{@code GET /tasks/?filter[Task][name][$startWith]=Super task}</li>
     * <li>{@code GET /tasks/?filter[Task][name][][$startWith]=Super&filter[Task][name][][$endWith]=task}</li>
     * </ul>
     *
     * @return {@link TypedParams} Map of filtering params passed to a request grouped by type of resource
     */
    public TypedParams<FilterParams> getFilters() {
        return filters;
    }

    void setFilters(Map<String, Set<String>> filters) {
        Map<String, Map<String, Set<String>>> temporaryFiltersMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : filters.entrySet()) {

            List<String> propertyList = buildPropertyListFromEntry(entry, RestrictedQueryParamsMembers.filter.name());

            String resourceType = propertyList.get(0);
            String propertyPath = String.join(".", propertyList.subList(1, propertyList.size()));

            if (temporaryFiltersMap.containsKey(resourceType)) {
                Map<String, Set<String>> resourceParams = temporaryFiltersMap.get(resourceType);
                resourceParams.put(propertyPath, Collections.unmodifiableSet(entry.getValue()));
            } else {
                Map<String, Set<String>> resourceParams = new LinkedHashMap<>();
                temporaryFiltersMap.put(resourceType, resourceParams);
                resourceParams.put(propertyPath, entry.getValue());
            }
        }

        Map<String, FilterParams> decodedFiltersMap = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, Set<String>>> resourceTypesMap : temporaryFiltersMap.entrySet()) {
            Map<String, Set<String>> filtersMap = Collections.unmodifiableMap(resourceTypesMap.getValue());
            decodedFiltersMap.put(resourceTypesMap.getKey(), new FilterParams(filtersMap));
        }

        this.filters = new TypedParams<>(Collections.unmodifiableMap(decodedFiltersMap));
    }

    /**
     * <strong>Important!</strong> Katharsis implementation differs form JSON API
     * <a href="http://jsonapi.org/format/#fetching-sorting">definition of sorting</a>
     * in order to fit standard query parameter serializing strategy and maximize effective processing of data.
     * <p>
     * Sort params can be send with following format: <br>
     * <strong>sort[ResourceType][property]([property])* = "asc|desc"</strong>
     * <p>
     * Examples of accepted sorting of resources:
     * <ul>
     * <li>{@code GET /tasks/?sort[Task][name]=asc}</li>
     * <li>{@code GET /project/?sort[Project][shortName]=desc&sort[User][name][firstName]=asc}</li>
     * </ul>
     *
     * @return {@link TypedParams} Map of sorting params passed to request grouped by type of resource
     */
    public TypedParams<SortingParams> getSorting() {
        return sorting;
    }

    void setSorting(Map<String, Set<String>> sorting) {
        Map<String, Map<String, RestrictedSortingValues>> temporarySortingMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : sorting.entrySet()) {

            List<String> propertyList = buildPropertyListFromEntry(entry, RestrictedQueryParamsMembers.sort.name());

            String resourceType = propertyList.get(0);
            String propertyPath = String.join(".", propertyList.subList(1, propertyList.size()));


            if (temporarySortingMap.containsKey(resourceType)) {
                Map<String, RestrictedSortingValues> resourceParams = temporarySortingMap.get(resourceType);
                resourceParams.put(propertyPath, RestrictedSortingValues.valueOf(entry.getValue()
                    .iterator()
                    .next()));
            } else {
                Map<String, RestrictedSortingValues> resourceParams = new HashMap<>();
                temporarySortingMap.put(resourceType, resourceParams);
                resourceParams.put(propertyPath, RestrictedSortingValues.valueOf(entry.getValue()
                    .iterator()
                    .next()));
            }
        }

        Map<String, SortingParams> decodedSortingMap = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, RestrictedSortingValues>> resourceTypesMap : temporarySortingMap.entrySet
            ()) {
            Map<String, RestrictedSortingValues> sortingMap = Collections.unmodifiableMap(resourceTypesMap.getValue());
            decodedSortingMap.put(resourceTypesMap.getKey(), new SortingParams(sortingMap));
        }


        this.sorting = new TypedParams<>(Collections.unmodifiableMap(decodedSortingMap));

    }

    /**
     * <strong>Important: </strong> Grouping itself is not specified by JSON API itself, but the
     * keyword and format it reserved for today and future use in Katharsis.
     * <p>
     * Group params can be send with following format: <br>
     * <strong>group[ResourceType] = "property(.property)*"</strong>
     * <p>
     * Examples of accepted grouping of resources:
     * <ul>
     * <li>{@code GET /tasks/?group[Task]=name}</li>
     * <li>{@code GET /project/?group[User]=name.firstName&include[Project]=team}</li>
     * </ul>
     *
     * @return {@link Map} Map of grouping params passed to request grouped by type of resource
     */
    public TypedParams<GroupingParams> getGrouping() {
        return grouping;
    }

    void setGrouping(Map<String, Set<String>> grouping) {
        Map<String, Set<String>> temporaryGroupingMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : grouping.entrySet()) {

            List<String> propertyList = buildPropertyListFromEntry(entry, RestrictedQueryParamsMembers.group.name());

            if (propertyList.size() > 1) {
                throw new ParametersDeserializationException("Exceeded maximum level of nesting of 'group' parameter " +
                    "(1) eg. group[Task][name] <-- #2 level and more are not allowed");
            }

            String resourceType = propertyList.get(0);

            if (temporaryGroupingMap.containsKey(resourceType)) {
                Set<String> resourceParams = temporaryGroupingMap.get(resourceType);
                resourceParams.addAll(entry.getValue());
                temporaryGroupingMap.put(resourceType, resourceParams);
            } else {
                Set<String> resourceParams = new LinkedHashSet<>();
                resourceParams.addAll(entry.getValue());
                temporaryGroupingMap.put(resourceType, resourceParams);
            }
        }

        Map<String, GroupingParams> decodedGroupingMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> resourceTypesMap : temporaryGroupingMap.entrySet()) {
            Set<String> groupingSet = Collections.unmodifiableSet(resourceTypesMap.getValue());
            decodedGroupingMap.put(resourceTypesMap.getKey(), new GroupingParams(groupingSet));
        }

        this.grouping = new TypedParams<>(Collections.unmodifiableMap(decodedGroupingMap));

    }

    /**
     * <strong>Important!</strong> Katharsis implementation sets on strategy of pagination whereas JSON API
     * <a href="http://jsonapi.org/format/#fetching-pagination">definition of pagination</a>
     * is agnostic about pagination strategies.
     * <p>
     * Pagination params can be send with following format: <br>
     * <strong>page[offset|limit] = "value"</strong>, where value is an integer
     * <p>
     * Examples of accepted grouping of resources:
     * <ul>
     * <li>{@code GET /projects/?page[offset]=0&page[limit]=10}</li>
     * </ul>
     *
     * @return {@link Map} Map of pagination keys passed to request
     */
    public Map<RestrictedPaginationKeys, Integer> getPagination() {
        return pagination;
    }

    void setPagination(Map<String, Set<String>> pagination) {
        Map<RestrictedPaginationKeys, Integer> decodedPagination = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : pagination.entrySet()) {
            List<String> propertyList = buildPropertyListFromEntry(entry, RestrictedQueryParamsMembers.page.name());

            if (propertyList.size() > 1) {
                throw new ParametersDeserializationException("Exceeded maximum level of nesting of 'page' parameter " +
                    "(1) eg. page[offset][minimal] <-- #2 level and more are not allowed");
            }

            String resourceType = propertyList.get(0);

            decodedPagination.put(RestrictedPaginationKeys.valueOf(resourceType), Integer.parseInt(entry
                .getValue()
                .iterator()
                .next()));
        }

        this.pagination = Collections.unmodifiableMap(decodedPagination);
    }

    /**
     * <strong>Important!</strong> Katharsis implementation differs form JSON API
     * <a href="http://jsonapi.org/format/#fetching-sparse-fieldsets">definition of sparse field set</a>
     * in order to fit standard query parameter serializing strategy and maximize effective processing of data.
     * <p>
     * Sparse field set params can be send with following format: <br>
     * <strong>fields[ResourceType] = "property(.property)*"</strong><br>
     * <p>
     * Examples of accepted sparse field sets of resources:
     * <ul>
     * <li>{@code GET /tasks/?fields[Task]=name}</li>
     * <li>{@code GET /tasks/?fields[Task][]=name&fields[Task][]=dueDate}</li>
     * <li>{@code GET /tasks/?fields[User]=name.surname&include[Task]=author}</li>
     * </ul>
     *
     * @return {@link TypedParams} Map of sparse field set params passed to a request grouped by type of resource
     */
    public TypedParams<IncludedFieldsParams> getIncludedFields() {
        return includedFields;
    }

    void setIncludedFields(Map<String, Set<String>> sparse) {
        Map<String, Set<String>> temporarySparseMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : sparse.entrySet()) {
            List<String> propertyList = buildPropertyListFromEntry(entry, RestrictedQueryParamsMembers.fields.name());

            if (propertyList.size() > 1) {
                throw new ParametersDeserializationException("Exceeded maximum level of nesting of 'fields' " +
                    "parameter (1) eg. fields[Task][name] <-- #2 level and more are not allowed");
            }

            String resourceType = propertyList.get(0);

            if (temporarySparseMap.containsKey(resourceType)) {
                Set<String> resourceParams = temporarySparseMap.get(resourceType);
                resourceParams.addAll(entry.getValue());
                temporarySparseMap.put(resourceType, resourceParams);
            } else {
                Set<String> resourceParams = new LinkedHashSet<>();
                resourceParams.addAll(entry.getValue());
                temporarySparseMap.put(resourceType, resourceParams);
            }
        }

        Map<String, IncludedFieldsParams> decodedSparseMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> resourceTypesMap : temporarySparseMap.entrySet()) {
            Set<String> sparseSet = Collections.unmodifiableSet(resourceTypesMap.getValue());
            decodedSparseMap.put(resourceTypesMap.getKey(), new IncludedFieldsParams(sparseSet));
        }

        this.includedFields = new TypedParams<>(Collections.unmodifiableMap(decodedSparseMap));
    }

    /**
     * <strong>Important!</strong> Katharsis implementation differs form JSON API
     * <a href="http://jsonapi.org/format/#fetching-includes">definition of includes</a>
     * in order to fit standard query parameter serializing strategy and maximize effective processing of data.
     * <p>
     * Included field set params can be send with following format: <br>
     * <strong>include[ResourceType] = "property(.property)*"</strong><br>
     * <p>
     * Examples of accepted sparse field sets of resources:
     * <ul>
     * <li>{@code GET /tasks/?include[Task]=author}</li>
     * <li>{@code GET /tasks/?include[Task][]=author&include[Task][]=comments}</li>
     * <li>{@code GET /projects/?include[Project]=task&include[Task]=comments}</li>
     * </ul>
     *
     * @return {@link TypedParams} Map of sparse field set params passed to a request grouped by type of resource
     */
    public TypedParams<IncludedRelationsParams> getIncludedRelations() {
        return includedRelations;
    }

    void setIncludedRelations(Map<String, Set<String>> inclusions) {
        Map<String, Set<Inclusion>> temporaryInclusionsMap = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : inclusions.entrySet()) {
            List<String> propertyList = buildPropertyListFromEntry(entry, RestrictedQueryParamsMembers.include.name());

            if (propertyList.size() > 1) {
                throw new ParametersDeserializationException("Exceeded maximum level of nesting of 'include' " +
                    "parameter (1)");
            }

            String resourceType = propertyList.get(0);

            if (temporaryInclusionsMap.containsKey(resourceType)) {
                Set<Inclusion> resourceParams = temporaryInclusionsMap.get(resourceType);
                resourceParams.add(new Inclusion(entry.getValue()
                    .iterator()
                    .next()));
                temporaryInclusionsMap.put(resourceType, resourceParams);
            } else {
                Set<Inclusion> resourceParams = new LinkedHashSet<>();
                resourceParams.add(new Inclusion(entry.getValue()
                    .iterator()
                    .next()));
                temporaryInclusionsMap.put(resourceType, resourceParams);
            }
        }

        Map<String, IncludedRelationsParams> decodedInclusions = new LinkedHashMap<>();

        for (Map.Entry<String, Set<Inclusion>> resourceTypesMap : temporaryInclusionsMap.entrySet()) {
            Set<Inclusion> inclusionSet = Collections.unmodifiableSet(resourceTypesMap.getValue());
            decodedInclusions.put(resourceTypesMap.getKey(), new IncludedRelationsParams(inclusionSet));
        }

        this.includedRelations = new TypedParams<>(Collections.unmodifiableMap(decodedInclusions));
    }

    private List<String> buildPropertyListFromEntry(Map.Entry<String, Set<String>> entry, String prefix) {
        String entryKey = entry.getKey()
            .substring(prefix.length());

        String pattern = "\\w+(?<!\\[)(?=\\])";
        Pattern regexp = Pattern.compile(pattern);
        Matcher matcher = regexp.matcher(entryKey);
        List<String> matchList = new LinkedList<>();

        while (matcher.find()) {
            matchList.add(matcher.group());
        }


        if (matchList.size() < 1) {
            throw new ParametersDeserializationException("Malformed filter parameter: " + entryKey);
        }

        return matchList;
    }

}
