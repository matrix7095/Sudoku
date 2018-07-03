import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.*;


public class Sudoku extends RecursiveAction {
    private Matrix question;
    private static int maxResult;
    private static HashSet<Matrix> resultList = new HashSet<>();

    public Sudoku(Matrix question) {

        this.question = new Matrix(question);
    }


    @Override
    protected void compute() {

        question = solve(question);

        if (question.getStatus().equals(Matrix.Status.SUCCESSED) && resultList.size() < maxResult) {
            resultList.add(question);
        } else if (question.getStatus().equals(Matrix.Status.PROCESSING) && resultList.size() < maxResult) {
            List<Sudoku> sudokuList =
                    search(question).stream()
                            .map(
                                    a -> {
                                        Sudoku subTask = new Sudoku(a);
                                        subTask.fork();
                                        return subTask;
                                    }
                            )
                            .collect(Collectors.toList());

            sudokuList.forEach(Sudoku::join);

        }


    }

    public static Matrix solve(Matrix matrix) {
        check(matrix);

        if (matrix.getStatus().equals(Matrix.Status.SUCCESSED) ||
                matrix.getStatus().equals(Matrix.Status.FAILED))
            return matrix;

        String before = "before";
        String after = "after";

        while (!before.equals(after)) {
            before = matrix.getResultString();

            matrix = elimination(matrix);
            matrix = update(matrix);

            after = matrix.getResultString();
        }

        check(matrix);

        return matrix;

    }

    public static Matrix elimination(Matrix matrix) {
        matrix.update();

        matrix.setStatus(Matrix.Status.PROCESSING);

        matrix.getNeedSolvePoint()
                .parallelStream()
                .forEach(
                        a -> {
                            a.removeImpossibleResult(
                                    Stream.concat(
                                            matrix.getRowByPoint(a).stream(),
                                            Stream.concat(
                                                    matrix.getColByPoint(a).stream(),
                                                    matrix.getBoxByPoint(a).stream()
                                            )
                                    ).filter(b -> !b.isNeedSolve())
                                            .map(Point::getValue)
                                            .distinct()
                                            .map(Integer::parseInt)
                                            .collect(Collectors.toList())
                            );

                            if (a.getPossibleResult().size() == 0) {
                                matrix.setStatus(Matrix.Status.FAILED);
                            }

                        }
                );


        return matrix;

    }

    public static Matrix update(Matrix matrix) {
        if (matrix.getStatus().equals(Matrix.Status.FAILED))
            return matrix;

        BiConsumer<Point, List<Point>> choice = (point, points) ->
                point.getPossibleResult()
                        .parallelStream()
                        .filter(a -> {
                            long count = points
                                    .stream()
                                    .filter(b -> b.getPossibleResult().contains(a))
                                    .count();
                            return count == 1L;
                        })
                        .findFirst()
                        .ifPresent(a -> point.setValue(a + ""));

        matrix.getNeedSolvePoint()
                .parallelStream()
                .forEach(a -> {
                    choice.accept(a, matrix.getRowByPoint(a));
                    choice.accept(a, matrix.getColByPoint(a));
                    choice.accept(a, matrix.getBoxByPoint(a));

                });


        return matrix;

    }

    public static boolean check(Matrix matrix) {
        if (matrix.getStatus().equals(Matrix.Status.FAILED))
            return false;

        if (matrix.getNeedSolvePoint().size() == 0)
            matrix.setStatus(Matrix.Status.SUCCESSED);


        matrix.getNeedSolvePoint()
                .parallelStream()
                .filter(a -> a.getPossibleResult().size() == 0)
                .findFirst()
                .ifPresent(a -> matrix.setStatus(Matrix.Status.FAILED));


        Predicate<List<Point>> checkValid = (list) -> {

            List<String> testList = list.stream()
                    .filter(a -> !a.isNeedSolve())
                    .map(Point::getValue)
                    .collect(Collectors.toList());

            return testList.size() == new HashSet<>(testList).size();
        };

        IntStream.range(0, 9)
                .filter(a ->
                        (!checkValid.test(matrix.getRowByPoint(new Point(a, a, ".")))) ||
                                (!checkValid.test(matrix.getColByPoint(new Point(a, a, ".")))) ||
                                (!checkValid.test(matrix.getBoxByPoint(new Point(a / 3 * 3, a % 3 * 3, "."))))
                )
                .findFirst()
                .ifPresent(a -> matrix.setStatus(Matrix.Status.FAILED));

        return matrix.getStatus().equals(Matrix.Status.SUCCESSED);
    }

    public static List<Matrix> search(Matrix matrix) {

        List<Matrix> matrixList =
                matrix.getNeedSolvePoint()
                        .stream()
                        .sorted(Comparator.comparingInt(a -> a.getPossibleResult().size()))
                        .limit(1)
                        .flatMap(
                                a -> a.getPossibleResult()
                                        .stream()
                                        .map(b -> {
                                            Matrix childMatrix = new Matrix(matrix);
                                            childMatrix.getPointByIndex(a.getRow(), a.getCol()).setValue(b + "");
                                            return childMatrix;
                                        })

                        )
                        .collect(Collectors.toList());

        return matrixList;

    }

    public static HashSet<Matrix> getResultList() {
        return resultList;
    }

    public static void setMaxResult(int max) {
        maxResult = max;
        resultList = new HashSet<>();
    }
}


class Matrix {
    private final int ROWS_NUM = 9;
    private final int COLS_NUM = 9;
    private Status status = Status.NEW;

    private List<Point> inputs = new ArrayList<>();
    private List<Point> result = new ArrayList<>();

    public Matrix(String input) {
        this.inputs = strToPoint(input);
        this.result = strToPoint(input);
    }

    public Matrix(Matrix matrix) {
        this(matrix.getInputsString(), matrix.getResultString(), Status.NEW);
    }

    public Matrix(String input, String result, Status status) {
        this.inputs = strToPoint(input);
        this.result = strToPoint(result);
        this.status = status;
    }

    private static List<Point> strToPoint(String string) {
        List<Point> points = IntStream.range(0, 81)
                .mapToObj(n -> new Point(n / 9, n % 9, string.substring(n, n + 1)))
                .collect(Collectors.toList());

        return points;

    }

    private static String pointToStr(List<Point> points) {
        return points.stream().map(Point::getValue).collect(Collectors.joining());

    }

    public static String show(String string) {
        StringBuffer stringBuffer = new StringBuffer();

        String[] tokens = string.split("");

        for (int i = 0; i < 9; i++) {
            stringBuffer.append("\n");
            for (int j = 0; j < 9; j++) {

                stringBuffer.append(tokens[i * 9 + j] + "\t");

            }

        }
        stringBuffer.append("\n");

        return stringBuffer.toString();
    }

    public List<Point> getResult() {
        return result;
    }

    public String getInputsString() {
        return pointToStr(this.inputs);
    }

    public String getResultString() {
        return pointToStr(this.result);
    }

    public Status getStatus() {
        return status;
    }

    public Point getPointByIndex(int row, int col) {
        return result.get(row * ROWS_NUM + col);
    }

    public List<Point> getNeedSolvePoint() {
        return this.result.parallelStream().filter(Point::isNeedSolve).collect(Collectors.toList());
    }

    public List<Point> getRowByPoint(Point corePoint) {
        List<Point> points = IntStream.range(0, 9)
                .mapToObj(n -> getPointByIndex(corePoint.getRow(), n))
                .collect(Collectors.toList());

        return points;
    }

    public List<Point> getColByPoint(Point corePoint) {
        List<Point> points = IntStream.range(0, 9)
                .mapToObj(n -> getPointByIndex(n, corePoint.getCol()))
                .collect(Collectors.toList());

        return points;
    }

    public List<Point> getBoxByPoint(Point corePoint) {
        List<Point> points = IntStream.range(0, 9)
                .mapToObj(n -> getPointByIndex((corePoint.getRow() / 3) * 3 + n / 3, (corePoint.getCol() / 3) * 3 + n % 3))
                .collect(Collectors.toList());

        return points;

    }

    public void update() {
        this.result.parallelStream().forEach(Point::update);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        NEW, PROCESSING, SUCCESSED, FAILED,

    }

    @Override
    public boolean equals(Object obj) {

        try {
            Matrix otherMatrix = (Matrix) obj;
            return this.getResultString().equals(otherMatrix.getResultString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.equals(obj);
    }
}


class Point {
    private int row;
    private int col;
    private boolean needSolve;
    private String value;
    private List<Integer> possibleResult = new ArrayList<>();

    public Point(int row, int col, String value) {
        this.row = row;
        this.col = col;
        this.value = value;
        this.update();
    }

    public Point(Point point) {
        this.row = point.getRow();
        this.col = point.getCol();
        this.value = point.getValue();
        this.update();
    }

    public void update() {

        if (value.equals(".")) {
            this.needSolve = true;
            this.possibleResult = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        } else {
            this.needSolve = false;
            this.possibleResult = new ArrayList<>(Arrays.asList(Integer.parseInt(this.value)));
        }

    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String getValue() {
        return value;
    }

    public boolean isNeedSolve() {
        return needSolve;
    }

    public List<Integer> getPossibleResult() {
        return possibleResult;
    }

    public void setValue(String value) {
        this.value = value;
        this.update();
    }

    public void removeImpossibleResult(List<Integer> list) {
        list.forEach(a -> this.possibleResult.remove(a));
    }
}