// modified from: http://sourceforge.net/p/javaocr/source/ci/master/tree/core/src/main/java/net/sourceforge/javaocr/

/*
 * Copyright (c) 2003-2012, Ronald B. Cemer , Konstantin Pribluda, William Whitney, Andrea De Pasquale
 *
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sourceforge.bookscanwizard.filters;

/**
 * abstract base class encapsulating image functionality  for images represented by linear arrays.
 * This class is stateful in regard to filtering, and thus not thread safe. Those optimisations aim
 * to reduce method invocations to be performant on davlik (android) where direct field access if more
 * performant than going through getter
 *
 * @author Konstantin Pribluda
 */
public abstract class AbstractLinearImage implements Image {
    /**
     * Width of the image, in pixels.
     */
    protected final int arrayWidth;
    /**
     * Height of the image, in pixels.
     */
    protected final int arrayHeight;
    /**
     * X-origin inside array
     */
    protected final int originX;
    /**
     * Y-origin inside array
     */
    protected final int originY;
    /**
     * actual image width
     */
    protected final int width;
    /**
     * actual image height
     */
    protected final int height;
    /**
     * actual position being processed
     */
    protected int currentIndex;

    /**
     * Aspect ratio of the image (<code>width/height</code>).
     */
    protected final float aspectRatio;


    // iteration  step
    int step;
    // iteration border
    int border;

    /**
     * construct image over while linear array with specified arrayWidth and arrayHeight
     *
     * @param arrayWidth  image arrayWidth
     * @param arrayHeight image arrayHeight
     */
    protected AbstractLinearImage(int arrayWidth, int arrayHeight) {
        this(arrayWidth, arrayHeight, 0, 0, arrayWidth, arrayHeight);
    }

    /**
     * construct image over subset of image in linear array
     *
     * @param arrayWidth  full image arrayWidth
     * @param arrayHeight full image arrayHeight
     * @param originX     X-origin of subimage
     * @param originY     Y-origin of subimage
     * @param width       subimage arrayWidth
     * @param height      subimage arrayHeight
     */
    protected AbstractLinearImage(int arrayWidth, int arrayHeight, int originX, int originY, int width, int height) {
        this.height = height;
        this.width = width;
        this.arrayHeight = arrayHeight;
        this.originX = originX;
        this.originY = originY;
        this.arrayWidth = arrayWidth;
        aspectRatio = ((float) width) / ((float) height);
    }


    /**
     * retrieve current  pixel empty
     *
     * @return current pixel empty
     */
    @Override
    abstract public int get();

    /**
     * save pixel to current position
     *
     * @param value
     */
    @Override
    abstract public void put(int value);


    /**
     * Get the empty of a pixel at a specific <code>x,y</code> position.
     *
     * @param x The pixel's x position.
     * @param y The pixel's y position.
     * @return The empty of the pixel.
     */
    @Override
    public int get(int x, int y) {
        setCurrentIndex(x, y);
        return get();
    }


    protected void setCurrentIndex(int x, int y) {
        currentIndex = ((y + originY) * arrayWidth) + x + originX;
    }

    /**
     * store pixel empty. manipulates current image pointer (
     * do not call it while iterating, use put(pixel) instead)
     *
     * @param x
     * @param y
     * @param value
     */
    @Override
    public void put(int x, int y, int value) {
        setCurrentIndex(x, y);
        put(value);
    }


    public int getArrayHeight() {
        return arrayHeight;
    }

    public int getArrayWidth() {
        return arrayWidth;
    }

    @Override
    public float getAspectRatio() {
        return aspectRatio;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getOriginX() {
        return originX;
    }

    @Override
    public int getOriginY() {
        return originY;
    }

    /**
     * whether given span equals to specific empty
     *
     * @param y     Y empty
     * @param from  inclusive from
     * @param to    inclusive to
     * @param value empty to be mathced
     * @return
     */
    @Override
    public boolean horizontalSpanEquals(final int y, final int from, final int to, final int value) {
        iterateH(y, from, to);
        while (hasNext()) {
            if (next() != value) return false;
        }
        return true;

    }

    /**
     * @param x     X Value
     * @param from  inclusive from
     * @param to    inclusive to
     * @param value valye to be matched
     * @return
     */
    @Override
    public boolean verticalSpanEquals(final int x, final int from, final int to, final int value) {
        iterateV(x, from, to);
        while (hasNext()) {
            if (next() != value) return false;
        }
        return true;
    }


    @Override
    public void iterateV(int x, int from, int to) {
        currentIndex = (from + originY - 1) * arrayWidth + x + originX;
        border = (to + originY) * arrayWidth + x + originX;
        step = arrayWidth;
    }

    @Override
    public void iterateH(int y, int from, int to) {
        final int base = (y + originY) * arrayWidth + originX - 1;
        currentIndex = base + from;
        border = base + to + 1;
        step = 1;
    }

    @Override
    public void iterateH(int y) {
        iterateH(y, 0, width - 1);
    }

    @Override
    public void iterateV(int x) {
        iterateV(x, 0, height - 1);
    }


    @Override
    public boolean hasNext() {
        //System.err.println("current: " + currentIndex + " border:" + border);
        return currentIndex < border;
    }

    @Override
    public int next() {
        currentIndex += step;
        return get();
    }

    /**
     * advance to next and store pixel at current position
     *
     * @param pixel
     */
    @Override
    public void next(int pixel) {
        currentIndex += step;
        put(pixel);

    }

    /**
     * copy image content to destination.  we hope that dimensions match
     *
     * @param dst
     */
    @Override
    public void copy(Image dst) {
        final int imgHeight = getHeight();
        for (int i = 0; i < imgHeight; i++) {
            for (iterateH(i), dst.iterateH(i); hasNext();) {
                dst.next(next());
            }
        }
    }

    /**
     * copy image content to destination with coordinate flip.  we hope that dimensions match
     *
     * @param dst
     */
    @Override
    public void flip(Image dst) {
        final int imgWidth = getWidth();
        for (int i = 0; i < imgWidth; i++) {
            for (iterateV(i), dst.iterateH(i); hasNext();) {
                dst.next(next());
            }
        }
    }

    @Override
    public String toString() {
        return "AbstractLinearImage{" +
                "arrayHeight=" + arrayHeight +
                ", arrayWidth=" + arrayWidth +
                ", aspectRatio=" + aspectRatio +
                ", height=" + height +
                ", originX=" + originX +
                ", originY=" + originY +
                ", width=" + width +
                '}';
    }


    @Override
    public Image row(int y) {
        return chisel(0, y, width, 1);
    }

    @Override
    public Image column(int x) {
        return chisel(x, 0, 1, height);
    }
}
