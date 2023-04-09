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

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class BlockingLifoQueue<E> extends LinkedBlockingDeque<E> {

    @Override
    public boolean offer(E e) {
        // override to put objects at the front of the list
        return super.offerFirst(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        // override to put objects at the front of the list
        return super.offerFirst(e, timeout, unit);
    }

    @Override
    public boolean add(E e) {
        // override to put objects at the front of the list
        return super.offerFirst(e);
    }

    @Override
    public void put(E e) throws InterruptedException {
        // override to put objects at the front of the list
        super.putFirst(e);
    }
}
