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
package net.sourceforge.bookscanwizard.op;

import java.awt.image.RenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;

/**
 * Adjusts gamma so that so that the given color is moved to the mid-point.
 */
public class Gamma extends Operation implements ColorOp {
    private static final Logger logger = Logger.getLogger(Gamma.class.getName());

    private byte[][] gammaTable = null;
    
    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        if (gammaTable == null) {
            int bands = getArgs().length;
            gammaTable = new byte[bands][];
            double avg =0;
            for (int i=0; i < bands; i++) {
                avg+= getArgs()[i];
            }
            avg /= bands;
            for (int i = 0; i < bands; i++) {
                double original = getArgs()[i] * 255 / 100;
                double gamma = calcAdjustment(original, GRAY_STANDARD);
                logger.log(Level.INFO, "gamma: {0} {1} {2}", new Object[]{original, gamma, adjust(original, gamma)});
                byte[] gammaRow = new byte[256];
                for (int j = 0; j <= 255; j++) {
                    gammaRow[j] = (byte) Math.round(adjust(j, gamma));
                }
                gammaTable[i] = gammaRow;
            }
        }
        LookupTableJAI table = new LookupTableJAI(gammaTable);
        img = JAI.create("lookup", img, table);
        return img;
    }

    public int adjust(double value, double gamma) {
        return (int) Math.round(Math.pow(value / 255.0D, 1.0D / gamma) * 255.0D);
    }

    public double calcAdjustment(double originalValue, int targetValue) {
        double guess = 5.0D;
        double mult = guess;
        while (mult > 0.000001D) {
            int test = adjust(originalValue, guess);
            if (test < targetValue) {
                guess += mult;
            } else if (test > targetValue) {
                guess -= mult;
            } else {
                return guess;
            }
            mult /= 2.0D;
        }
        return guess > 5.0D ? 10.0D : 0.0D;
    }
}
