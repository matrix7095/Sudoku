import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.RecursiveTask;


public class Sudoku extends RecursiveTask<ArrayList<Matrix>> {
    private String question = new String();
    private ArrayList<Matrix> totalResult = new ArrayList<>();

    public Sudoku(String question) {
        this.question = question;
    }

    @Override
    protected ArrayList<Matrix> compute() {
        Matrix matrix = new Matrix(this.question);

        matrix = solve(matrix);

        if (check(matrix)) {
            totalResult.add(matrix);
            return totalResult;

        } else {
            if (matrix.getStatus().equals("failed")) {
                return totalResult;
            } else {
                List<RecursiveTask<ArrayList<Matrix>>> recursiveTaskList = new ArrayList<>();
                ArrayList<Matrix> childMatrixList = search(matrix);

                for (Matrix childMatrix : childMatrixList) {
                    Sudoku subTask = new Sudoku(childMatrix.getResultString());

                    subTask.fork();
                    recursiveTaskList.add(subTask);
                }

                for (RecursiveTask<ArrayList<Matrix>> subTask : recursiveTaskList) {
                    totalResult.addAll(subTask.join());
                }

                return totalResult;


            }
        }


    }


    public Matrix solve(Matrix matrix) {

        String before = "before";
        String after = "after";

        while (!before.equals(after)) {
            before = matrix.getResultString();

            matrix = elimination(matrix);
            matrix = update(matrix);

            after = matrix.getResultString();
        }
        return matrix;

    }

    public Matrix elimination(Matrix matrix) {
        matrix.update();

        ArrayList<Point> unknownPoints = matrix.getNeedSolvePoints();

        for (Point corePoint : unknownPoints) {
            ArrayList<Point> nearPoints = matrix.getNearPoints(corePoint);

            for (Point nearPoint : nearPoints) {
                corePoint.removeImpossibleResult(Integer.parseInt(nearPoint.getValue()));
            }

            if (corePoint.getPossibleResult().size() == 0) {
                matrix.setStatus("failed");
                return matrix;
            }
        }

        return matrix;

    }

    public Matrix update(Matrix matrix) {
        ArrayList<Point> unknownPoints = matrix.getNeedSolvePoints();

        for (Point corePoint : unknownPoints) {
            Point[] row = matrix.getRowByPoint(corePoint);
            Point[] col = matrix.getColByPoint(corePoint);
            Point[] box = matrix.getBoxByPoint(corePoint);

            Point[][] nearPoints = {row, col, box};

            for (Point[] points : nearPoints) {
                int num = choice(points, corePoint);
                if (num > 0) {
                    matrix.getPointByIndex(corePoint.getRow(), corePoint.getCol()).setValue(num + "");

                }
            }

        }

        return matrix;
    }

    public int choice(Point[] points, Point corePoint) {
        int count;
        for (int i : corePoint.getPossibleResult()) {
            count = 0;

            for (Point testPoint : points) {
                if (testPoint.getPossibleResult().contains(i)) {
                    count++;
                }
            }
            if (count == 1) {
                return i;
            }

        }


        return -1;
    }

    public ArrayList<Matrix> search(Matrix matrix) {

        ArrayList<Matrix> returnList = new ArrayList<>();

        ArrayList<Point> unknownPoints = matrix.getNeedSolvePoints();
        unknownPoints.sort((a, b) -> (a.getPossibleResult().size() - b.getPossibleResult().size()));

        Point parentPoint = unknownPoints.get(0);

        for (int num : parentPoint.getPossibleResult()) {
            Matrix childMatrix = new Matrix(matrix.getInputsString(), matrix.getResultString());

            childMatrix.getPointByIndex(parentPoint.getRow(), parentPoint.getCol()).setValue(num + "");

            returnList.add(childMatrix);
        }

        return returnList;


    }

    public boolean check(Matrix matrix) {

        boolean done = true;

        if (matrix.getStatus().equals("failed"))
            return false;

        ArrayList<Point> unknownPoints = matrix.getNeedSolvePoints();


        for (Point point : unknownPoints) {
            done = false;
            if (point.getPossibleResult().size() <= 0) {
                matrix.setStatus("failed");
            }
        }

        matrix.setStatus("done");
        return done;


    }


}

class Matrix {
    private final int ROWS_NUM = 9;
    private final int COLS_NUM = 9;

    private Point[] inputs = new Point[81];
    private Point[] result = new Point[81];
    private String status = "new";

    public Matrix(String input) {
        this.inputs = strToPoint(input);
        this.result = strToPoint(input);

    }

    public Matrix(String input, String result) {
        this.inputs = strToPoint(input);
        this.result = strToPoint(result);
        this.status = "done";
    }

    private Point[] strToPoint(String string) {
        Point[] points = new Point[81];
        String[] tokens = string.split("");
        for (int i = 0; i < ROWS_NUM; i++) {
            for (int j = 0; j < COLS_NUM; j++) {

                Point point = new Point(i, j, tokens[i * ROWS_NUM + j]);
                points[i * ROWS_NUM + j] = point;

            }

        }
        return points;
    }

    private String pointToStr(Point[] points) {
        StringBuffer stringBuffer = new StringBuffer();

        for (int i = 0; i < ROWS_NUM; i++) {
            for (int j = 0; j < COLS_NUM; j++) {

                stringBuffer.append(points[i * ROWS_NUM + j].getValue());

            }

        }

        return stringBuffer.toString();
    }

    public Point[] getInputs() {
        return inputs;
    }

    public Point[] getResult() {
        return this.result;

    }

    public String getInputsString() {
        return pointToStr(this.inputs);
    }

    public String getResultString() {
        return pointToStr(this.result);
    }

    public Point getPointByIndex(int row, int col) {
        return result[row * ROWS_NUM + col];
    }

    public ArrayList<Point> getNeedSolvePoints() {
        ArrayList<Point> points = new ArrayList<>(Arrays.asList(result));
        points.removeIf(a -> (!a.isNeedSolve()));
        return points;
    }

    public ArrayList<Point> getNearPoints(Point corePoint) {
        ArrayList<Point> points = new ArrayList<>();
        points.addAll(Arrays.asList(this.getRowByPoint(corePoint)));
        points.addAll(Arrays.asList(this.getColByPoint(corePoint)));
        points.addAll(Arrays.asList(this.getBoxByPoint(corePoint)));

        points.removeIf(a -> a.isNeedSolve());

        return points;

    }

    public Point[] getRowByPoint(Point point) {
        Point[] result = new Point[9];

        int row = point.getRow();

        for (int i = 0; i < COLS_NUM; i++) {
            result[i] = getPointByIndex(row, i);
        }

        return result;

    }

    public Point[] getColByPoint(Point point) {
        Point[] result = new Point[9];

        int col = point.getCol();

        for (int i = 0; i < ROWS_NUM; i++) {
            result[i] = getPointByIndex(i, col);
        }

        return result;

    }

    public Point[] getBoxByPoint(Point point) {
        Point[] result = new Point[9];

        int row = Math.floorDiv(point.getRow(), 3);
        int col = Math.floorDiv(point.getCol(), 3);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                result[i * 3 + j] = getPointByIndex(row * 3 + i, col * 3 + j);
            }
        }


        return result;

    }

    public String getStatus() {
        return status;
    }


    public static String show(String string) {
        StringBuffer stringBuffer = new StringBuffer();

        String[] tokens =string.split("");

        for (int i = 0; i < 9; i++) {
            stringBuffer.append("\n");
            for (int j = 0; j < 9; j++) {

                stringBuffer.append(tokens[i * 9 + j] + "\t");

            }

        }
        stringBuffer.append("\n");

        return stringBuffer.toString();
    }

    public void update() {
        for (Point point :
                this.result) {
            point.update();
        }
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setResult(String string) {
        this.status = "done";
        this.result = strToPoint(string);
    }


    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();

        for (int i = 0; i < ROWS_NUM; i++) {
            for (int j = 0; j < COLS_NUM; j++) {

                stringBuffer.append(inputs[i * ROWS_NUM + j].getValue());

            }

        }

        return stringBuffer.toString();
    }
}

class Point {
    private int row;
    private int col;
    private boolean needSolve;
    private String value;
    private ArrayList<Integer> possibleResult = new ArrayList();

    public Point(int row, int col, String value) {
        this.row = row;
        this.col = col;
        this.value = value;
        update();

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

    public void setValue(String value) {
        this.value = value;
        update();


    }

    public void update() {
        this.possibleResult.clear();

        if (value.equals(".")) {
            this.needSolve = true;

            for (int i = 1; i < 10; i++) {
                possibleResult.add(i);

            }
        } else {
            this.needSolve = false;
            possibleResult.add(Integer.parseInt(value));
        }
    }

    public void removeImpossibleResult(Integer value) {
        possibleResult.remove(value);

    }

    public ArrayList<Integer> getPossibleResult() {
        return possibleResult;
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Point other = (Point) obj;
            return this.col == other.getCol() && this.row == other.getRow();
        } catch (Exception e) {

        }

        return super.equals(obj);
    }
}
