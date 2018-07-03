import java.util.concurrent.ForkJoinPool;


public class main {
    public static void main(String[] args) {


        String test1 = "..3.2.6..9..3.5..1..18.64....81.29..7.......8..67.82....26.95..8..2.3..9..5.1.3..";
        String test2 = "4.....8.5.3..........7......2.....6.....8.4......1.......6.3.7.5..2.....1.4......";
        String test3 = "...1..26.7...3....3.2.8.4.....4.8..1.35...94.2..3.5.....6.5.7.9....4...8.57..9...";
        String test4 = "2.............62....1....7...6..8...3...9...7...6..4...4....8....52.............3";

        String[] testArray = new String[]{test1, test2, test3, test4};

        for (String test : testArray) {
            System.out.println("The input is");
            System.out.println(Matrix.show(test));

            Matrix matrix = new Matrix(test);
            Sudoku sudoku = new Sudoku(matrix);

            Sudoku.setMaxResult(10);

            ForkJoinPool pool = new ForkJoinPool();
            pool.invoke(sudoku);

            System.out.println("Has " + Sudoku.getResultList().size() + " result(s)");
            Sudoku.getResultList()
                    .forEach(
                            a -> System.out.println(Matrix.show(a.getResultString()))
                    );
            System.out.println("_________________________________");

        }


    }
}
