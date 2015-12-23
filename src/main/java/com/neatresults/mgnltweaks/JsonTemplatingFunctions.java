/**
 *
 * Copyright 2015 by Jan Haderka <jan.haderka@neatresults.com>
 *
 * This file is part of neat-tweaks module.
 *
 * Neat-tweaks is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Neat-tweaks is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with neat-tweaks.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @license GPL-3.0 <http://www.gnu.org/licenses/gpl.txt>
 *
 * Should you require distribution under alternative license in order to
 * use neat-tweaks commercially, please contact owner at the address above.
 *
 */
package com.neatresults.mgnltweaks;

import javax.jcr.Node;

import info.magnolia.jcr.util.ContentMap;

/**
 * Templating Functions to expose json builder.
 */
public class JsonTemplatingFunctions {

    public JsonTemplatingFunctions() {
    }

    /**
     * Will operate on passed in node.
     */
    public static JsonBuilder with(ContentMap content) {
        return with(content.getJCRNode());
    }

    /**
     * Will operate on passed in node.
     */
    public static JsonBuilder with(Node node) {
        JsonBuilder foo = new JsonBuilder();
        foo.setNode(node);
        return foo;
    }

    /**
     * Will skip current node, but iterate over all children of it instead.
     */
    public static JsonBuilder withChildNodesOf(ContentMap content) {
        return withChildNodesOf(content.getJCRNode());
    }

    /**
     * Will skip current node, but iterate over all children of it instead.
     */
    public static JsonBuilder withChildNodesOf(Node node) {
        JsonBuilder foo = new JsonBuilder();
        foo.setNode(node);
        foo.setChildrenOnly(true);
        return foo;
    }

}
