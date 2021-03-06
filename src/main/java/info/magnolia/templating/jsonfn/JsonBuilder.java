/**
 * This file Copyright (c) 2016 Magnolia International
 * Ltd.  (http://www.magnolia-cms.com). All rights reserved.
 *
 *
 * This file is dual-licensed under both the Magnolia
 * Network Agreement and the GNU General Public License.
 * You may elect to use one or the other of these licenses.
 *
 * This file is distributed in the hope that it will be
 * useful, but AS-IS and WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, or NONINFRINGEMENT.
 * Redistribution, except as permitted by whichever of the GPL
 * or MNA you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or
 * modify this file under the terms of the GNU General
 * Public License, Version 3, as published by the Free Software
 * Foundation.  You should have received a copy of the GNU
 * General Public License, Version 3 along with this program;
 * if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * 2. For the Magnolia Network Agreement (MNA), this file
 * and the accompanying materials are made available under the
 * terms of the MNA which accompanies this distribution, and
 * is available at http://www.magnolia-cms.com/mna.html
 *
 * Any modifications to this file must keep this entire header
 * intact.
 *
 */
package info.magnolia.templating.jsonfn;

import static info.magnolia.cms.util.QueryUtil.search;
import static info.magnolia.context.MgnlContext.getJCRSession;
import static info.magnolia.jcr.util.PropertyUtil.*;
import static info.magnolia.templating.jsonfn.Java8Util.*;

import info.magnolia.dam.templating.functions.DamTemplatingFunctions;
import info.magnolia.jcr.util.ContentMap;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.jcr.wrapper.I18nNodeWrapper;
import info.magnolia.link.LinkUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;


/**
 * Builder for converting JCR nodes into json.
 */
public class JsonBuilder implements Cloneable {

    /**
     * Simple bean holding info about expanded-to-be property mapping.
     */
    private static class MultiExpand {

        private final String repository;
        private final String propertyName;

        private MultiExpand(String targetRepository, String targetPropertyName) {
            this.repository = targetRepository;
            this.propertyName = targetPropertyName;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(JsonBuilder.class);
    private static final Pattern ESCAPES = Pattern.compile("\\\\");

    private ObjectMapper mapper = new ObjectMapper();
    private Node node;
    private String referencingPropertyName;
    private List<String> regexExcludes = new LinkedList<>();
    private List<String> butInclude = new LinkedList<>();
    private Map<String, String> expands = new HashMap<>();
    private boolean childrenOnly;
    private int totalDepth = 0;
    private LinkedList<String> renditions = new LinkedList<>();
    private String preexisingJson;
    private boolean inline;
    private boolean wrapForI18n;
    private String readNodeTypes = "^(?!rep:).*$";
    private String allowOnlyNodeTypes = ".*";

    private boolean allowDeleted = false;

    private Map<Character, Character> masks = new LinkedHashMap<>();
    private Map<String, List<String>> subNodeSpecificProperties = new LinkedHashMap<>();
    private boolean escapeBackslash;
    private final Map<String, String> childrenArrayCandidates = new LinkedHashMap<>();
    private Map<String, MultiExpand> expandsMulti = new LinkedHashMap<>();
    private final Map<String, JsonNode> customInserts = new HashMap<>();
    private Map<Pattern, String> renames = new LinkedHashMap<>();

    private final DamTemplatingFunctions damTemplatingFunctions;

    protected JsonBuilder(final DamTemplatingFunctions damTemplatingFunctions) {
        this.damTemplatingFunctions = damTemplatingFunctions;
    }

    protected DamTemplatingFunctions getDamTemplatingFunctions() {
        return this.damTemplatingFunctions;
    }

    protected void setNode(Node node) {
        this.node = node;
    }

    protected void setReferencingPropertyName(String name) {
        this.referencingPropertyName = name;
    }

    protected void setChildrenOnly(boolean childrenOnly) {
        this.childrenOnly = childrenOnly;
    }

    /**
     * Will expand id into sub array.
     *
     * @param propertyName
     *            property to expand.
     * @param repository
     *            repository in which to look for node matching the id specified in the property.
     */
    public JsonBuilder expand(String propertyName, String repository) {
        this.expands.put(propertyName, repository);
        this.butInclude.add(propertyName);
        return this;
    }

    /**
     * Will expand id(s) provided in propertyNameRegex into sub array of nodes from targetRepository with property matching the targetPropertyName and one of the values in propertyNameRegex.
     *
     * @param propertyNameRegex
     *            source property name pattern to expand.
     * @param targetRepository
     *            repository in which to look for node matching the id specified in the property matching propertyNameRegex.
     * @param targetPropertyName
     *            target property with id value.
     */
    public JsonBuilder expand(String propertyNameRegex, String targetRepository, String targetPropertyName) {
        this.expandsMulti.put(propertyNameRegex, new MultiExpand(targetRepository, targetPropertyName));
        this.butInclude.add(propertyNameRegex);
        return this;
    }

    /**
     * Will attempt to retrieve rendition specific link for all generated links. Links will be available as "@rendition_name" : "link-or-null"
     *
     * @param variation
     *            rendition to create link for.
     */
    public JsonBuilder binaryLinkRendition(String... variation) {
        this.renditions.addAll(Arrays.asList(variation));
        return this;
    }

    /**
     * Excludes properties matching provided regex.
     */
    public JsonBuilder exclude(String... string) {
        this.regexExcludes.addAll(Arrays.asList(string));
        return this;
    }

    /**
     * will expand children of current node number of levels down.
     */
    public JsonBuilder down(int level) {
        this.totalDepth = level;
        return this;
    }

    public JsonBuilder wrapForI18n() {
        this.wrapForI18n = true;
        return this;
    }

    public JsonBuilder inline() {
        this.inline = true;
        return this;
    }

    public JsonBuilder maskChar(char what, char replace) {
        this.masks.put(what, replace);
        return this;
    }

    public JsonBuilder escapeBackslash() {
        this.escapeBackslash = true;
        return this;
    }

    public JsonBuilder readNodeTypes(String nodeTypesRegex) {
        readNodeTypes = nodeTypesRegex;
        return this;
    }

    public JsonBuilder allowOnlyNodeTypes(String nodeTypesRegex) {
        allowOnlyNodeTypes = nodeTypesRegex;
        return this;
    }

    public JsonBuilder allowDeleted() {
        allowDeleted = true;
        return this;
    }

    /**
     * Includes only specified properties. Use together with excludeAll().
     */
    public JsonBuilder add(String... string) {
        Arrays.stream(string)
                .filter(it -> it.contains("['") && it.contains("']"))
                .map(it -> it.split("\\['"))
                .forEach(subpropertyArray -> addToSubPropertyMap(subpropertyArray[0], StringUtils.substringBefore(subpropertyArray[1], "']")));

        List<String> list = new ArrayList<>();
        Arrays.stream(string)
                .filter(it -> !it.contains("['")).forEach(list::add);
        this.butInclude.addAll(list);
        return this;
    }

    private void addToSubPropertyMap(String parentNodeName, String propertyName) {
        if (!subNodeSpecificProperties.containsKey(parentNodeName)) {
            subNodeSpecificProperties.put(parentNodeName, new ArrayList<>());
        }
        subNodeSpecificProperties.get(parentNodeName).add(propertyName);
    }

    /**
     * Includes only specified properties. Use together with excludeAll().
     */
    public JsonBuilder addAll() {
        this.butInclude.add(".*");
        return this;
    }

    public JsonBuilder childrenAsArray(String propertyName, String valueRegex) {
        childrenArrayCandidates.put(propertyName, valueRegex);
        return this;
    }

    /**
     * Insert custom JSON anywhere in the tree, replacing pre-existing content.
     * Invalid JSON is ignored, i.e. the original content remains in that case.
     *
     * @param pathSuffix
     * @param json
     */
    public JsonBuilder insertCustom(String pathSuffix, String json) {
        try {
            JsonNode jsonObject = mapper.reader().readTree(json);
            customInserts.put(pathSuffix, jsonObject);
        } catch (IOException e) {
            log.debug("Failed to parse custom JSON", e);
        }
        return this;
    }

    public JsonBuilder renameKey(String replaceRegex, String replacement) {
        this.renames.put(Pattern.compile(replaceRegex), replacement);
        return this;
    }

    /**
     * Executes configured chain of operations and produces the json output.
     */
    public String print() {

        ObjectWriter ow = mapper.writer();
        if (!inline) {
            ow = ow.withDefaultPrettyPrinter();
        }

        if (wrapForI18n) {
            node = new I18nNodeWrapper(node);
        }
        try {
            // total depth is that of starting node + set total by user
            totalDepth += node.getDepth();
            String json;
            if (childrenOnly) {
                Collection<EntryableContentMap> childNodes = new LinkedList<>();
                NodeIterator nodes = this.node.getNodes();
                asNodeStream(nodes)
                        .filter(this::isSearchInNodeType)
                        .map(this::cloneWith)
                        .forEach(builder -> childNodes.add(new EntryableContentMap(builder)));
                json = ow.writeValueAsString(childNodes);
            } else if (!allowOnlyNodeTypes.equals(".*")) {
                Collection<EntryableContentMap> childNodes = new LinkedList<>();
                NodeIterator nodes = this.node.getNodes();
                asNodeStream(nodes)
                        .filter(this::isSearchInNodeType)
                        .forEach(new PredicateSplitterConsumer<>(this::isOfAllowedDepthAndType,
                                allowedNode -> childNodes.add(new EntryableContentMap(this.cloneWith(allowedNode))),
                                allowedParent -> childNodes.addAll(this.getAllowedChildNodesContentMapsOf(allowedParent, 1))));
                json = ow.writeValueAsString(childNodes);

            } else {
                EntryableContentMap map = new EntryableContentMap(this);
                List<String> garbage = map.entrySet().stream()
                        .filter(entry -> entry.getValue() instanceof EntryableContentMap)
                        .filter(entry -> ((EntryableContentMap) entry.getValue()).entrySet().isEmpty())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                garbage.forEach(map::remove);
                json = ow.writeValueAsString(map);
            }

            if (StringUtils.isNotEmpty(preexisingJson)) {
                String trimmedJson = preexisingJson.trim();
                if (trimmedJson.endsWith("}")) {
                    json = "[" + preexisingJson + "," + json + "]";
                } else if (trimmedJson.endsWith("]")) {
                    json = StringUtils.substringBeforeLast(preexisingJson, "]") + (trimmedJson.equals("[]") ? "" : ",") + json + "]";
                }
            }
            if (escapeBackslash) {
                json = ESCAPES.matcher(json).replaceAll("\\\\\\\\");
            }
            return json;
        } catch (JsonProcessingException | RepositoryException e) {
            log.debug("Failed to generate JSON string", e);
        }

        return "{ }";
    }

    private boolean isSearchInNodeType(Node n) {
        try {
            return n != null && n.getPrimaryNodeType().getName().matches(readNodeTypes);
        } catch (RepositoryException e) {
            // when failing to check because of the repo issue, assume node is fine.
            log.error(e.getMessage(), e);
            return true;
        } catch (PatternSyntaxException e) {
            // when failing due to broken pattern, leave result empty to alert dev to broken pattern.
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean isOfAllowedDepthAndType(Node n) {
        boolean keep = true;
        try {
            keep = (this.totalDepth >= n.getDepth()) && isOfAllowedNodeType(n);
        } catch (RepositoryException e) {
            // ignore
        }
        return keep;
    }

    private boolean isOfAllowedNodeType(Node n) {
        try {
            return n != null && n.getPrimaryNodeType().getName().matches(allowOnlyNodeTypes);
        } catch (RepositoryException e) {
            // when failing to check because of the repo issue, assume node is fine.
            log.error(e.getMessage(), e);
            return true;
        } catch (PatternSyntaxException e) {
            // when failing due to broken pattern, leave result empty to alert dev to broken pattern.
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private Collection<EntryableContentMap> getAllowedChildNodesContentMapsOf(Node n, int levels) {
        try {
            Collection<EntryableContentMap> childNodes = new LinkedList<>();
            NodeIterator nodes = n.getNodes();
            asNodeStream(nodes)
                    .filter(this::isSearchInNodeType)
                    .forEach(new PredicateSplitterConsumer<>(this::isOfAllowedDepthAndType,
                            allowedNode -> childNodes.add(new EntryableContentMap(this.cloneWith(allowedNode))),
                            allowedParent -> childNodes.addAll(this.getAllowedChildNodesContentMapsOf(allowedParent, levels + 1))));
            return childNodes;
        } catch (RepositoryException e) {
            // failed to get child nodes
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private boolean isExpandable(String propertyName) {
        // quick check for simple props
        return expands.containsKey(propertyName) || expandsMulti.keySet().stream().anyMatch(propertyName::matches);

    }

    private JsonBuilder cloneWith(Node n) {
        JsonBuilder clone = clone();
        clone.node = n;
        return clone;
    }

    @Override
    protected JsonBuilder clone() {
        try {
            JsonBuilder clone = (JsonBuilder) super.clone();
            clone.butInclude = new LinkedList<>(clone.butInclude);
            clone.expands = new HashMap<>(clone.expands);
            clone.expandsMulti = new LinkedHashMap<>(clone.expandsMulti);
            clone.masks = new LinkedHashMap<>(clone.masks);
            clone.renames = new LinkedHashMap<>(clone.renames);
            clone.subNodeSpecificProperties = new LinkedHashMap<>(clone.subNodeSpecificProperties);
            clone.referencingPropertyName = null;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pimped up ContentMap that is Jackson friendly (supports entrySet() method).
     */
    public static class EntryableContentMap extends ContentMap {

        /**
         * Internal map of resolved properties.
         */
        private final Map<String, Object> props = new LinkedHashMap<>();

        /**
         * Represents getters of the node itself.
         */
        private final Map<String, Method> specialProperties = new HashMap<>();

        private JsonBuilder config;

        private List<Object> deletedKeys = new LinkedList<>();

        public EntryableContentMap(JsonBuilder builder) {
            super(builder.node);
            this.config = builder;

            Class<? extends Node> clazz = builder.node.getClass();
            try {
                specialProperties.put("@name", clazz.getMethod("getName", (Class<?>[]) null));
                specialProperties.put("@id", clazz.getMethod("getIdentifier", (Class<?>[]) null));
                specialProperties.put("@path", clazz.getMethod("getPath", (Class<?>[]) null));
                specialProperties.put("@depth", clazz.getMethod("getDepth", (Class<?>[]) null));
                specialProperties.put("@nodeType", clazz.getMethod("getPrimaryNodeType", (Class<?>[]) null));
                specialProperties.put("@link", LinkUtil.class.getMethod("createAbsoluteLink", Node.class));
            } catch (SecurityException e) {
                log.debug("Failed to gain access to Node get***() method. Check VM security settings. {}", e.getLocalizedMessage(), e);
            } catch (NoSuchMethodException e) {
                log.debug("Failed to retrieve get***() method of Node class. Check the classpath for conflicting version of JCR classes. {}", e.getLocalizedMessage(), e);
            }
        }

        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet() {
            if (props.isEmpty()) {
                populateProperties(props);
            }
            return props.entrySet();
        }

        @Override
        public boolean containsKey(Object key) {
            if (props.isEmpty()) {
                populateProperties(props);
            }
            return props.containsKey(key);
        }

        @Override
        public Object remove(final Object key) {
            if (props.isEmpty()) {
                populateProperties(props);
            }
            Object obj = props.remove(key);
            this.deletedKeys.add(key);
            return obj;
        }

        @Override
        public Object get(Object key) {
            Object superResult = super.get(key);
            if (!(superResult instanceof ContentMap))
                return superResult;

            Node node = ((ContentMap) superResult).getJCRNode();
            if (hasCustomReplacement(node))
                return getCustomReplacement(node);
            if (isArrayParent(node))
                return childrenAsContentMapList(node);

            return superResult;
        }

        private boolean hasCustomReplacement(Item item) {
            return getCustomReplacement(item) != null;
        }

        private JsonNode getCustomReplacement(Item item) {
            try {
                final String path = item.getPath();

                Optional<String> keyOptional = config.customInserts.keySet().stream()
                        .filter(path::endsWith)
                        .findFirst();
                if (keyOptional.isPresent())
                    return config.customInserts.get(keyOptional.get());
            } catch (RepositoryException e) {
                log.debug("Failed to get path of JCR item", e);
            }

            return null;
        }

        private boolean isArrayParent(Node candidate) {
            for (String key : config.childrenArrayCandidates.keySet()) {
                final String value = config.childrenArrayCandidates.get(key);
                final Pattern valuePattern = Pattern.compile(value);
                final Pattern keyPattern = Pattern.compile(key);

                try {
                    if (!asPropertyStream(candidate.getProperties())
                            .filter(p -> keyPattern.matcher(getName(p)).matches() && valuePattern.matcher(getValueString(p)).matches())
                            .collect(Collectors.toList()).isEmpty())
                        return true;
                } catch (RepositoryException e) {
                    log.debug("Failed to get properties of node", e);
                }

                if (!specialProperties.entrySet().stream()
                        .filter(entry -> keyPattern.matcher(entry.getKey()).matches())
                        .filter(entry -> valuePattern.matcher(invoke(entry.getValue(), candidate) + "").matches())
                        .collect(Collectors.toList()).isEmpty())
                    return true;
            }

            return false;
        }

        private List<ContentMap> childrenAsContentMapList(Node node) {
            try {
                return asNodeStream(node.getNodes())
                        .map(n -> new EntryableContentMap(config.cloneWith(n)))
                        .collect(Collectors.toList());
            } catch (RepositoryException e) {
                log.debug("Failed to get children of node {}", node, e);
                return Collections.emptyList();
            }
        }

        private void populateProperties(Map<String, Object> props) {
            PropertyIterator properties;
            try {
                Node node = getJCRNode();
                // filter properties only for the nodetypes we are interested in, but skip the rest
                if (node.getPrimaryNodeType().getName().matches(config.allowOnlyNodeTypes)) {
                    properties = node.getProperties();
                    Stream<String> stream;
                    List<String> includes = new LinkedList<>();
                    includes.addAll(config.butInclude);
                    if (config.subNodeSpecificProperties.containsKey(node.getName())) {
                        includes.addAll(config.subNodeSpecificProperties.get(node.getName()));
                    }

                    Stream<String> matchedRegex = getAllMatchedRegex(node.getName(), config.subNodeSpecificProperties.keySet());
                    matchedRegex.forEach(match -> includes.addAll(config.subNodeSpecificProperties.get(match)));

                    if (config.subNodeSpecificProperties.containsKey(config.referencingPropertyName)) {
                        includes.addAll(config.subNodeSpecificProperties.get(config.referencingPropertyName));
                    }

                    matchedRegex = getAllMatchedRegex(config.referencingPropertyName, config.subNodeSpecificProperties.keySet());
                    matchedRegex.forEach(match -> includes.addAll(config.subNodeSpecificProperties.get(match)));

                    stream = asPropertyStream(properties)
                            .map(Java8Util::getName)
                            .filter(name -> matchesRegex(name, includes))
                            .filter(name -> !matchesRegex(name, config.regexExcludes) && !matchesRegex(getName(node) + "'" + name + "'", config.regexExcludes));

                    // do not try to include binary data since we don't try to encode them either and jackson just blows w/o that
                    stream.filter(name -> getJCRPropertyType(getPropertyValueObject(node, name)) != PropertyType.BINARY)
                            .forEach(new PredicateSplitterConsumer<>(config::isExpandable,
                                    expandableProperty -> props.put(renameAndMask(expandableProperty), expand(expandableProperty, node)),
                                    flatProperty -> props.put(renameAndMask(flatProperty), getPropertyValueObject(node, flatProperty))));

                    asPropertyStream(node.getProperties())
                            .filter(this::hasCustomReplacement)
                            .forEach(p -> props.put(renameAndMask(getName(p)), getCustomReplacement(p)));

                    // merge multiexpands with use of temp copy to avoid CCME
                    HashMap<String, Object> propsClone = new HashMap<>(props);
                    config.expandsMulti.keySet().stream()
                            .map(key -> new AbstractMap.SimpleEntry<>(key, propsClone.keySet().stream()
                                    .filter(propKey -> propKey.matches(key))
                                    .map(props::remove)
                                    .collect(Collectors.toList())))
                            .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), flatten(entry.getValue())))
                            .filter(entry -> entry.getValue().size() > 0)
                            .forEach(entry -> props.put(renameAndMask(entry.getKey()), entry.getValue()));

                    Stream<Entry<String, Method>> specialStream;
                    specialStream = specialProperties.entrySet().stream()
                            .filter(entry -> matchesRegex(entry.getKey(), includes))
                            .filter(entry -> (!matchesRegex(entry.getKey(), config.regexExcludes) && !matchesRegex(getName(node) + "'" + entry.getKey() + "'", config.regexExcludes)));
                    specialStream.forEach(entry -> props.put(renameAndMask(entry.getKey()), invoke(entry.getValue(), node)));
                    if (node.getPrimaryNodeType().getName().equals("mgnl:asset")) {
                        config.renditions.forEach(rendition -> props.put("@rendition_" + rendition, generateRenditionLink(rendition, node)));
                    }
                } else {
                    // nothing since we don't do anything except for removal.
                }
                if (config.totalDepth >= node.getDepth()) {
                    asNodeStream(node.getNodes())
                            .filter(config::isSearchInNodeType)
                            .forEach(new PredicateSplitterConsumer<>(config::isOfAllowedDepthAndType,
                                    allowedNode -> props.put(renameAndMask(getName(allowedNode)), getOutputSubtree(allowedNode)),
                                    allowedParent -> props.putAll(this.getAllowedChildNodesPropertyMapsOf(allowedParent))));
                }

            } catch (RepositoryException e) {
                // ignore and return empty map
                log.debug("Failed to read node from repository with {}", e.getMessage(), e);
            }

            deletedKeys.forEach(props::remove);
        }

        private Collection<Object> flatten(List<Object> values) {
            if (values.size() == 0) {
                // nothing to do
                return values;
            }
            if (values.size() == 1) {
                // for multivalue properties we end up in the list wrapped in the list, so let's unwrap
                Object val = values.get(0);
                if (val instanceof List) {
                    return flatten((List) val);
                } else {
                    return values;
                }
            }
            // dedup the items in the result list
            TreeSet<Object> flat = new TreeSet<>((o1, o2) -> {
                if (!(o1 instanceof EntryableContentMap) || !(o2 instanceof EntryableContentMap)) {
                    return o1.equals(o2) ? 0 : 1;
                }
                EntryableContentMap c1 = (EntryableContentMap) o1;
                EntryableContentMap c2 = (EntryableContentMap) o2;
                return (c1.entrySet().containsAll(c2.entrySet()) && c2.entrySet().containsAll(c1.entrySet())) ? 0 : (c1.size() > c2.size() ? 1 : -1);
            });
            // and flatten out collections (if any)
            values.forEach(new PredicateSplitterConsumer<>(item -> item instanceof Collection,
                    item -> flat.addAll((Collection) item),
                    flat::add));
            return flat;
        }

        private Object getOutputSubtree(Node node) {
            if (hasCustomReplacement(node))
                return getCustomReplacement(node);

            if (isArrayParent(node))
                return childrenAsContentMapList(node);

            return new EntryableContentMap(config.cloneWith(node));
        }

        private String renameAndMask(String name) {
            for (Entry<Pattern, String> e : config.renames.entrySet()) {
                if (e.getKey().matcher(name).matches()) {
                    name = e.getValue();
                    break;
                }
            }
            for (Entry<Character, Character> e : config.masks.entrySet()) {
                name = name.replace(e.getKey(), e.getValue());
            }
            return name;
        }

        private Map<String, Object> getAllowedChildNodesPropertyMapsOf(Node parent) {
            try {
                Map<String, Object> props = new LinkedHashMap<>();
                asNodeStream(parent.getNodes())
                        .filter(config::isSearchInNodeType)
                        .forEach(new PredicateSplitterConsumer<>(config::isOfAllowedDepthAndType,
                                allowedNode -> props.put(renameAndMask(getName(allowedNode)), new EntryableContentMap(config.cloneWith(allowedNode))),
                                allowedParent -> props.putAll(this.getAllowedChildNodesPropertyMapsOf(allowedParent))));
                return props;
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
                return Collections.emptyMap();
            }
        }

        private Object generateRenditionLink(String rendition, Node node) {
            try {
                return config.getDamTemplatingFunctions().getAssetLink("jcr:" + node.getIdentifier(), rendition);
            } catch (RepositoryException e) {
                log.debug("Failed to retrieve rendition {} for {} with {}", rendition, node, e.getMessage(), e);
            }
            return null;
        }

        private Object invoke(Method method, Node node) {
            try {
                try {
                    if (method.getName().equals("createAbsoluteLink") && node.getPrimaryNodeType().getName().equals("mgnl:asset")) {
                        return config.getDamTemplatingFunctions().getAssetLink("jcr:" + node.getIdentifier());
                    }
                } catch (RepositoryException e) {
                    // bad luck we handle it the usual way
                    log.debug("Failed to create link using {} for {} with {}", method, node, e.getMessage(), e);
                }
                if (method.getParameterCount() > 0) {
                    return method.invoke(null, node);
                }
                Object result = method.invoke(node);
                if (result instanceof NodeType) {
                    return ((NodeType) result).getName();
                } else {
                    return result;
                }

            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                // ignore
                log.debug("Failed to invoke {} for {} with {}", method, node, e.getMessage(), e);
            }
            return null;
        }

        private Object expand(String expandableProperty, Node node) {
            Map<String, Object> expanded = new HashMap<>();
            String propWorkspace = config.expands.get(expandableProperty);
            String propTargetName = "jcr:uuid";
            if (propWorkspace == null) {
                // multi expand
                Optional<java.util.Map.Entry<String, MultiExpand>> propDescriptor = config.expandsMulti.entrySet().stream()
                        .filter(entry -> expandableProperty.matches(entry.getKey()))
                        .findFirst();
                if (!propDescriptor.isPresent()) {
                    return expanded;
                }
                MultiExpand propValue = propDescriptor.get().getValue();
                propWorkspace = propValue.repository;
                propTargetName = propValue.propertyName;
            }
            try {
                final String workspace = propWorkspace;
                final String targetName = propTargetName;
                Property property = node.getProperty(expandableProperty);
                if (property.isMultiple()) {
                    List<String> expandables = getValuesStringList(property.getValues());
                    return expandables.stream()
                            .map(expandable -> expandSingle(expandable, workspace, expandableProperty, targetName))
                            .filter(it -> it != null)
                            .collect(Collectors.toList());
                } else {
                    String expandable = getValueString(property);
                    return expandSingle(expandable, workspace, expandableProperty, targetName);
                }

            } catch (RepositoryException e) {
                log.debug(e.getMessage(), e);
            }

            return expanded;
        }

        private Object expandSingle(String expandable, String workspace, String expandableProperty, String targetName) {
            if (expandable == null) {
                return null;
            }
            Node expandedNode;
            try {
                if (targetName.equals("jcr:uuid")) {
                    Session session = getJCRSession(workspace);
                    if (expandable.startsWith("jcr:")) {
                        expandable = StringUtils.removeStart(expandable, "jcr:");
                    }
                    if (expandable.startsWith("/")) {
                        expandedNode = session.getNode(expandable);
                    } else {
                        expandedNode = session.getNodeByIdentifier(expandable);
                    }
                    if (config.allowDeleted || isNotDeleted(expandedNode)) {
                        return mapToECMap(expandedNode, expandableProperty, config);
                    } else {
                        return null;
                    }
                } else {
                    String statement = "select * from [nt:base] where contains(" + escapeForQuery(targetName) + ",'" + escapeForQuery(expandable) + "')";
                    NodeIterator results = search(workspace, statement);
                    return asNodeStream(results)
                            .filter(node -> config.allowDeleted || isNotDeleted(node))
                            .map(expanded -> mapToECMap(expanded, expandableProperty, config))
                            .collect(Collectors.toList());
                }
            } catch (RepositoryException e) {
                log.debug(e.getMessage(), e);
                return null;
            }
        }

        private boolean isNotDeleted(Node node) {
            try {
                boolean isDeleted = NodeUtil.hasMixin(node, NodeTypes.Deleted.NAME);
                return !isDeleted;
            } catch (RepositoryException e) {
                throw new RuntimeException(String.format("Failed to check for deleted nodes because of %s.", e.getMessage()), e);
            }
        }

        private String escapeForQuery(String string) {
            return string.replaceAll("'", "''");
        }

        private EntryableContentMap mapToECMap(Node expandedNode, String expandableProperty, JsonBuilder config) {
            JsonBuilder builder = config.clone();
            if (builder.wrapForI18n) {
                expandedNode = new I18nNodeWrapper(expandedNode);
            }
            builder.setNode(expandedNode);
            try {
                // reset total depth in respect to current depth and position of the expanded node in its own hierarchy
                builder.totalDepth = config.totalDepth - config.node.getDepth() + expandedNode.getDepth() - 1;
            } catch (RepositoryException e) {
                log.debug("Failed to restrict depth of expanded node [" + expandedNode + "] for property [" + expandableProperty + "] with: " + e.getMessage());
            }
            builder.setReferencingPropertyName(expandableProperty);
            return new EntryableContentMap(builder);

        }

        private boolean matchesRegex(String test, Collection<String> regexList) {
            return !regexList.stream().noneMatch(test::matches);
        }

        private Stream<String> getAllMatchedRegex(String test, Collection<String> regexList) {
            if (test == null) {
                return Stream.empty();
            }
            return regexList.stream().filter(test::matches);
        }
    }

    public void setJson(String json) {
        this.preexisingJson = json;
    }

}
