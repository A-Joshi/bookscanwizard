package net.sourceforge.bookscanwizard.op;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.BSW;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;

/**
 * This adjusts the brightness of an image so that the brightness is even
 * across the image.  This should be run on a blank page, or a grey or white card.
 */
public class NormalizeLighting extends Operation implements ColorOp {
    private static final Logger logger =Logger.getLogger(NormalizeLighting.class.getName());
    private RenderedImage mapImage;
    private float scale = 1F/16F;

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws RuntimeException, IOException {
        if (mapImage == null) {
            File f = BSW.getFileFromCurrentDir(getTextArgs()[0] + ".dat");
            try {
                mapImage = ImageIO.read(f);
            } catch (IIOException e) {
                throw new UserException("Could not find NormalizedLighting image "+f.getName());
            }
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(mapImage);             
            pb.add((float) img.getMinX() * scale);
            pb.add((float) img.getMinY() * scale);
            mapImage = JAI.create("translate", pb);
            logger.log(Level.FINE, "{0}", new Rectangle(mapImage.getMinX(), mapImage.getMinY(), mapImage.getWidth(), mapImage.getHeight()));
            Rectangle bounds = new Rectangle(img.getMinX(), img.getMinY(), img.getWidth(), img.getHeight());
            mapImage = getFullSizeImage(mapImage, bounds);
        }
        double gray;

        if (getTextArgs().length > 1) {
            gray = Double.parseDouble(getTextArgs()[1]) * 255D / 100D;
        } else {
            gray = ImageStatistics.GRAY_STANDARD;
        }
        return normalizeImage(mapImage, img, gray);
    }

    public String getConfig(FileHolder holder, RenderedImage img) throws IOException {
        mapImage =JAI.create("SubsampleAverage",img, (double) scale, (double) scale, BSW.QUALITY_HINTS);
        ParameterBlock params = new ParameterBlock();
        params.addSource(img);
        String name ="nl_"+holder.getName();
        File f= BSW.getFileFromCurrentDir(name+".dat");
        ImageIO.write(mapImage, "png", f);
        mapImage = null;
        return "NormalizeLighting = nl_"+holder.getName();
    }

    public RenderedImage getFullSizeImage(RenderedImage img, Rectangle bounds) {
        ParameterBlock params = new ParameterBlock();
        params.addSource(img);

        params.add(1).add(2).add(1).add(2);
        params.add(BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        params.add(0);
        img = JAI.create("border", params);
        ParameterBlock pb = new ParameterBlock()
            .addSource(img).add(1/scale).add(1/scale)
            .add(0F).add(0F).
            add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
        img = JAI.create("scale", pb, BSW.QUALITY_HINTS);
        pb = new ParameterBlock();
            pb.addSource(img);
            pb.add((float) bounds.getMinX());
            pb.add((float) bounds.getMinY());
            pb.add((float) bounds.getWidth());
            pb.add((float) bounds.getHeight());
        img =  JAI.create("crop", pb);
        return img;
    }

    /**
     * Takes a graycard image, and another image, and use the graycard settings to adjust for
     * lighting differences across the page.
     */
    public static RenderedImage normalizeImage(RenderedImage mapImage, RenderedImage img, double gray) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        // need to temporarily change the image to short, so that we don't clip
        // te result
        pb.add(DataBuffer.TYPE_SHORT);
        img = JAI.create("format", pb);

        pb = new ParameterBlock();
        pb.addSource(img);
        pb.add(new double[]{gray, gray, gray});
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
}
