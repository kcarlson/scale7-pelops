/*
 * The MIT License
 *
 * Copyright (c) 2011 Dominic Williams, Daniel Washusen and contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.scale7.cassandra.pelops;

import org.apache.cassandra.thrift.*;

import java.util.ArrayList;
import java.util.List;

import static org.apache.cassandra.thrift.ColumnOrSuperColumn._Fields;

/**
 * Contains helper methods for dealing with ColumnOrSuperColumn objects.
 *
 * @author Yaniv Kunda
 */
public class ColumnOrSuperColumnHelper {

    private static abstract class FieldAdapter<T> {

        private final String description;

        public FieldAdapter(ColumnOrSuperColumn._Fields field) {
            description = field.getFieldName().replace('_', ' ');
        }

        public abstract T getValue(ColumnOrSuperColumn cosc);
    }

    public static FieldAdapter<Column> COLUMN = new FieldAdapter<Column>(_Fields.COLUMN) {
        public Column getValue(ColumnOrSuperColumn cosc) { return cosc.column;}
    };

    public static FieldAdapter<SuperColumn> SUPER_COLUMN = new FieldAdapter<SuperColumn>(_Fields.SUPER_COLUMN) {
        public SuperColumn getValue(ColumnOrSuperColumn cosc) { return cosc.super_column;}
    };

    public static FieldAdapter<CounterColumn> COUNTER_COLUMN = new FieldAdapter<CounterColumn>(_Fields.COUNTER_COLUMN) {
        public CounterColumn getValue(ColumnOrSuperColumn cosc) { return cosc.counter_column;}
    };

    public static FieldAdapter<CounterSuperColumn> COUNTER_SUPER_COLUMN = new FieldAdapter<CounterSuperColumn>(_Fields.COUNTER_SUPER_COLUMN) {
        public CounterSuperColumn getValue(ColumnOrSuperColumn cosc) { return cosc.counter_super_column;}
    };

    public static <T> List<T> transform(List<ColumnOrSuperColumn> coscList, FieldAdapter<T> fieldAdapter) {
        List<T> result = new ArrayList<T>(coscList.size());
        for (ColumnOrSuperColumn cosc : coscList) {
            T element = fieldAdapter.getValue(cosc);
            assert element != null : "The " + fieldAdapter.description + " should not be null";
            result.add(element);
        }
        return result;
    }

}
