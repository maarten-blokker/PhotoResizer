package nl.debijenkorf.tools.photoresizer.resizer.impl;

import java.awt.*;
import java.awt.image.*;
import nl.debijenkorf.tools.photoresizer.Configuration;
import nl.debijenkorf.tools.photoresizer.resizer.ImageAligner;
import nl.debijenkorf.tools.photoresizer.resizer.Preset.VAlign;

/**
 * Created by Daniel on 14/07/15.
 */
public class SmartImageAligner implements ImageAligner {

    @Override
    public BufferedImage align(BufferedImage image,
            double canvasAspectRatio,
            int bottomPercentage,
            int minTopPercentage,
            int minLeftRightPercentage,
            Color fillColor,
            VAlign vAlign) {

        ImageEdges imageEdges = findImageEdges(image, 300);

        // Do not align the image if either top, bottom, left or right of the non-white image
        // touch the edge of the canvas
        if (imageEdges.getLeftCoordinate() == 0 || imageEdges.getTopCoordinate() == 0
                || imageEdges.getRightCoordinate() == image.getWidth() - 1
                || imageEdges.getBottomCoordinate() == image.getHeight() - 1) {
            return image;
        }

        // calculate how much we need to pad around "trimmed" image
        Margin margin = calculateMargin(
                canvasAspectRatio,
                imageEdges.getRightCoordinate() - imageEdges.getLeftCoordinate(),
                imageEdges.getBottomCoordinate() - imageEdges.getTopCoordinate(),
                bottomPercentage,
                minTopPercentage,
                minLeftRightPercentage,
                vAlign);

        // calculate how much we need to pad or cut relative to original image
        Margin relativeMargin = calculateRelativeMargin(imageEdges, margin,
                image.getWidth(), image.getHeight());

        // cut or pad image depending on relativeMargin calculated value
        BufferedImage alignedImage = trimAndPad(image, relativeMargin, fillColor);

        return alignedImage;
    }

    protected Margin calculateRelativeMargin(ImageEdges imageEdges, Margin margin,
            int originalWidth, int originalHeight) {
        Margin relativeMargin = new Margin();
        relativeMargin.setTopMargin(margin.getTopMargin() - imageEdges.getTopCoordinate());
        relativeMargin.setLeftMargin(margin.getLeftMargin() - imageEdges.getLeftCoordinate());
        relativeMargin.setRightMargin(margin.getRightMargin() - (originalWidth - (imageEdges.getRightCoordinate() + 1)));
        relativeMargin.setBottomMargin(margin.getBottomMargin() - (originalHeight - (imageEdges.getBottomCoordinate() + 1)));
        return relativeMargin;
    }

    protected BufferedImage trimAndPad(BufferedImage image, Margin relativeMargin, Color fillColor) {
        BufferedImage trimmedImage = trim(image, relativeMargin.getTopMargin() * -1,
                relativeMargin.getBottomMargin() * -1, relativeMargin.getLeftMargin() * -1,
                relativeMargin.getRightMargin() * -1);

        BufferedImage paddedImage = pad(trimmedImage, relativeMargin.getTopMargin(),
                relativeMargin.getBottomMargin(), relativeMargin.getLeftMargin(),
                relativeMargin.getRightMargin(), fillColor);

        return paddedImage;

    }

    protected ImageEdges findImageEdges(BufferedImage image, int tolerance) {
        int leftMostPixel = 0,
                topMostPixel = 0,
                rightMostPixel = image.getWidth() - 1,
                bottomMostPixel = image.getHeight() - 1;

        leftToRightSweep:
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                double distance = colorDistance(new Color(argb, true), Color.WHITE);
                if (distance > tolerance) {
                    leftMostPixel = x;
                    break leftToRightSweep;
                }
            }
        }

        rightToLeftSweep:
        for (int x = image.getWidth() - 1; x >= leftMostPixel; x--) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                double distance = colorDistance(new Color(argb, true), Color.WHITE);
                if (distance > tolerance) {
                    rightMostPixel = x;
                    break rightToLeftSweep;
                }
            }
        }

        topToBottomSweep:
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                double distance = colorDistance(new Color(argb, true), Color.WHITE);
                if (distance > tolerance) {
                    topMostPixel = y;
                    break topToBottomSweep;
                }
            }
        }

        int bottomMostPixelFallback = image.getHeight() - 1;
        bottomToTopSweep:
        for (int y = image.getHeight() - 1; y >= topMostPixel; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                Color color = new Color(argb, true);
                Color topPxClr = y == 0 ? Color.WHITE : new Color(image.getRGB(x, y - 1));
                Color top2PxClr = y < 2 ? Color.WHITE : new Color(image.getRGB(x, y - 2));
                Color bottomPxClr = y == image.getHeight() - 1 ? Color.WHITE : new Color(image.getRGB(x, y + 1));
                double distanceToWhite = colorDistance(color, Color.WHITE);
                double distanceToTop = (distanceToWhite + colorDistance(bottomPxClr, Color.WHITE)) / 2
                        - (colorDistance(topPxClr, Color.WHITE) + colorDistance(top2PxClr, Color.WHITE)) / 2;
                if (distanceToTop < 0 && Math.abs(distanceToTop) > tolerance * 3) {
                    bottomMostPixel = y - 1;
                    break bottomToTopSweep;
                }

                if (bottomMostPixelFallback == image.getHeight() - 1) {
                    if (distanceToWhite > tolerance) {
                        bottomMostPixelFallback = y;
                    }
                }
            }
        }
        if (bottomMostPixel == image.getHeight() - 1) {
            bottomMostPixel = bottomMostPixelFallback;
        }

        // mark bottom within tolerance
        int coloredCount = 0;
        for (int x1 = 0; x1 < image.getWidth(); x1++) {
            int argb = image.getRGB(x1, bottomMostPixel);
            Color color = new Color(argb, true);
            int distance = colorDistance(color, Color.WHITE);
            if (distance > tolerance) {
                if (Configuration.isDebug()) {
                    image.setRGB(x1, bottomMostPixel, Color.BLUE.getRGB());
                }
                coloredCount++;
            }
        }

        // marking for debuggin
        if (Configuration.isDebug()) {
            drawH(image, bottomMostPixel, Color.ORANGE);
            drawH(image, topMostPixel, Color.PINK);
            drawV(image, leftMostPixel, Color.GREEN);
            drawV(image, rightMostPixel, Color.BLUE);
        }

        ImageEdges edges = new ImageEdges();
        edges.setBottomCoordinate(bottomMostPixel);
        edges.setLeftCoordinate(leftMostPixel);
        edges.setRightCoordinate(rightMostPixel);
        edges.setTopCoordinate(topMostPixel);

        return edges;
    }

    protected void findMostProbableReflectionCenter(BufferedImage image, int tolerance, int[] mostProbableReflectionCenter) {
        // build index array from bottom of image to top
        int[][] leftEdges = new int[image.getHeight() / 2][1];
        int[][] rightEdges = new int[image.getHeight() / 2][1];

        for (int y = image.getHeight() / 2; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                double distance = colorDistance(new Color(argb, true), Color.WHITE);
                if (distance > tolerance) {
                    if (Configuration.isDebug()) {
                        image.setRGB(x, y, Color.RED.getRGB());
                    }
                    leftEdges[image.getHeight() - 1 - y][0] = x;
                    break;
                }
            }

            for (int x = image.getWidth() - 1; x >= 0; x--) {
                int argb = image.getRGB(x, y);
                double distance = colorDistance(new Color(argb, true), Color.WHITE);
                if (distance > tolerance) {
                    if (Configuration.isDebug()) {
                        image.setRGB(x, y, Color.RED.getRGB());
                    }
                    rightEdges[image.getHeight() - 1 - y][0] = x;
                    break;
                }
            }
        }

        // sweep-fold to calculate matching probability
        int[] leftRightProbability = new int[leftEdges.length - 1];
        for (int foldPoint = 1; foldPoint < leftEdges.length; foldPoint++) {
            int index = foldPoint - 1;
            leftRightProbability[index] = 0;

            int countLeft = 0;
            int countRight = 0;
            int sumDeltaLeft = 0;
            int sumDeltaRight = 0;
            int prefYL1Val = -1;
            int prefYR1Val = -1;

            for (int y1 = foldPoint - 1, y2 = foldPoint; y1 >= 0 && y2 < leftEdges.length - 1; y1--, y2++) {
                int yL1Val = leftEdges[y1][0];
                if (yL1Val == 0) {
                    break;
                }
                int yL2Val = leftEdges[y2][0];
                if (yL2Val == 0) {
                    break;
                }

                int yR1Val = rightEdges[y1][0];
                if (yR1Val == 0) {
                    break;
                }
                int yR2Val = rightEdges[y2][0];
                if (yR2Val == 0) {
                    break;
                }

                int leftDiff = Math.abs(yL2Val - yL1Val);
                if (leftDiff < image.getWidth() / 200) {
                    if (prefYL1Val != -1) {
                        sumDeltaLeft += Math.abs(prefYL1Val - yL1Val);
                    }
                    prefYL1Val = yL1Val;
                    countLeft += 4;
                } else if (leftDiff < image.getWidth() / 100) {
                    if (prefYL1Val != -1) {
                        sumDeltaLeft += Math.abs(prefYL1Val - yL1Val);
                    }
                    prefYL1Val = yL1Val;
                    countLeft += 2;
                } else if (leftDiff < image.getWidth() / 50) {
                    if (prefYL1Val != -1) {
                        sumDeltaLeft += Math.abs(prefYL1Val - yL1Val);
                    }
                    prefYL1Val = yL1Val;
                    countLeft++;
                }

                int rightDiff = Math.abs(yR2Val - yR1Val);
                if (rightDiff < image.getWidth() / 200) {
                    if (prefYR1Val != -1) {
                        sumDeltaRight += Math.abs(prefYR1Val - yR1Val);
                    }
                    prefYR1Val = yR1Val;
                    countRight += 4;
                } else if (rightDiff < image.getWidth() / 100) {
                    if (prefYR1Val != -1) {
                        sumDeltaRight += Math.abs(prefYR1Val - yR1Val);
                    }
                    prefYR1Val = yR1Val;
                    countRight += 2;
                } else if (rightDiff < image.getWidth() / 50) {
                    if (prefYR1Val != -1) {
                        sumDeltaRight += Math.abs(prefYR1Val - yR1Val);
                    }
                    prefYR1Val = yR1Val;
                    countRight++;
                }
            }
            int combinedProbability = (countLeft + countRight) * (sumDeltaLeft + sumDeltaRight) / (image.getWidth() / 50);
            if (combinedProbability > mostProbableReflectionCenter[1]) {
                mostProbableReflectionCenter[0] = index;
                mostProbableReflectionCenter[1] = combinedProbability;
            }
            leftRightProbability[index] = combinedProbability;
            if (combinedProbability > 300) {
                combinedProbability *= 1;
            }
        }
    }

    private void drawH(BufferedImage image, int y, Color color) {
        for (int x = 0; x < image.getWidth(); x++) {
            image.setRGB(x, y, color.getRGB() * image.getRGB(x, y));
        }
    }

    private void drawV(BufferedImage image, int x, Color color) {
        for (int y = 0; y < image.getHeight(); y++) {
            image.setRGB(x, y, color.getRGB() * image.getRGB(x, y));
        }
    }

    protected int colorDistance(Color color1, Color color2) {
        return (color1.getRed() - color2.getRed()) * (color1.getRed() - color2.getRed())
                + (color1.getGreen() - color2.getGreen()) * (color1.getGreen() - color2.getGreen())
                + (color1.getBlue() - color2.getBlue()) * (color1.getBlue() - color2.getBlue());
    }

    protected Margin calculateMargin(double canvasAspectRatio, int trimmedImageWidth, int trimmedImageHeight,
            int bottomPercentage, int minTopPercentage, int minLeftRightPercentage,
            VAlign vAlign) {
        Margin margin = new Margin();
        double totalImageWidth = (double) trimmedImageWidth * 100 / (100 - minLeftRightPercentage);
        int topPlusBottomPercent;
        if (VAlign.MIDDLE == vAlign) {
            // For middle vertical alignment mode, top and bottom margin will be equal to the biggest one
            topPlusBottomPercent = 2 * Math.max(bottomPercentage, minTopPercentage);
        } else {
            topPlusBottomPercent = minTopPercentage + bottomPercentage;
        }
        double totalImageHeight = (double) trimmedImageHeight * 100 / (100 - topPlusBottomPercent);

        double imageRatio = totalImageWidth / totalImageHeight;

        if (canvasAspectRatio >= imageRatio) {
            // bottom margin
            int imageHeightPercentage = (100 - (bottomPercentage + minTopPercentage));
            int bottomMargin = (trimmedImageHeight * bottomPercentage) / imageHeightPercentage;
            margin.setBottomMargin(bottomMargin);

            // top margin
            int totalCanvasHeight = (trimmedImageHeight * 100) / imageHeightPercentage;
            int topMargin = totalCanvasHeight - trimmedImageHeight - bottomMargin;
            margin.setTopMargin(topMargin);

            // left margin
            int totalCanvasWidth = (int) (canvasAspectRatio * totalCanvasHeight);
            int leftPlusRightMargin = totalCanvasWidth - trimmedImageWidth;
            int leftMargin = leftPlusRightMargin / 2;
            margin.setLeftMargin(leftMargin);

            // right margin
            int rightMargin = leftPlusRightMargin - leftMargin;
            margin.setRightMargin(rightMargin);

        } else {
            // left margin
            int totalCanvasWidth = trimmedImageWidth * 100 / (100 - minLeftRightPercentage);
            int leftPlusRightMargin = totalCanvasWidth - trimmedImageWidth;
            int leftMargin = leftPlusRightMargin / 2;
            margin.setLeftMargin(leftMargin);

            // right margin
            int rightMargin = leftPlusRightMargin - leftMargin;
            margin.setRightMargin(rightMargin);

            int totalCanvasHeight = (int) (totalCanvasWidth / canvasAspectRatio);

            if (VAlign.MIDDLE == vAlign) {
                // top margin
                int topPlusBottomMargin = totalCanvasHeight - trimmedImageHeight;
                int topMargin = topPlusBottomMargin / 2;
                margin.setTopMargin(topMargin);

                // bottom margin
                int bottomMargin = totalCanvasHeight - trimmedImageHeight - topMargin;
                margin.setBottomMargin(bottomMargin);

            } else {
                // bottom margin
                int bottomMargin = (bottomPercentage * totalCanvasHeight) / 100;
                margin.setBottomMargin(bottomMargin);

                // top margin
                int topMargin = totalCanvasHeight - trimmedImageHeight - bottomMargin;
                margin.setTopMargin(topMargin);
            }
        }
        return margin;
    }

    protected static class ImageEdges {

        private int leftCoordinate;
        private int rightCoordinate;
        private int topCoordinate;
        private int bottomCoordinate;

        public int getLeftCoordinate() {
            return leftCoordinate;
        }

        public void setLeftCoordinate(int leftCoordinate) {
            this.leftCoordinate = leftCoordinate;
        }

        public int getRightCoordinate() {
            return rightCoordinate;
        }

        public void setRightCoordinate(int rightCoordinate) {
            this.rightCoordinate = rightCoordinate;
        }

        public int getTopCoordinate() {
            return topCoordinate;
        }

        public void setTopCoordinate(int topCoordinate) {
            this.topCoordinate = topCoordinate;
        }

        public int getBottomCoordinate() {
            return bottomCoordinate;
        }

        public void setBottomCoordinate(int bottomCoordinate) {
            this.bottomCoordinate = bottomCoordinate;
        }
    }

    protected static class Margin {

        private int bottomMargin;
        private int topMargin;
        private int leftMargin;
        private int rightMargin;

        public int getBottomMargin() {
            return bottomMargin;
        }

        public int getTopMargin() {
            return topMargin;
        }

        public int getLeftMargin() {
            return leftMargin;
        }

        public int getRightMargin() {
            return rightMargin;
        }

        public void setBottomMargin(int bottomMargin) {
            this.bottomMargin = bottomMargin;
        }

        public void setTopMargin(int topMargin) {
            this.topMargin = topMargin;
        }

        public void setLeftMargin(int leftMargin) {
            this.leftMargin = leftMargin;
        }

        public void setRightMargin(int rightMargin) {
            this.rightMargin = rightMargin;
        }
    }

    /**
     * Negative crop value is forgiven (default to 0)
     *
     * @param image The original image to be trimmed
     * @param cropTop The amount of pixel to be cut from the top
     * @param cropBottom The amount of pixel to be cut from the bottom
     * @param cropLeft The amount of pixel to be cut from the left
     * @param cropRight The amount of pixel to be cut from the right
     * @return
     */
    protected BufferedImage trim(BufferedImage image, int cropTop, int cropBottom,
            int cropLeft, int cropRight) {
        int parentX = cropLeft < 0 ? 0 : cropLeft;
        int parentY = cropTop < 0 ? 0 : cropTop;
        int newWidth = cropRight <= 0 ? image.getWidth() - parentX : image.getWidth() - parentX - cropRight;
        int newHeight = cropBottom <= 0 ? image.getHeight() - parentY : image.getHeight() - parentY - cropBottom;

        WritableRaster r = image.getRaster();
        ColorModel cm = image.getColorModel();
        r = r.createWritableChild(
                parentX,
                parentY,
                newWidth,
                newHeight,
                0, 0, null);

        return new BufferedImage(cm, r, cm.isAlphaPremultiplied(), null);
    }

    /**
     * Negative margin value is forgiven (default to 0)
     *
     * @param src The original image to be trimmed
     * @param topPadding Pixel amount to add to the top
     * @param bottomPadding Pixel amount to add to bottom
     * @param leftPadding
     * @param rightPadding
     * @param color
     * @return
     * @throws IllegalArgumentException
     * @throws ImagingOpException
     */
    protected BufferedImage pad(BufferedImage src,
            int topPadding, int bottomPadding, int leftPadding, int rightPadding,
            Color color)
            throws IllegalArgumentException, ImagingOpException {

        if (src == null) {
            throw new IllegalArgumentException("src cannot be null");
        }
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null");
        }

        topPadding = topPadding < 0 ? 0 : topPadding;
        bottomPadding = bottomPadding < 0 ? 0 : bottomPadding;
        leftPadding = leftPadding < 0 ? 0 : leftPadding;
        rightPadding = rightPadding < 0 ? 0 : rightPadding;

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        /*
		 * Calculate new height and new width
         */
        int newWidth = srcWidth + leftPadding + rightPadding;
        int newHeight = srcHeight + topPadding + bottomPadding;

        boolean colorHasAlpha = (color.getAlpha() != 255);
        boolean imageHasAlpha = (src.getTransparency() != BufferedImage.OPAQUE);

        BufferedImage result;

        /*
		 * We need to make sure our resulting image that we render into contains
		 * alpha if either our original image OR the margin color we are using
		 * contain it.
         */
        if (colorHasAlpha || imageHasAlpha) {
            result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        } else {
            result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        }

        Graphics g = result.getGraphics();

        // "Clear" the background of the new image with our margin color first.
        g.setColor(color);
        g.fillRect(0, 0, newWidth, newHeight);

        // Draw the image into the center of the new padded image.
        g.drawImage(src, leftPadding, topPadding, null);
        g.dispose();
        
        return result;
    }
}
