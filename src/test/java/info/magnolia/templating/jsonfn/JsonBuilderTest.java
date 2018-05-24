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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import info.magnolia.cms.beans.config.URI2RepositoryManager;
import info.magnolia.cms.i18n.DefaultI18nContentSupport;
import info.magnolia.cms.i18n.I18nContentSupport;
import info.magnolia.context.Context;
import info.magnolia.context.MgnlContext;
import info.magnolia.dam.templating.functions.DamTemplatingFunctions;
import info.magnolia.jcr.util.NodeTypeTemplateUtil;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.jcr.util.PropertiesImportExport;
import info.magnolia.jcr.wrapper.JCRMgnlPropertiesFilteringNodeWrapper;
import info.magnolia.link.Link;
import info.magnolia.objectfactory.Components;
import info.magnolia.objectfactory.guice.GuiceUtils;
import info.magnolia.repository.RepositoryConstants;
import info.magnolia.repository.RepositoryManager;
import info.magnolia.test.ComponentsTestUtil;
import info.magnolia.test.RepositoryTestCase;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonBuilderTest extends RepositoryTestCase {

    private final String rootTree = "/home.@type=mgnl:page\n" +
            "/home/mgnl_apex.@type=mgnl:apex\n" +
            "/home/mgnl_apex/alias.@type=mgnl:contentNode\n" +
            "/home/mgnl_apex/alias.name=a\n" +
            "/home/mgnl_apex/alias2.@type=mgnl:contentNode\n" +
            "/home/mgnl_apex/alias2.name=das\n" +
            "/home/mgnl_apex/alias2.locale=de\n" +
            "/home/mgnl_apex/alias3.@type=mgnl:contentNode\n" +
            "/home/mgnl_apex/alias3.name=this\n" +
            "/home/mgnl_apex/alias3.locale=en\n" +
            "/home/section.@type=mgnl:page\n" +
            "/home/section/mgnl_apex.@type=mgnl:apex\n" +
            "/home/section/mgnl_apex/alias.@type=mgnl:contentNode\n" +
            "/home/section/mgnl_apex/alias.name=b\n" +
            "/home/section/mgnl_apex/alias2.@type=mgnl:contentNode\n" +
            "/home/section/mgnl_apex/alias2.name=bb\n" +
            "/home/section/mgnl_apex/alias3.@type=mgnl:contentNode\n" +
            "/home/section/mgnl_apex/alias3.name=duplicate_name\n" +
            "/home/section/mgnl_apex/alias4.@type=mgnl:contentNode\n" +
            "/home/section/mgnl_apex/alias4.name=ist\n" +
            "/home/section/mgnl_apex/alias4.locale=de\n" +
            "/home/section/mgnl_apex/alias5.@type=mgnl:contentNode\n" +
            "/home/section/mgnl_apex/alias5.name=is\n" +
            "/home/section/mgnl_apex/alias5.locale=en\n" +
            "/home/section2.@type=mgnl:page\n" +
            "/home/section2/mgnl_apex.@type=mgnl:apex\n" +
            "/home/section2/mgnl_apex/alias.@type=mgnl:contentNode\n" +
            "/home/section2/mgnl_apex/alias.name=x\n" +
            "/home/section/article.@type=mgnl:page\n" +
            "/home/section/article/mgnl_apex.@type=mgnl:apex\n" +
            "/home/section/article/mgnl_apex/alias.@type=mgnl:contentNode\n" +
            "/home/section/article/mgnl_apex/alias.name=c\n" +
            "/home/section/article/mgnl_apex/alias2.@type=mgnl:contentNode\n" +
            "/home/section/article/mgnl_apex/alias2.name=duplicate_name\n" +
            "/home/section/article/mgnl_apex/alias3.@type=mgnl:contentNode\n" +
            "/home/section/article/mgnl_apex/alias3.name=c\n" +
            "/home/section/article/mgnl_apex/alias3.locale=de\n" +
            "/home/section/article/mgnl_apex/alias4.@type=mgnl:contentNode\n" +
            "/home/section/article/mgnl_apex/alias4.locale=de\n" +
            "/home/section/article/mgnl_apex/alias4.name=dec\n" +
            "/home/section/article/mgnl_apex/alias5.@type=mgnl:contentNode\n" +
            "/home/section/article/mgnl_apex/alias5.locale=de\n" +
            "/home/section/article/mgnl_apex/alias5.name=deutsch\n" +
            "/home/section/article/mgnl_apex/alias6.@type=mgnl:contentNode\n" +
            "/home/section/article/mgnl_apex/alias6.locale=en\n" +
            "/home/section/article/mgnl_apex/alias6.name=english\n" +
            "/home/section2/article.@type=mgnl:page\n" +
            "/home/section2/article/mgnl_apex.@type=mgnl:apex\n" +
            "/home/section2/article/mgnl_apex/alias.@type=mgnl:contentNode\n" +
            "/home/section2/article/mgnl_apex/alias.name=c\n";
    private Node catNode;
    private Session session;
    private JsonTemplatingFunctions templatingFunctions;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Context context = MgnlContext.getInstance();
        session = context.getJCRSession(RepositoryConstants.WEBSITE);
        // register node type
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate type = NodeTypeTemplateUtil.createSimpleNodeType(nodeTypeManager, "mgnl:apex",
                Arrays.asList(NodeType.NT_HIERARCHY_NODE, NodeType.MIX_REFERENCEABLE, NodeTypes.Created.NAME, NodeTypes.Activatable.NAME, NodeTypes.LastModified.NAME, NodeTypes.Renderable.NAME));
        nodeTypeManager.registerNodeType(type, true);

        new PropertiesImportExport().createNodes(session.getRootNode(), IOUtils.toInputStream(rootTree));

        // silly but can't use : in name in util
        session.move("/home/mgnl_apex", "/home/mgnl:apex");
        session.move("/home/section/mgnl_apex", "/home/section/mgnl:apex");
        session.move("/home/section2/mgnl_apex", "/home/section2/mgnl:apex");
        session.move("/home/section/article/mgnl_apex", "/home/section/article/mgnl:apex");
        session.move("/home/section2/article/mgnl_apex", "/home/section2/article/mgnl:apex");
        session.save();

        Components.getComponent(RepositoryManager.class).createWorkspace("magnolia", "category");
        Session catSession = MgnlContext.getInstance().getJCRSession("category");
        // register node type
        type = NodeTypeTemplateUtil.createSimpleNodeType(nodeTypeManager, "category", Arrays.asList(NodeType.NT_HIERARCHY_NODE, NodeType.MIX_REFERENCEABLE, NodeTypes.Created.NAME, NodeTypes.Activatable.NAME, NodeTypes.LastModified.NAME, NodeTypes.Renderable.NAME));
        nodeTypeManager.registerNodeType(type, true);

        catNode = catSession.getRootNode().addNode("foo", "category");
        catNode.addNode("foobar", NodeTypes.Content.NAME);
        catNode.setProperty("name", "myCategory");
        catSession.save();
        ComponentsTestUtil.setInstance(I18nContentSupport.class, new DefaultI18nContentSupport());

        templatingFunctions = new JsonTemplatingFunctions(GuiceUtils.providerForInstance(context), mock(DamTemplatingFunctions.class));
    }

    /**
     * jsonfn.from(content).add(".*").print()
     *
     * ==> { "foo" : "hahaha", "a" :"x", b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testPrintAll() throws Exception {
        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex")).add(".*").print();
        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"mgnl:created\" : "));
        assertThat(json, containsString("\"jcr:created\" : "));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).add(".*").print()
     *
     * ==> { "foo" : "hahaha", "a" :"x", b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testPrintMultiple() throws Exception {
        // GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("foo", new String[]{"baz", "bar", "boo"});

        // WHEN
        String json = templatingFunctions.from(node).add("foo").print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"foo\" : [ \"baz\", \"bar\", \"boo\" ]"));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).add("@link").print()
     *
     * ==> { "@link" : "/some/path.html" }
     */
    @Test
    public void testPrintLink() throws Exception {
        // GIVEN
        URI2RepositoryManager man = mock(URI2RepositoryManager.class);
        when(man.getURI(any(Link.class))).thenReturn("/Foo/Link/Test.html");
        ComponentsTestUtil.setInstance(URI2RepositoryManager.class, man);
        I18nContentSupport i18n = mock(I18nContentSupport.class);
        when(i18n.toI18NURI(any(String.class))).thenReturn("/Foo/Link/Test.html");
        ComponentsTestUtil.setInstance(I18nContentSupport.class, i18n);

        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex")).add("@link").print();

        // THEN
        assertThat(json, startsWith("{"));
        // starts w/ null, because we don't set any context path
        assertThat(json, containsString("\"@link\" : \"null/Foo/Link/Test.html\""));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).expand("baz", "category").print()
     *
     * ==> { "foo" : "hahaha", "baz" : {"identifier" : "1234-123456-1234", "name" : "cat1"}, b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExpand() throws Exception {
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("baz", catNode.getIdentifier());

        // WHEN
        String json = templatingFunctions.from(node).expand("baz", "category").add("@id").print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"baz\" : {"));
        assertThat(json, containsString("" + catNode.getIdentifier()));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).expand("baz", "category").print()
     *
     * ==> { "foo" : "hahaha", "baz" : {"identifier" : "1234-123456-1234", "name" : "cat1"}, b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExpandMultivalue() throws Exception {
        // GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        Session catSession = catNode.getSession();
        catSession.getWorkspace().copy(catNode.getPath(), "/othercat");
        Node catNode2 = catSession.getNode("/othercat");
        node.setProperty("baz", new String[]{catNode.getIdentifier(), catNode2.getIdentifier()});

        // WHEN
        String json = templatingFunctions.from(node).expand("baz", "category").add("@id").print();

        // THEN
        assertThat(json, startsWith("{"));
        // [{ == array of props ;)
        assertThat(json, containsString("\"baz\" : [ {"));
        assertThat(json, containsString("" + catNode.getIdentifier()));
        assertThat(json, containsString("" + catNode2.getIdentifier()));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).expand("baz", "category").print()
     *
     * ==> { "foo" : "hahaha", "baz" : {"identifier" : "1234-123456-1234", "name" : "cat1"}, b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExpandWithPropHidingWrapper() throws Exception {
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("baz", catNode.getIdentifier());
        node = new JCRMgnlPropertiesFilteringNodeWrapper(node);

        // WHEN
        String json = templatingFunctions.from(node).expand("baz", "category").add("@id").print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"baz\" : {"));
        assertThat(json, containsString("" + catNode.getIdentifier()));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).expand("baz", "category").exclude(".*:.*").print()
     *
     * ==> { "foo" : "hahaha", "baz" : {"identifier" : "1234-123456-1234", "name" : "cat1"}, b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExpandAndFilter() throws Exception {
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("baz", catNode.getIdentifier());

        // WHEN
        String json = templatingFunctions.from(node).expand("baz", "category").add("name").print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, containsString("\"baz\" : {"));
        assertThat(json, containsString("\"name\" : \"myCategory\""));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).expand("baz", "category").exclude(".*:.*").print()
     *
     * ==> { "foo" : "hahaha", "baz" : {"identifier" : "1234-123456-1234", "name" : "cat1"}, b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExpandAndFilterWithChildren() throws Exception {
        Node node = session.getNode("/home/section/article/mgnl:apex");
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            iter.nextNode().setProperty("baz", catNode.getIdentifier());
        }

        // WHEN
        String json = templatingFunctions.fromChildNodesOf(node).expand("baz", "category").add("@name").add("name").print();

        // THEN
        assertThat(json, startsWith("["));
        assertThat(json, allOf(containsString("\"alias\""), containsString("\"alias2\""), containsString("\"alias3\""), containsString("\"alias4\""), containsString("\"alias5\""), containsString("\"alias6\"")));
        assertThat(json, not(containsString("\"" + node.getIdentifier() + "\"")));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, containsString("\"baz\" : {"));
        assertThat(json, containsString("\"name\" : \"myCategory\""));
        assertThat(json, endsWith("]"));
    }

    /**
     * jsonfn.from(content).expand("baz", "category").down(4).print()
     *
     * ==> { "foo" : "hahaha", "baz" : {"identifier" : "1234-123456-1234", "name" : "cat1"}, b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExpandAndFilterAndRepeatWithChildren() throws Exception {
        // GIVEN
        Node node = session.getNode("/home/section/article/mgnl:apex");
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            iter.nextNode().setProperty("baz", catNode.getIdentifier());
        }
        node.getNode("alias2").addNode("level3", "mgnl:contentNode").addNode("level4", "mgnl:contentNode").addNode("level5", "mgnl:contentNode");

        // WHEN
        String json = templatingFunctions.fromChildNodesOf(node).expand("baz", "category").down(3).add("@name").print();

        // THEN
        assertThat(json, startsWith("["));
        assertThat(json, allOf(containsString("\"alias\""), containsString("\"alias2\""), containsString("\"alias3\""), containsString("\"alias4\""), containsString("\"alias5\""), containsString("\"alias6\"")));
        assertThat(json, not(containsString("\"" + node.getIdentifier() + "\"")));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, containsString("\"baz\" : {"));
        assertThat(json, containsString("\"@name\" : \"foo\""));
        assertThat(json, containsString("\"@name\" : \"level4\""));
        assertThat(json, not(containsString("\"@name\" : \"level5\"")));
        assertThat(json, endsWith("]"));
    }

    /**
     * jsonfn.from(content).expand("baz", "category").exclude(".*:.*").print()
     *
     * ==> { "foo" : "hahaha", "baz" : {"identifier" : "1234-123456-1234", "name" : "cat1"}, b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExpandAndExcludeAndFilter() throws Exception {
        //GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("baz", catNode.getIdentifier());

        // WHEN
        String json = templatingFunctions.from(node).expand("baz", "category").add("@id", "name", "baz").print();

        //THEN
        assertThat(json, startsWith("{"));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, containsString("\"baz\" : {"));
        assertThat(json, containsString("\"name\" : \"myCategory\""));
        assertThat(json, containsString("" + catNode.getIdentifier()));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).exclude(".*foo").print()
     *
     * ==> { "a" :"x", b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExcludes() throws Exception {
        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex")).add("mgnl:.*").print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"mgnl:created\" : "));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).exclude(".*foo", "bar.*").print()
     *
     * ==> { "a" :"x", b: 1234, ... }
     */
    @Test
    public void testExcludes2() throws Exception {
        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex")).add(".*").exclude("jcr:.*", "mgnl:.*", "@.*", "name", "alias").print();

        // THEN
        assertEquals("{ }", json);
    }

    /**
     * jsonfn.from(content).exclude(".*").print()
     *
     * ==> { }
     */
    @Test
    public void testExcludes3() throws Exception {
        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex")).exclude(".*").print();

        // THEN
        assertEquals("{ }", json);
    }

    /**
     * jsonfn.from(content).add("someProperty").print()
     *
     * ==> { "someProperty" : "someValue" }
     */
    @Test
    public void testExcludes4() throws Exception {
        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex")).down(0).add("mgnl:createdBy").print();

        // THEN
        assertEquals("{\n" +
                "  \"mgnl:createdBy\" : \"anonymous\"\n" +
                "}", json);
    }

    /**
     * jsonfn.from(content).add(".*").print()
     *
     * ==> { "foo" : "hahaha", "a" :"x", b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testPrintAllWithAppend() throws Exception {
        // GIVEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex")).down(2).add("name").print();

        // WHEN
        json = templatingFunctions.appendFrom(json, session.getNode("/home/mgnl:apex")).down(2).add("name").print();

        // THEN
        assertThat(json, startsWith("["));
        assertThat(json, containsString("\"name\" : \"c\""));
        assertThat(json, containsString("\"name\" : \"a\""));
        assertThat(json, endsWith("]"));
    }

    /**
     * jsonfn.from(content).add(".*").print()
     *
     * ==> [{ "foo" : "hahaha", "a" :"x", b: 1234, "bar" : "meh", ... }, ...]
     */
    @Test
    public void testPrintAllWithAppendToEmptyArray() throws Exception {
        // GIVEN
        String json = "[]";

        // WHEN
        json = templatingFunctions.appendFrom(json, session.getNode("/home/mgnl:apex")).down(2).add("name").print();

        // THEN
        assertThat(json, startsWith("["));
        assertThat(json, not(containsString("[,{")));
        assertThat(json, containsString("\"name\" : \"a\""));
        assertThat(json, endsWith("]"));
    }

    /**
     * to output all in single line:
     *
     * jsonfn.from(content).add(".*").inline().print()
     *
     * ==> { "foo" : "hahaha", "a" :"x", b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testPrintInline() throws Exception {
        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex")).down(2).add("name").inline().print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"name\":\"c\""));
        assertThat(json, endsWith("}"));
        assertThat(json, not(containsString("\n")));
    }

    /**
     * to output all stuff from root, jcr:xxx node types should be skipped:
     *
     * jsonfn.from(root).add(".*").print()
     *
     * ==> { "foo" : "hahaha", "a" :"x", b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExcludeSystemNodes() throws Exception {
        // WHEN
        String json = templatingFunctions.fromChildNodesOf(session.getNode("/")).down(2).add("@name").inline().print();

        // THEN
        assertThat(json, startsWith("["));
        assertThat(json, containsString("\"@name\":\"home\""));
        assertThat(json, endsWith("]"));
        assertThat(json, not(containsString("jcr:system")));
    }

    /**
     * jsonfn.from(content).add(".*").inline().print()
     *
     * ==> { "foo" : "that\\'s it", ... }
     */
    @Test
    public void testPrintDoubleQuoted() throws Exception {
        // GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("escape", "that\"s it");

        // WHEN
        String json = templatingFunctions.from(node).add("escape").inline().escapeBackslash().print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"escape\":\"that\\\\\"s it\""));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).add(".*").inline().print()
     *
     * ==> { "foo" : "that\\'s it", ... }
     */
    @Test
    public void testPrintDoubleQuoted2() throws Exception {
        // GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("description",
                "<p><span style=\"line-height:1.6em\">l&auml;ngsten produzierten Profikameras &uuml;berhaupt.</span></p><p><span style=\"line-height:1.6em\">Mit der F3 !</span></p>");

        // WHEN
        String json = templatingFunctions.from(node).add("description").inline().print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString(
                "\"description\":\"<p><span style=\\\"line-height:1.6em\\\">l&auml;ngsten produzierten Profikameras &uuml;berhaupt.</span></p><p><span style=\\\"line-height:1.6em\\\">Mit der F3 !</span></p>\""));
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).add(".*").inline().print()
     *
     * ==> { "foo" : "that\\'s it", ... }
     */
    @Test
    public void testPrintQuoted() throws Exception {
        // GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("escape", "that\"s it");

        // WHEN
        String json = templatingFunctions.from(node).add("escape").inline().escapeBackslash().print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"escape\":\"that\\\\\"s it\""));
        assertThat(json, endsWith("}"));
    }

    /**
     * to output all stuff from root, jcr:xxx node types should be skipped:
     *
     * jsonfn.from(root).add(".*").print()
     *
     * ==> { "foo" : "hahaha", "a" :"x", b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testLimitByTypesOfNodes() throws Exception {
        // WHEN
        String json = templatingFunctions.fromChildNodesOf(session.getNode("/")).down(2).readNodeTypes("rep:system").add("@name").inline().print();

        // THEN
        assertThat(json, startsWith("["));
        assertThat(json, not(containsString("\"@name\":\"home\"")));
        assertThat(json, endsWith("]"));
        assertThat(json, containsString("jcr:system"));
    }

    /**
     * to output all stuff from root, jcr:xxx node types should be skipped:
     *
     * jsonfn.from(root).add(".*").print()
     *
     * ==> { "foo" : "hahaha", "a" :"x", b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testFlattenOutputByTypesOfSingleNode() throws Exception {
        //GIVEN
        session.getNode("/home").addNode("test_above_apex", "mgnl:folder").addNode("apex_under_test", "mgnl:apex");

        // WHEN
        String json = templatingFunctions.from(session.getNode("/")).down(2).allowOnlyNodeTypes(".*apex").add("@name", "@nodeType", "@path", "@depth").print();

        // THEN
        assertThat(json, startsWith("["));
        assertThat(json.substring(1, json.length() - 1), not(containsString("[")));
        assertThat(json, endsWith("]"));
        assertThat(json, not(containsString("mgnl:folder")));
        assertThat(json, not(containsString("apex_under_test")));
        assertThat(json, containsString("mgnl:apex"));

    }

    /**
     * to output all stuff from root, jcr:xxx node types should be skipped:
     *
     * jsonfn.from(root).add(".*").print()
     *
     * ==> { "foo" : "hahaha", "a" :"x", b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testFlattenOutputByTypesOfNodes() throws Exception {
        //GIVEN
        session.getNode("/home").addNode("test_above_apex", "mgnl:folder").addNode("apex_under_test", "mgnl:apex");

        // WHEN
        String json = templatingFunctions.fromChildNodesOf(session.getNode("/home")).down(2).allowOnlyNodeTypes(".*apex").add("@name", "@nodeType", "@path").print();

        // THEN
        assertThat(json, startsWith("["));
        assertThat(json, not(containsString("\"@name\":\"home\"")));
        assertThat(json, containsString("\"@name\" : \"mgnl:apex\""));
        assertThat(json, endsWith("]"));
        assertThat(json, containsString("/home/test_above_apex/apex_under_test"));
    }

    /**
     * Mask chars not friendly to your js framework in names of objects and properties jsonfn.from(content).add(".*").maskChars(":","_").print()
     *
     * ==> { "a" :"x", b: 1234, "b_r" : "property called b:r", ... }
     */
    @Test
    public void testMaskChars() throws Exception {
        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex")).add("mgnl:.*").add("@name").maskChar(':', '_').print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"mgnl_created\" : "));
        assertThat(json, not(containsString("\"mgnl:created\" : ")));
        assertThat(json, endsWith("}"));
    }

    /**
     * Lists specified properties only for sub nodes but not for the parent nodes.
     *
     */
    @Test
    public void testSubNodePropertyListing() throws Exception {
        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article")).down(1).add("mgnl:.*").add("mgnl:apex['@name']").maskChar(':', '_').print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"mgnl_created\" : "));
        assertThat(json, not(containsString("\"@name\" : \"article\"")));
        assertThat(json, containsString("\"@name\" : \"mgnl:apex\""));
        assertThat(json, not(containsString("\"mgnl:created\" : ")));
        assertThat(json, endsWith("}"));
    }

    /**
     * Lists specified properties only for expanded nodes but not for the parent nodes.
     */
    @Test
    public void testExpandedNodePropertyListing() throws Exception {
        //GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("baz", catNode.getIdentifier());
        node.addNode("blah", NodeTypes.Content.NAME);

        // WHEN
        String json = templatingFunctions.from(node).expand("baz", "category").add("name").add("baz['@name']").down(2).print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, not(containsString("\"@name\" : \"mgnl:apex\"")));
        assertThat(json, not(containsString("\"@name\" : \"blah\"")));
        assertThat(json, containsString("\"baz\" : {"));
        assertThat(json, containsString("\"@name\" : \"foo\""));
        assertThat(json, containsString("\"name\" : \"myCategory\""));
        assertThat(json, endsWith("}"));
    }

    @Test
    public void testChildrenAsArrayNoMatch() throws Exception {
        //GIVEN
        Node node = session.getNode("/home/section2/article");
        Node multiParent = node.addNode("multiParent", NodeTypes.Content.NAME);
        multiParent.setProperty("foo", "bar");
        multiParent.addNode("testA", NodeTypes.Content.NAME);
        multiParent.addNode("testB", NodeTypes.Content.NAME);
        multiParent.addNode("testC", NodeTypes.Content.NAME);

        //WHEN
        String noArray = templatingFunctions.from(node).add("name").down(2).print();
        String noArrayByRegex = templatingFunctions.from(node).add("name").childrenAsArray(".*test.*", ".*whatever.*").down(2).print();
        String withArray = templatingFunctions.from(node).add("name").childrenAsArray("foo", "bar").down(3).print();

        //THEN
        assertFalse(noArray.contains("["));
        assertFalse(noArray.contains("]"));
        assertFalse(noArrayByRegex.contains("["));
        assertFalse(noArrayByRegex.contains("]"));
        assertTrue(withArray.contains("["));
        assertTrue(withArray.contains("]"));
        assertEquals("{\n"
                + "  \"mgnl:apex\" : {\n"
                + "    \"alias\" : {\n"
                + "      \"name\" : \"c\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"multiParent\" : [ { }, { }, { } ]\n"
                + "}", withArray);
    }

    @Test
    public void testChildrenAsArrayMatch() throws Exception {
        //GIVEN
        Node node = session.getNode("/home/section2/article");
        Node multiParent = node.addNode("multiParent", NodeTypes.Content.NAME);
        multiParent.setProperty("foo", "bar");
        multiParent.addNode("testA", NodeTypes.Content.NAME);
        multiParent.addNode("testB", NodeTypes.Content.NAME);
        multiParent.addNode("testC", NodeTypes.Content.NAME);

        //WHEN
        // check that the nodes matching the property (name) and value are included when present.
        String withArrayByValueRegex = templatingFunctions.from(node).add("name").childrenAsArray("@name", "test.*").down(2).print();
        String withArrayByNameRegex = templatingFunctions.from(node).add("name").childrenAsArray(".*oo.*", "bar").down(2).print();

        //THEN
        assertTrue(withArrayByValueRegex.contains("["));
        assertTrue(withArrayByValueRegex.contains("]"));
        assertTrue(withArrayByNameRegex.contains("["));
        assertTrue(withArrayByNameRegex.contains("]"));
        assertEquals("{\n"
                + "  \"mgnl:apex\" : {\n"
                + "    \"alias\" : {\n"
                + "      \"name\" : \"c\"\n"
                + "    }\n"
                + "  },\n"
                + "  \"multiParent\" : {\n"
                + "    \"testA\" : [ ],\n"
                + "    \"testB\" : [ ],\n"
                + "    \"testC\" : [ ]\n"
                + "  }\n"
                + "}", withArrayByValueRegex);
    }

    @Test
    public void testInsertCustom() throws Exception {
        //GIVEN
        Node node = session.getNode("/home/section2/article");
        Node customContainer = node.addNode("customContainer", NodeTypes.Content.NAME);
        customContainer.setProperty("aa", "bb");

        String myArray = "[ \"one\", \"two\", \"three\" ]";
        String myObject = "{ \"foo\" : \"bar\" }";
        String myNumber = "99";

        //WHEN
        String customArray = templatingFunctions.from(node).addAll().insertCustom("article/customContainer", myArray).down(2).inline().print();
        String customObject = templatingFunctions.from(node).addAll().insertCustom("article/customContainer", myObject).down(2).inline().print();
        String customNumber = templatingFunctions.from(node).addAll().insertCustom("article/customContainer", myNumber).down(2).inline().print();
        String noReplacement = templatingFunctions.from(node).addAll().insertCustom("nonexisting/path", myNumber).down(2).inline().print();
        String replacedAtProperty = templatingFunctions.from(node).addAll().insertCustom("article/customContainer/aa", myObject).down(2).inline().print();

        //THEN
        assertTrue(customArray.contains("\"customContainer\":[\"one\",\"two\",\"three\"]"));
        assertTrue(customObject.contains("\"customContainer\":{\"foo\":\"bar\"}"));
        assertTrue(customNumber.contains("\"customContainer\":99"));
        assertTrue(noReplacement.contains("\"aa\":\"bb\""));
        assertThat(replacedAtProperty, containsString("\"aa\":{\"foo\":\"bar\"}"));
    }

    /**
     * Lists specified properties only for expanded nodes but not for the parent nodes.
     */
    @Test
    public void testMultiExpandNodePropertyListing() throws Exception {
        //GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.addNode("blah", NodeTypes.Content.NAME);
        catNode.setProperty("fooId", "123");
        Session catSession = catNode.getSession();
        Node cn2 = catSession.getRootNode().addNode("foo2name", "category");
        cn2.setProperty("fooId", "456");
        Node cn3 = catSession.getRootNode().addNode("foo3name", "category");
        cn3.setProperty("fooId", "789");
        catSession.save();

        node.setProperty("foo1", "123");
        node.setProperty("foo2", "456");
        node.setProperty("foo3", "789");

        session.getWorkspace().copy(node.getPath(), node.getParent().getPath() + "/test2");
        session.getWorkspace().copy(node.getPath(), node.getParent().getPath() + "/test3");

        // WHEN
        String json = templatingFunctions.from(node).expand("foo.", "category", "fooId").add("name", "@name").down(1).print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, not(containsString("\"foobar\" : {")));
        assertThat(json, containsString("\"@name\" : \"foo\""));
        assertThat(json, containsString("\"@name\" : \"foo2name\""));
        assertThat(json, containsString("\"@name\" : \"foo3name\""));
        assertEquals(1, StringUtils.countMatches(json, "foo3name"));
        assertThat(json, endsWith("}"));
    }

    /**
     * Lists specified properties only for expanded nodes but not for the parent nodes.
     */
    @Test
    public void testMultiExpandNodeMultiPropertiesListing() throws Exception {
        //GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.addNode("blah", NodeTypes.Content.NAME);
        catNode.setProperty("fooId", "123");
        Session catSession = catNode.getSession();
        Node cn2 = catSession.getRootNode().addNode("foo2name", "category");
        // this guy contains all and we should not allow duplicates
        cn2.setProperty("fooId", new String[]{"456", "789", "123"});
        Node cn3 = catSession.getRootNode().addNode("foo3name", "category");
        cn3.setProperty("fooId", "789");
        catSession.save();

        node.setProperty("foo1", "123");
        node.setProperty("foo2", "456");
        node.setProperty("foo3", "789");

        session.getWorkspace().copy(node.getPath(), node.getParent().getPath() + "/test2");
        session.getWorkspace().copy(node.getPath(), node.getParent().getPath() + "/test3");

        // WHEN
        String json = templatingFunctions.from(node).expand("foo.", "category", "fooId").maskChar('.', 'x').add("name", "@name").down(1).print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, not(containsString("\"foobar\" : {")));
        assertThat(json, not(containsString("\"foo.\" : {")));
        assertThat(json, containsString("\"foox\" : [ {"));
        assertThat(json, containsString("\"@name\" : \"foo\""));
        assertThat(json, containsString("\"@name\" : \"foo2name\""));
        assertThat(json, containsString("\"@name\" : \"foo3name\""));
        assertEquals(1, StringUtils.countMatches(json, "foo2name"));
        assertThat(json, endsWith("}"));
    }

    /**
     * Lists specified properties only for expanded nodes but not for the parent nodes.
     */
    @Test
    public void testMultiExpandNodeMultiPropertiesListingFromMultiValueProperty() throws Exception {
        //GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.addNode("blah", NodeTypes.Content.NAME);
        catNode.setProperty("fooId", "123");
        Session catSession = catNode.getSession();
        Node cn2 = catSession.getRootNode().addNode("foo2name", "category");
        // this guy contains all and we should not allow duplicates
        cn2.setProperty("fooId", new String[]{"456", "789", "123"});
        Node cn3 = catSession.getRootNode().addNode("foo3name", "category");
        cn3.setProperty("fooId", "789");
        catSession.save();

        node.setProperty("foox", new String[]{"123", "456", "789"});

        session.getWorkspace().copy(node.getPath(), node.getParent().getPath() + "/test2");
        session.getWorkspace().copy(node.getPath(), node.getParent().getPath() + "/test3");

        // WHEN
        String json = templatingFunctions.from(node).expand("foox", "category", "fooId").add("name", "@name").down(1).print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, not(containsString("\"foobar\" : {")));
        assertThat(json, not(containsString("\"foo.\" : {")));
        assertThat(json, containsString("\"foox\" : [ {"));
        assertThat(json, containsString("\"@name\" : \"foo\""));
        assertThat(json, containsString("\"@name\" : \"foo2name\""));
        assertThat(json, containsString("\"@name\" : \"foo3name\""));
        assertEquals(1, StringUtils.countMatches(json, "foo2name"));
        assertThat(json, endsWith("}"));
    }

    @Test
    public void nodeOrderIsPreserved() throws Exception {
        //GIVEN
        Node node = session.getRootNode().addNode("test");
        Set<String> subNodes = new LinkedHashSet(Arrays.asList("test3", "test1", "test2"));
        for (String subNode : subNodes) {
            node.addNode(subNode);
        }

        // WHEN
        String json = templatingFunctions.from(node).add("test(.+)['@path']").down(1).print();
        Map<String, Object> jsonMap = new ObjectMapper().readValue(json, LinkedHashMap.class);

        // THEN
        assertEquals(jsonMap.keySet(), subNodes);
    }

    /**
     * Lists specified properties only for sub nodes but not for the parent nodes.
     */
    @Test
    public void testSubNodePropertyListingWithExclusionOfParent() throws Exception {
        // GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex/alias");
        node.addNode("tab0", NodeTypes.Content.NAME);
        node.addNode("tab1", NodeTypes.Content.NAME);
        node.addNode("tab2", NodeTypes.Content.NAME);
        session.save();

        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex/alias")).down(1).add( "tab(.+)['@id']").inline().print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"tab1\":{\"@id\""));
        assertThat(json, not(containsString("},\"@id\":")));
        assertThat(json, containsString("},\"tab2\":{\"@id\":"));
        assertThat(json, endsWith("}"));
    }

    /**
     * Lists and renames properties and nodes.
     */
    @Test
    public void testExpandedNodePropertyListingWithRenames() throws Exception {
        //GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        node.setProperty("baz", catNode.getIdentifier());
        node.addNode("blah", NodeTypes.Content.NAME);
        session.save();

        // WHEN
        String json = templatingFunctions.from(node).expand("baz", "category").renameKey("baz", "booz").renameKey("@name", "ren@name").renameKey("..me", "wasName").add("name").add("baz['@name']").down(2).print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, not(containsString("\"jcr:created\" : ")));
        assertThat(json, not(containsString("\"ren@name\" : \"mgnl:apex\"")));
        assertThat(json, not(containsString("\"ren@name\" : \"blah\"")));
        assertThat(json, containsString("\"booz\" : {"));
        assertThat(json, containsString("\"ren@name\" : \"foo\""));
        assertThat(json, containsString("\"wasName\" : \"myCategory\""));
        assertThat(json, endsWith("}"));
    }

    /**
     * Lists specified properties only for parent nodes but not for the subnodes.
     */
    @Test
    public void testSubNodeWithSamePropertyAsOneOfAncestors() throws Exception {
        Session session = MgnlContext.getInstance().getJCRSession(RepositoryConstants.WEBSITE);

        // GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex/alias");
        node.setProperty("abstract", "foo");
        Node child = node.addNode("linkInternal", NodeTypes.Content.NAME);
        child.setProperty("abstract", "foo");

        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex/alias")).down(3).add("alias['abstract']").exclude("linkInternal['abstract']").inline().print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("{\"abstract\":\"foo\"}"));
        assertThat(json, endsWith("}"));
    }

    /**
     * Lists specified properties only for parent nodes but not for the subnodes.
     */
    @Test
    public void testSubNodeWithSystemPropertyNameAsOneOfAncestors() throws Exception {
        Session session = MgnlContext.getInstance().getJCRSession(RepositoryConstants.WEBSITE);

        // GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex/alias");
        node.setProperty("abstract", "foo");
        Node child = node.addNode("linkInternal", NodeTypes.Content.NAME);
        child.setProperty("abstract", "foo");

        // WHEN
        String json = templatingFunctions.from(session.getNode("/home/section2/article/mgnl:apex/alias")).down(3).add("alias['@id']").exclude("linkInternal['@id']").inline().print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("{\"@id\":\""));
        assertEquals(json.split("id").length, 2);
        assertThat(json, endsWith("}"));
    }

    /**
     * jsonfn.from(content).expand("baz", "category").print() when baz is deleted
     *
     * ==> { "foo" : "hahaha", "baz" : {"identifier" : "1234-123456-1234", "name" : "cat1"}, b: 1234, "bar" : "meh", ... }
     */
    @Test
    public void testExpandDeleted() throws Exception {

        // GIVEN
        Node node = session.getNode("/home/section2/article/mgnl:apex");
        catNode.addMixin(NodeTypes.Deleted.NAME);
        node.setProperty("baz", catNode.getIdentifier());

        // WHEN
        String json = templatingFunctions.from(node).expand("baz", "category").add("@id").print();

        // THEN
        assertThat(json, startsWith("{"));
        assertThat(json, containsString("\"baz\" : null"));
        assertThat(json, not(containsString("" + catNode.getIdentifier())));
        assertThat(json, endsWith("}"));
    }

}
