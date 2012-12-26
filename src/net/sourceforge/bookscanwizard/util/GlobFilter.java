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

/*
 * Based on code from BasicFileChooserUI.java with the following license:
 * /*
 * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package net.sourceforge.bookscanwizard.util;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * A file filter which accepts file patterns containing
 * the special wildcards *? on Windows and *?[] on Unix.
 */
public class GlobFilter implements FileFilter {
    private Pattern pattern;

    public GlobFilter(String globPattern) {
        setPattern(globPattern);
    }

    /**
     * Sets the pattern with the standard operation system wildcards.
     * @param globPattern
     */
    public final void setPattern(String globPattern) {
        char[] gPat = globPattern.toCharArray();
        char[] rPat = new char[gPat.length * 2];
        boolean isWin32 = (File.separatorChar == '\\');
        boolean inBrackets = false;
        int j = 0;

        if (isWin32) {
            // On windows, a pattern ending with *.* is equal to ending with *
            int len = gPat.length;
            if (globPattern.endsWith("*.*")) {
                len -= 2;
            }
            for (int i = 0; i < len; i++) {
                switch(gPat[i]) {
                  case '*':
                    rPat[j++] = '.';
                    rPat[j++] = '*';
                    break;

                  case '?':
                    rPat[j++] = '.';
                    break;

                  case '\\':
                    rPat[j++] = '\\';
                    rPat[j++] = '\\';
                    break;

                  default:
                    if ("+()^$.{}[]".indexOf(gPat[i]) >= 0) {
                        rPat[j++] = '\\';
                    }
                    rPat[j++] = gPat[i];
                    break;
                }
            }
        } else {
            for (int i = 0; i < gPat.length; i++) {
                switch(gPat[i]) {
                  case '*':
                    if (!inBrackets) {
                        rPat[j++] = '.';
                    }
                    rPat[j++] = '*';
                    break;

                  case '?':
                    rPat[j++] = inBrackets ? '?' : '.';
                    break;

                  case '[':
                    inBrackets = true;
                    rPat[j++] = gPat[i];

                    if (i < gPat.length - 1) {
                        switch (gPat[i+1]) {
                          case '!':
                          case '^':
                            rPat[j++] = '^';
                            i++;
                            break;

                          case ']':
                            rPat[j++] = gPat[++i];
                            break;
                        }
                    }
                    break;

                  case ']':
                    rPat[j++] = gPat[i];
                    inBrackets = false;
                    break;

                  case '\\':
                    if (i == 0 && gPat.length > 1 && gPat[1] == '~') {
                        rPat[j++] = gPat[++i];
                    } else {
                        rPat[j++] = '\\';
                        if (i < gPat.length - 1 && "*?[]".indexOf(gPat[i+1]) >= 0) {
                            rPat[j++] = gPat[++i];
                        } else {
                            rPat[j++] = '\\';
                        }
                    }
                    break;

                  default:
                    //if ("+()|^$.{}<>".indexOf(gPat[i]) >= 0) {
                    if (!Character.isLetterOrDigit(gPat[i])) {
                        rPat[j++] = '\\';
                    }
                    rPat[j++] = gPat[i];
                    break;
                }
            }
        }
        this.pattern = Pattern.compile(new String(rPat, 0, j), Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean accept(File f) {
        if (f == null) {
            return false;
        }
        if (f.isDirectory()) {
            return true;
        }
        return pattern.matcher(f.getName()).matches();
    }
}
