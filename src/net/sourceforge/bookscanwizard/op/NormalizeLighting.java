package net.sourceforge.bookscanwizard.op;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.media.jai.BorderExtender;
import javax.media.jai.Histogram;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.util.Utils;

/**
 * This adjusts the brightness of an image so that the brightness is even
 * across the image.  This should be run on a blank page, or a grey or white card.
 */
public class NormalizeLighting extends Operation implements ColorOp {
    private static final Logger logger =Logger.getLogger(NormalizeLighting.class.getName());
    private volatile RenderedImage leftImage;
    private volatile RenderedImage rightImage;
    
    private String leftPageName;
    private String rightPageName;
    private double[] median;

    static ThreadLocal<Boolean> processing = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };
    
    @Override
    protected List<Operation> setup(List<Operation> operationList) throws Exception {
        leftPageName = getTextArgs()[0];
        if (getTextArgs().length > 1) {
            rightPageName = getTextArgs()[1];
        }
        leftImage = null;
        rightImage = null;
        return operationList;
    }

    @Override
    protected void preprocess(FileHolder holder, RenderedImage img, boolean preview) throws Exception {
        System.out.println("holder: "+holder+" "+processing.get()+" "+(leftImage == null));
        if (leftImage == null && !processing.get()) {
            for (FileHolder fh : getPageSet().getFileHolders()) {
                if (fh.getName().equals(leftPageName)) {
                    fh.setForceOn(true);
                    processing.set(true);
                    leftImage = Operation.previewOperations(currentPreviewOps, fh, fh.getImage(), true);
                    Histogram histogram =
                        (Histogram)JAI.create("histogram", leftImage).getProperty("histogram");
                    addMedian(histogram.getPTileThreshold(0.5));
                    fh.setForceOn(false);
                    processing.set(false);
                }
                if (fh.getName().equals(rightPageName)) {
                    fh.setForceOn(true);
                    processing.set(true);
                    rightImage = Operation.previewOperations(currentPreviewOps, fh, fh.getImage(), true);
                    Histogram histogram =
                        (Histogram)JAI.create("histogram", rightImage).getProperty("histogram");
                    addMedian(histogram.getPTileThreshold(0.5));
                    fh.setForceOn(false);
                    processing.set(false);
                }
            }
            if (rightImage == null) {
                rightImage = leftImage;
            }
        }
    }

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        if (processing.get()) {
            return processReferenceImage(img);
        } else {
            RenderedImage normalizeImage = holder.getPosition() == FileHolder.LEFT ? leftImage : rightImage;
            if (normalizeImage == null) {
                System.out.println("mapImage is null");
            }
            if (img == null) {
                System.out.println("img is null");
            }
            return normalizeImage(normalizeImage, img);
        }
    }

    /**
     * This takes the blank pages, scales it down, blurs it, then scales it back up
     * This 
     * @param source
     * @return
     * @throws IOException 
     */
    public RenderedImage processReferenceImage(RenderedImage source) throws IOException {
        final float scale = 10;
        int newWidth = source.getWidth() / (int) scale;
        int newHeight = source.getHeight() / (int) scale;
        RenderedImage img = Utils.getScaledInstance(source, newWidth, newHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        img = GaussianBlur.blur(img, 2);
        
        ParameterBlock params = new ParameterBlock();
        params.addSource(img);
        params.add(1).add(2).add(1).add(2);
        params.add(BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        params.add(0);
        img = JAI.create("border", params);
        ParameterBlock pb = new ParameterBlock()
            .addSource(img).add(scale).add(scale)
            .add(0F).add(0F).
            add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
        img = JAI.create("scale", pb, BSW.QUALITY_HINTS);
        pb = new ParameterBlock();
        pb.addSource(img);
        pb.add((float) source.getMinX());
        pb.add((float) source.getMinY());
        pb.add((float) source.getWidth());
        pb.add((float) source.getHeight());
        img =  JAI.create("crop", pb);
        return img;
    }

    /**
     * Takes a graycard image, and another image, and use the graycard settings to adjust for
     * lighting differences across the page.
     */
    private RenderedImage normalizeImage(RenderedImage mapImage, RenderedImage img) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        // need to temporarily change the image to short, so that we don't clip
        // te result
        pb.add(DataBuffer.TYPE_SHORT);
        img = JAI.create("format", pb);
        
        pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(median);//new double[]{gray, gray, gray});
        img = JAI.create("addconst", pb);

        pb = new ParameterBlock();
        pb.addSource(img);
        pb.addSource(mapImage);
        img = JAI.create("subtract", pb);

        // change it back to bytes
        pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(DataBuffer.TYPE_BYTE);
        img = JAI.create("format", pb);
        return img;
    }

    private void addMedian(double[] pTileThreshold) {
        if (median == null) {
            median = pTileThreshold;
        } else {
            for (int i=0; i < median.length; i++) {
                median[i] = (median[i] + pTileThreshold[i]) / 2.0;
            }
        }
    }
}
