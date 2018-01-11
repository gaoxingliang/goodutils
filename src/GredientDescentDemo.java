import java.util.ArrayList;
import java.util.List;

public class GredientDescentDemo {
    /**
     * 一个完整的梯度下降代码:
     * 假设方程是: y= 3x1 + 4x2
     * 然后给出2组示例数据, 并求出3, 4
     * <p>
     * Created by edward.gao on 11/01/2018.
     */
    public static void main(String[] args) {

        List<Record> recordList = new ArrayList<>();

        recordList.add(new Record(1, 1, 7));
        recordList.add(new Record(2, 5, 26));


        Theta now = new Theta();
        double stepAlpha = 0.01d;
        double precision = 0.000001d;
        int iteration = 0;
        while (iteration++ < 10000) {

            Theta nextTheta = newTheta(now, recordList, stepAlpha);

            // if it's convergence
            // 看看是否收敛
            double dist = calDistance(nextTheta, now);
            //System.out.println("now=" + now + "next=" + nextTheta + ", dist=" + dist) ;

            if (Math.abs(dist - precision) < precision) {
                System.out.println("Finished after " + iteration + " and result is - " + nextTheta);
                break;
            }
            else {
                now = nextTheta;
            }
        }

    }


    private static Theta newTheta(Theta now, List<Record> recordList, double stepAlpha) {
        Theta stepTheta = new Theta();

        /**
         * 如何计算新的Theta
         * 规则: Theta(i) = oldTheta(i) + alpha * Sum [ y(i) - hTheta(xi)*xj]
         */

        stepTheta.o1 = now.o1 + stepAlpha * (
                recordList.get(0).x1 * (recordList.get(0).y - hOfThetaO1(now, recordList, 0))
                        +
                        recordList.get(1).x1 * (recordList.get(1).y - hOfThetaO1(now, recordList, 1)));

        stepTheta.o2 = now.o2 + stepAlpha * (
                recordList.get(0).x2 * (recordList.get(0).y - hOfThetaO1(now, recordList, 0))
                        +
                        recordList.get(1).x2 * (recordList.get(1).y - hOfThetaO1(now, recordList, 1)));
        return stepTheta;

    }

    /**
     * hTheta 是我们假设的函数, 这里我们假设他是一个二元一次的
     */
    private static double hOfThetaO1(Theta theta, List<Record> recordList, int m) {
        return theta.o1 * recordList.get(m).x1 + theta.o2 * recordList.get(m).x2;
    }

    private static double calDistance(Theta t1, Theta t2) {
        return Math.sqrt(Math.pow(t1.o1 - t2.o1, 2) + Math.pow(t1.o2 - t2.o2, 2));
    }

    static class Theta {
        double o1 = 0d, o2 = 0d;

        public String toString() {
            return "(" + o1 + "," + o2 + ")";
        }
    }

    // y = 3x1 + 4x2
    static class Record {
        public final int x1, x2, y;

        public Record(int x1, int x2, int y) {
            this.x1 = x1;
            this.x2 = x2;
            this.y = y;
        }
    }

}
