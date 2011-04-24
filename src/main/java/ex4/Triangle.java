package ex4;

public class Triangle {
    public enum Type {
        INCOMPLETE, UNCLASSIFIED,
        EQUILATERAL, ISOSCELES, RECTANGLED, ISOSCELES_RECTANGLED, ACUTE, OBTUSE;
    }
    private int alpha;
    private int beta;
    private int gamma;
    private Type type;
    public Triangle( int alpha, int beta ) {
        this.alpha = alpha;
        this.beta = beta;
        this.type = Type.UNCLASSIFIED;
    }
    public int getAlpha() {
        return alpha;
    }
    public int getBeta() {
        return beta;
    }
    public int getGamma() {
        return gamma;
    }
    public void setGamma( int gamma ) {
        this.gamma = gamma;
    }
    public Type getType() {
        return type;
    }
    public void setType( Type type ) {
        this.type = type;
    }
    public String toString(){
        return (type == null ? "unclassified" : type.toString()) + " triangle: " +
                "alpha=" + alpha + ", beta=" + beta + ", gamma=" + gamma;
    }
}
