package nl.debijenkorf.tools.photoresizer.resizer;

/**
 *
 * @author Maarten Blokker
 */
public class Preset {

    private final double topLine;
    private final double baseLine;
    private final double leftRightMargin;
    private final VAlign valign;

    public enum VAlign {
        BOTTOM,
        MIDDLE
    }

    public Preset(double topLine, double baseLine, double leftRightMargin) {
        this(topLine, baseLine, leftRightMargin, VAlign.BOTTOM);
    }

    public Preset(double topLine, double baseLine, double leftRightMargin, VAlign valign) {
        this.topLine = topLine;
        this.baseLine = baseLine;
        this.leftRightMargin = leftRightMargin;
        this.valign = valign;
    }

    public double getTopLine() {
        return topLine;
    }

    public double getBaseLine() {
        return baseLine;
    }

    public double getLeftRightMargin() {
        return leftRightMargin;
    }

    public VAlign getValign() {
        return valign;
    }

}
