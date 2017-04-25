package nl.debijenkorf.tools.photoresizer.resizer;

import java.awt.*;
import java.awt.image.BufferedImage;
import nl.debijenkorf.tools.photoresizer.resizer.Preset.VAlign;


/**
 * Created by Daniel on 14/07/15.
 */
public interface ImageAligner {
    /**
     * This method trims (detects the first edge of the image from top, bottom, left and right)
     * and apply padding according to the specified parameter
     *
     * @param image image to process
     * @param canvasAspectRatio ratio of width to height of the target canvas
     * @param bottomPercentage how many percent bottom fill must be to the total height of the image
     * @param minTopPercentage how many percent top fill is at the least to the total height of the image
     * @param minLeftRightPercentage how many percent combined left + right fill is to the total width of the image
     * @param fillColor Color that is used to fill
     * @param vAlign
     * @return
     */
    BufferedImage align(BufferedImage image, double canvasAspectRatio, int bottomPercentage, int minTopPercentage,
                        int minLeftRightPercentage, Color fillColor, VAlign vAlign);
}
