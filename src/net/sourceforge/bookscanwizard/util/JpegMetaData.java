package net.sourceforge.bookscanwizard.util;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.sourceforge.bookscanwizard.UserException;
import org.w3c.dom.NodeList;
/*
 * Based on code from: http://192.9.162.102/thread.jspa?threadID=542593
 */

public class JpegMetaData {
    private static final String JPEGMetaFormat = "javax_imageio_jpeg_image_1.0";
    private IIOMetadata metaData;

    public JpegMetaData(File file) throws IOException {
        byte[] exifRAW;
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            ImageReader reader = null;
            while (readers.hasNext()) {
                ImageReader tmp = readers.next();
                if (JPEGMetaFormat.equals(tmp.getOriginatingProvider().getNativeImageMetadataFormatName())) {
                    reader = tmp;
                    break;
                }
            }
            if (reader == null) {
                throw new UserException("The selected jpeg file did not contain any metadata.");
                
            }
            reader.setInput(in, true, false);
            exifRAW = getEXIF(reader.getImageMetadata(0));
            reader.dispose();
        }

        if (exifRAW == null) {
            throw new UserException("The selected jpeg file did not contain any exif data.");
        }

        metaData = getTiffMetaFromEXIF(exifRAW);
    }

    public String getValue(String name) throws IOException {
        return getMetaDataValue(metaData, name);
    }

    private static String getMetaDataValue(IIOMetadata exifMeta, String name) {
        IIOMetadataNode root = (IIOMetadataNode) exifMeta.getAsTree("com_sun_media_imageio_plugins_tiff_image_1.0");

        NodeList imageDirectories = root.getElementsByTagName("TIFFIFD");
        for (int i = 0; i < imageDirectories.getLength(); i++) {
            IIOMetadataNode directory = (IIOMetadataNode) imageDirectories.item(i);

            NodeList tiffTags = directory.getElementsByTagName("TIFFField");
            for (int j = 0; j < tiffTags.getLength(); j++) {
                IIOMetadataNode tag = (IIOMetadataNode) tiffTags.item(j);

                String tagName = tag.getAttribute("name");
                if (tagName.equals(name)) {
                    String tagValue;


                    StringBuilder tmp = new StringBuilder();
                    IIOMetadataNode values = (IIOMetadataNode) tag.getFirstChild();

                    if ("TIFFUndefined".equals(values.getNodeName())) {
                        tmp.append(values.getAttribute("value"));
                    } else {
                        NodeList tiffNumbers = values.getChildNodes();
                        for (int k = 0; k < tiffNumbers.getLength(); k++) {
                            tmp.append(((IIOMetadataNode) tiffNumbers.item(k)).getAttribute("value"));
                            tmp.append(",");
                        }
                        tmp.deleteCharAt(tmp.length() - 1);
                    }

                    tagValue = tmp.toString();
                    return tagValue;
                }
            }
        }
        return null;
    }

    /**Returns the EXIF information from the given metadata if present.  The
     * metadata is assumed to be in <pre>javax_imageio_jpeg_image_1.0</pre> format.
     * If EXIF information was not present then null is returned.*/
    public static byte[] getEXIF(IIOMetadata meta) {
        //http://java.sun.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html

        //javax_imageio_jpeg_image_1.0
        //-->markerSequence
        //---->unknown (attribute: "MarkerTag" val: 225 (for exif))

        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(JPEGMetaFormat);

        IIOMetadataNode markerSeq = (IIOMetadataNode) root.getElementsByTagName("markerSequence").item(0);

        NodeList unkowns = markerSeq.getElementsByTagName("unknown");
        for (int i = 0; i < unkowns.getLength(); i++) {
            IIOMetadataNode marker = (IIOMetadataNode) unkowns.item(i);
            if ("225".equals(marker.getAttribute("MarkerTag"))) {
                return (byte[]) marker.getUserObject();
            }
        }
        return null;
    }

    /**Uses a TIFFImageReader plugin to parse the given exif data into tiff
     * tags.  The returned IIOMetadata is in whatever format the tiff ImageIO
     * plugin uses.  If there is no tiff plugin, then this method returns null.*/
    public static IIOMetadata getTiffMetaFromEXIF(byte[] exif) {
        java.util.Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("tif");

        ImageReader reader;
        if (!readers.hasNext()) {
            return null;
        } else {
            reader = readers.next();
        }

        //skip the 6 byte exif header
        ImageInputStream wrapper = new MemoryCacheImageInputStream(
                new java.io.ByteArrayInputStream(exif, 6, exif.length - 6));
        reader.setInput(wrapper, true, false);

        IIOMetadata exifMeta;
        try {
            exifMeta = reader.getImageMetadata(0);
        } catch (Exception e) {
            //shouldn't happen
            throw new Error(e);
        }

        reader.dispose();
        return exifMeta;
    }
}
