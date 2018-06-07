import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;

public class Main {
    public static void main(String[] args) {
        String test1 = "..3.2.6..9..3.5..1..18.64....81.29..7.......8..67.82....26.95..8..2.3..9..5.1.3..";
        String test2 = "4.....8.5.3..........7......2.....6.....8.4......1.......6.3.7.5..2.....1.4......";
        String test3 = "...1..26.7...3....3.2.8.4.....4.8..1.35...94.2..3.5.....6.5.7.9....4...8.57..9...";
        Sudoku sudoku = new Sudoku(test1);

        ForkJoinPool pool = new ForkJoinPool();
        ArrayList<Matrix> matrices = pool.invoke(sudoku);

        System.out.println("Input:");
        System.out.println(Matrix.show(test1));
        System.out.println("Result:");
        for (Matrix matrix : matrices) {
            System.out.println(matrix.show(matrix.getResultString()));
            System.out.println("________________________________");
        }

    }
}
