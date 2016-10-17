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

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Consumer that splits treating of incoming data based on result of predicate evaluation.
 *
 * @param <T>
 *            Type of consumed data.
 */
public class PredicateSplitterConsumer<T> implements Consumer<T> {
    private Predicate<T> predicate;
    private Consumer<T> positive;
    private Consumer<T> negative;
    private Consumer<T> both;

    public PredicateSplitterConsumer(Predicate<T> predicate, Consumer<T> positive, Consumer<T> negative) {
        this(predicate, positive, negative, null);
    }

    public PredicateSplitterConsumer(Predicate<T> predicate, Consumer<T> positive, Consumer<T> negative, Consumer<T> both) {
        this.predicate = predicate;
        this.positive = positive;
        this.negative = negative;
        this.both = both;
    }

    @Override
    public void accept(T t) {
        if (predicate.test(t)) {
            positive.accept(t);
        } else {
            negative.accept(t);
        }
        if (both != null) {
            both.accept(t);
        }
    }
}