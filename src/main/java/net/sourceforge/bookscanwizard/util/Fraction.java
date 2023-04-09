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

/**
 * A class for converting between fractions and decimals.
 */
public final class Fraction implements Comparable<Fraction> {
    protected long numerator;
    protected long denominator;

    public long getNumerator() {
        return numerator;
    }

    public long getDenominator() {
        return denominator;
    }

    public Fraction(double num, long maxDenominator) {
        init(num, maxDenominator);
    }

    public Fraction(String fractionStr) {
        String[] fraction = fractionStr.split("/");
        if (fraction.length < 2) {
            init(Double.parseDouble(fraction[0]), 64L);
        } else {
            long _denominator = Long.parseLong(fraction[1]);
            String[] mixed = fraction[0].split(" ");
            long _numerator = Long.parseLong(mixed[(mixed.length - 1)]);
            if (mixed.length > 1) {
                numerator += Long.parseLong(mixed[0]) * denominator;
            }
            init(numerator, denominator);
        }
    }

    public Fraction(long num, long den) {
        init(num, den);
    }

    private void init(double num, long maxDenominator) {
        init(Math.round(num * maxDenominator), maxDenominator);
    }

    private void init(long num, long den) {
        if (den < 0L) {
            den *= -1L;
            num *= -1L;
        }

        long g = gcd(num, den);
        numerator = (num / g);
        denominator = (den / g);
    }

    public Fraction(Fraction f) {
        this.numerator = f.numerator;
        this.denominator = f.denominator;
    }

    @Override
    public String toString() {
        if (denominator == 1L) {
            return "" + numerator;
        }
        if (Math.abs(numerator) >= denominator) {
            long whole = numerator / denominator;
            long fraction = numerator % denominator;
            return whole + " " + new Fraction(fraction, denominator);
        }
        return numerator + "/" + denominator;
    }

    public double asDouble() {
        return numerator / denominator;
    }

    public static long gcd(long a, long b) {
        long x = Math.abs(a);
        long y = Math.abs(b);
        if (y > x) {
            long t = x;
            x = y;
            y = t;
        }
        while (y != 0L) {
            long t = x % y;
            x = y;
            y = t;
        }
        return x;
    }

    public Fraction negative() {
        long an = numerator;
        long ad = denominator;
        return new Fraction(-an, ad);
    }

    public Fraction inverse() {
        long an = numerator;
        long ad = denominator;
        return new Fraction(ad, an);
    }

    public Fraction plus(Fraction b) {
        long an = numerator;
        long ad = denominator;
        long bn = b.numerator;
        long bd = b.denominator;
        return new Fraction(an * bd + bn * ad, ad * bd);
    }

    public Fraction plus(long n) {
        long an = numerator;
        long ad = denominator;
        long bn = n;
        long bd = 1L;
        return new Fraction(an * bd + bn * ad, ad * bd);
    }

    public Fraction minus(Fraction b) {
        long an = numerator;
        long ad = denominator;
        long bn = b.numerator;
        long bd = b.denominator;
        return new Fraction(an * bd - bn * ad, ad * bd);
    }

    public Fraction minus(long n) {
        long an = numerator;
        long ad = denominator;
        long bn = n;
        long bd = 1L;
        return new Fraction(an * bd - bn * ad, ad * bd);
    }

    public Fraction times(Fraction b) {
        long an = numerator;
        long ad = denominator;
        long bn = b.numerator;
        long bd = b.denominator;
        return new Fraction(an * bn, ad * bd);
    }

    public Fraction times(long n) {
        long an = numerator;
        long ad = denominator;
        long bn = n;
        long bd = 1L;
        return new Fraction(an * bn, ad * bd);
    }

    public Fraction dividedBy(Fraction b) {
        long an = numerator;
        long ad = denominator;
        long bn = b.numerator;
        long bd = b.denominator;
        return new Fraction(an * bd, ad * bn);
    }

    public Fraction dividedBy(long n) {
        long an = numerator;
        long ad = denominator;
        long bn = n;
        long bd = 1L;
        return new Fraction(an * bd, ad * bn);
    }

    @Override
    public int compareTo(Fraction b) {
        long an = numerator;
        long ad = denominator;
        long bn = b.numerator;
        long bd = b.denominator;
        long l = an * bd;
        long r = bn * ad;
        return l == r ? 0 : l < r ? -1 : 1;
    }

    public int compareTo(long n) {
        long an = numerator;
        long ad = denominator;
        long bn = n;
        long l = an;
        long r = bn * ad;
        return l == r ? 0 : l < r ? -1 : 1;
    }

    @Override
    public boolean equals(Object other) {
        if ((other instanceof Fraction)) {
            return compareTo((Fraction) other) == 0;
        }
        return false;
    }

    public boolean equals(long n) {
        return compareTo(n) == 0;
    }

    @Override
    public int hashCode() {
        return (int) (numerator ^ denominator);
    }
}
