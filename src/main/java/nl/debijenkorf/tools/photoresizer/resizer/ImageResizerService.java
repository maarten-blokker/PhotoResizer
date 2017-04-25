package nl.debijenkorf.tools.photoresizer.resizer;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for image resizer service. It contains list of image definitions and their properties.
 *
 * Created by Daniel on 18/04/15.
 */
public interface ImageResizerService {

    void process (Preset preset, Color color, boolean align, InputStream image, OutputStream os) throws IOException;

    enum ImageProfile {

        /**
         * Documented in https://bijenkorf.atlassian.net/wiki/display/BSL/Image+service
         */

        ish_lister (252, 348, 90, ScalingStrategy.FILL, Color.WHITE, true, 15, 15, 9, VAlign.BOTTOM),
        ish_lister_2x (506, 696, 90, ScalingStrategy.FILL, Color.WHITE, true, 15, 15, 9, VAlign.BOTTOM),
        ish_detail (475, 654, 90, ScalingStrategy.FILL, Color.WHITE, true, 15, 15, 9, VAlign.BOTTOM),
        ish_detail_2x (950, 1308, 90, ScalingStrategy.FILL, Color.WHITE, true, 15, 15, 9, VAlign.BOTTOM),
        ish_select (72, 98, 90, ScalingStrategy.FILL, Color.WHITE, true, 15, 15, 9, VAlign.BOTTOM),
        ish_select_2x (144, 196, 90, ScalingStrategy.FILL, Color.WHITE, true, 15, 15, 9, VAlign.BOTTOM),

        web_select (72, 98, 90, ScalingStrategy.FILL, Color.WHITE, true, 5, 5, 9, VAlign.MIDDLE),
        web_select_2x (144, 196, 90, ScalingStrategy.FILL, Color.WHITE, true, 5, 5, 9, VAlign.MIDDLE),

        web_detail (768, 1060, 90, ScalingStrategy.FILL, Color.WHITE, true, 5, 5, 9, VAlign.MIDDLE),
        web_detail_2x (1108, 1528, 90, ScalingStrategy.FILL, Color.WHITE, true, 5, 5, 9, VAlign.MIDDLE),
        web_detail_lq (768, 1060, 0, ScalingStrategy.FILL, Color.WHITE, true, 5, 5, 9, VAlign.MIDDLE),

        web_lister (258, 357, 90, ScalingStrategy.FILL, Color.WHITE, true, 15, 15, 9, VAlign.BOTTOM),
        web_lister_2x (516, 714, 90, ScalingStrategy.FILL, Color.WHITE, true, 15, 15, 9, VAlign.BOTTOM),

        app_lister_2x (288, 397, 90, ScalingStrategy.FILL, Color.WHITE, true, 5, 5, 9, VAlign.MIDDLE),
        app_lister_3x (432, 596, 90, ScalingStrategy.FILL, Color.WHITE, true, 5, 5, 9, VAlign.MIDDLE),

        original (0, 0, 100, null, null, false, null, null, null, null);

        public final int height;
        public final int width;
        public final int quality;
        public final ScalingStrategy scalingStrategy;
        public final Color fill;
        public final boolean subSampling;
        public final Integer minTopMarginPercent;
        public final Integer minBottomMarginPercent;
        public final Integer minLeftRightPercent;
        public final VAlign vAlign;

        ImageProfile (int width, int height, int quality, ScalingStrategy scalingStrategy, Color fill, boolean subSampling,
                Integer minTopMarginPercent, Integer minBottomMarginPercent, Integer minLeftRightPercent, VAlign vAlign) {

            this.height = height;
            this.width = width;
            this.quality = quality;
            this.scalingStrategy = scalingStrategy;
            this.fill = fill;
            this.subSampling = subSampling;
            this.minTopMarginPercent = minTopMarginPercent;
            this.minBottomMarginPercent = minBottomMarginPercent;
            this.minLeftRightPercent = minLeftRightPercent;
            this.vAlign = vAlign;
        }
    }

    enum ScalingStrategy {
        CROP,
        FILL,
        SKEW
    }

    enum VAlign {
        BOTTOM, //default
        MIDDLE
    }
}
