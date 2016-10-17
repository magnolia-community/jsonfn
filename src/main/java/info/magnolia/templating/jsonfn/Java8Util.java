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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

/**
 * Some utils for making life w/ java 8 easier. Copied from Neat-Tweaks. This should eventually end up where rest of utils for manipulating JCR APIs are ... unless someone can think of better way to do the same.
 */
public final class Java8Util {

    private Java8Util() {
    }

    /**
     * Turns an iterator into stream.
     */
    public static <T> Stream<T> asStream(Iterator<T> iterator) {
        int characteristics = Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, characteristics);

        boolean parallel = false;
        Stream<T> stream = StreamSupport.stream(spliterator, parallel);
        return stream;
    }

    /**
     * Turns a PropertyIterator into Property stream.
     */
    @SuppressWarnings("unchecked")
    public static Stream<Property> asPropertyStream(PropertyIterator properties) {
        return asStream(properties);
    }

    /**
     * Turns a NodeIterator into Node stream.
     */
    @SuppressWarnings("unchecked")
    public static Stream<Node> asNodeStream(NodeIterator nodes) {
        return asStream(nodes);
    }

    /**
     * Returns name of the Property or null when property can't be accessed.
     */
    public static String getName(Property p) {
        try {
            return p.getName();
        } catch (RepositoryException e) {
            return null;
        }
    }

    /**
     * Returns name of the Node or null when node can't be accessed.
     */
    public static String getName(Node n) {
        try {
            return n.getName();
        } catch (RepositoryException e) {
            return null;
        }
    }

    /**
     * Returns path of the Node or null when node can't be accessed.
     */
    public static String getPath(Node n) {
        try {
            return n.getPath();
        } catch (RepositoryException e) {
            return null;
        }
    }

    /**
     * Returns path of the Property or null when property can't be accessed.
     */
    public static String getPath(Property p) {
        try {
            return p.getPath();
        } catch (RepositoryException e) {
            return null;
        }
    }
}
