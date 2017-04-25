package nl.debijenkorf.tools.photoresizer.resizer.impl;

import org.imgscalr.Scalr;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import nl.debijenkorf.tools.photoresizer.Configuration;
import nl.debijenkorf.tools.photoresizer.resizer.ImageAligner;
import nl.debijenkorf.tools.photoresizer.resizer.ImageResizerService;
import nl.debijenkorf.tools.photoresizer.resizer.Preset;

/**
 * Image resizer service using ImgScalr library
 *
 * Created by Daniel on 18/04/15.
 */
public class ImgScalrResizer implements ImageResizerService {

    private final ImageAligner aligner;

    public ImgScalrResizer(ImageAligner filler) {
        this.aligner = filler;
    }

    @Override
    public void process(Preset preset, Color color, boolean align, InputStream input, OutputStream output) throws IOException {
        int targetWidth = Configuration.TARGET_WIDTH;
        int targetHeight = Configuration.TARGET_HEIGHT;
        double targetAspectRatio = (double) targetWidth / targetHeight;

        BufferedImage bufferedImage = ImageIO.read(input);
        if (bufferedImage == null) {
            throw new IOException("Image format is not supported");
        }
        if (align) {
            bufferedImage = align(preset, color, bufferedImage, targetAspectRatio);
        }

        // resize
        BufferedImage scaledImage = Scalr.resize(bufferedImage, Scalr.Method.ULTRA_QUALITY, targetWidth, targetHeight);
        if (scaledImage.getWidth() > targetWidth || scaledImage.getHeight() > targetHeight) {
            scaledImage = Scalr.crop(scaledImage, targetWidth, targetHeight);
        }

        // recompress
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        writer.setOutput(ImageIO.createImageOutputStream(output));

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(1F);
        param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);

        IIOMetadata metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(scaledImage), param);

        writer.write(null, new IIOImage(scaledImage, null, metadata), param);
    }

    private BufferedImage align(Preset preset, Color color, BufferedImage bufferedImage, double targetAspectRatio) {
        BufferedImage alignedImage = bufferedImage;
        // align image if profile requires it

        int topMarginPercent = (int) preset.getTopLine();
        int bottomMarginPercent = (int) (100 - preset.getBaseLine());
        int leftRightMargin = (int) preset.getLeftRightMargin();
        alignedImage = aligner.align(
                bufferedImage, targetAspectRatio,
                bottomMarginPercent, topMarginPercent, leftRightMargin,
                Configuration.isDebug() ? Color.LIGHT_GRAY : color, preset.getValign());

        return alignedImage;
    }

}
