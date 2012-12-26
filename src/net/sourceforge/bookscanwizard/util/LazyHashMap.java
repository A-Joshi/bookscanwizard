/*
 *
 * Copyright (c) 2013 by Steve Devore
 *                       http://bookscanwizard.sourceforge.net
 *
 * This file is part of the Book Scan Wizard.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package net.sourceforge.bookscanwizard.util;

import java.util.Collection;
import java.util.HashMap;

public class LazyHashMap<K,V> extends HashMap<K, V> {
    private Class cls;
    
    public LazyHashMap(Class cls) {
        this.cls = cls;
    }

    public V getOrCreate(K key) {
        V value = get(key);
        if (value == null) {
            try {
                value = (V) cls.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            put(key, value);
        }
        return value;
    }

    public Object getFirstItem(String key) {
        Collection value = (Collection) get(key);
        if (value != null && value.iterator().hasNext()) {
            return value.iterator().next();
        } else {
            return null;
        }
    }
}
