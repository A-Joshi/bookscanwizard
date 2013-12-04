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
import java.awt.image.SampleModel;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import net.sourceforge.bookscanwizard.ColorOp;
import net.sourceforge.bookscanwizard.FileHolder;
import net.sourceforge.bookscanwizard.Operation;
import net.sourceforge.bookscanwizard.UserException;
import net.sourceforge.bookscanwizard.gui.UserPreferenceBean;
import net.sourceforge.bookscanwizard.util.ProcessHelper;
import net.sourceforge.bookscanwizard.util.Utils;
import org.apache.commons.io.IOUtils;

/**
 * This will call ScanTailor to process the image further.
 */
public class ScanTailor extends Operation implements ColorOp  {
    private static final Charset UTF8 = Charset.forName("UTF8");
    
    
/*    @Override
    protected RenderedImage previewOperation(FileHolder holder, RenderedImage img) throws Exception {
        // do not run during preview.
        return img;
    }*/

    @Override
    public RenderedImage performOperation(FileHolder holder, RenderedImage img) throws Exception {
        String[] args = getTextArgs();
        String name = "scantailor-cli";
        String dir = UserPreferenceBean.instance().getScanTailorLocation();
        if (dir != null && !dir.isEmpty()) {
            name = new File(dir, name).getPath();
        }
        name = ProcessHelper.findPath(name);
        File parent = new File(name).getParentFile();
        if (!new File(name).isFile()) {
            throw new UserException("The executable \"scantailor-cli\" could not be found.\nEnsure the directory is on the system path or the location is entered under Tools, Preferences");
        }
        List<String> cmdList = new ArrayList<>();
        cmdList.add(name);
        cmdList.addAll(Arrays.asList(getTextArgs()));
        cmdList.add("--layout=1");
//        cmdList.add("--content-box=0.0x0.0:"+img.getWidth()+"x"+img.getHeight());
                cmdList.add("--content-box=0.0x0.0:200x200");
//        cmdList.add("--disable-content-detection");
//        cmdList.add("--margins=0");
//        cmdList.add("--normalize-illumination");
//        cmdList.add("--dpi=300");
//        cmdList.add("--output-dpi=300");
        cmdList.add("--output-project="+new File(pageSet.getDestinationDir(), "st.st").getAbsolutePath());
        String pngName = new File(pageSet.getDestinationDir(), holder.getName() + ".png").getCanonicalPath();
        cmdList.add(pngName);
        cmdList.add(pageSet.getDestinationDir().getCanonicalPath());
        System.out.println(cmdList);
        
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(pngName))) {
            ImageIO.write(img, "PNG", os);
        }
        ProcessBuilder processBuilder = new ProcessBuilder(cmdList)
            .directory(pageSet.getDestinationDir())
            .redirectErrorStream(true);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Process proc = processBuilder.start();
            IOUtils.copy(proc.getInputStream(), bos);
            int retVal = proc.waitFor();
            new File(pngName).delete();
            if (retVal != 0) {
                String msg = new String(bos.toByteArray(), UTF8);
                throw new UserException(msg);
            }
        }
        File tifName = new File(pageSet.getDestinationDir(), holder.getName() + ".tif");
        img = Utils.renderedToBuffered(JAI.create("fileload", tifName.getPath()));
        SampleModel sm = img.getSampleModel();
        
        tifName.delete();
        img = ImageIO.read(tifName);
        return img;
    }
}
