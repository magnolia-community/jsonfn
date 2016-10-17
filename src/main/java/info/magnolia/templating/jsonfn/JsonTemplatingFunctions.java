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

import info.magnolia.context.Context;
import info.magnolia.jcr.util.ContentMap;
import info.magnolia.objectfactory.Components;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Templating Functions to expose json builder.
 */
@Singleton
public class JsonTemplatingFunctions {

    private static final Logger log = LoggerFactory.getLogger(JsonTemplatingFunctions.class);

    private final Provider<Context> contextProvider;

    @Inject
    public JsonTemplatingFunctions(final Provider<Context> contextProvider) {
        this.contextProvider = contextProvider;
    }

    /**
     * Old deprecated constructor.
     *
     * @deprecated since 1.0.7, use {@link #JsonTemplatingFunctions(Provider<Context>)} instead.
     */
    @Deprecated
    public JsonTemplatingFunctions() {
        this(new Provider<Context>() {
            @Override
            public Context get() {
                return Components.getComponent(Context.class);
            }
        });
    }

    /**
     * Will operate on passed in node.
     */
    public JsonBuilder from(ContentMap content) {
        return from(content.getJCRNode());
    }

    /**
     * Will operate on passed in node.
     */
    public JsonBuilder from(Node node) {
        JsonBuilder foo = new JsonBuilder();
        foo.setNode(node);
        return foo;
    }

    /**
     * Will skip root node of the workspace, but iterate over all children of it instead.
     */
    public JsonBuilder fromChildNodesOf(String workspace) {
        try {
            return fromChildNodesOf(contextProvider.get().getJCRSession(workspace).getRootNode());
        } catch (RepositoryException e) {
            // ignore - if root node is not accessible, repo is broken or user is denied access. Either way, there's nothing to show for it and no reason to spit out extra stuff in log files, this will not escape anyones notice.
            log.debug("Repository could not be accessed due:" + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Will skip current node, but iterate over all children of it instead.
     */
    public JsonBuilder fromChildNodesOf(ContentMap content) {
        return fromChildNodesOf(content.getJCRNode());
    }

    /**
     * Will skip current node, but iterate over all children of it instead.
     */
    public JsonBuilder fromChildNodesOf(Node node) {
        JsonBuilder foo = new JsonBuilder();
        foo.setNode(node);
        foo.setChildrenOnly(true);
        return foo;
    }

    /**
     * Will operate on passed in node and append output to provided json.
     */
    public JsonBuilder appendFrom(String json, ContentMap content) {
        return appendFrom(json, content.getJCRNode());
    }

    /**
     * Will operate on passed in node.
     */
    public JsonBuilder appendFrom(String json, Node node) {
        JsonBuilder foo = new JsonBuilder();
        foo.setNode(node);
        foo.setJson(json);
        return foo;
    }

}
