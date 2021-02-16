package ml;

public class ClassificationError implements LossFunction {
    @Override
    public double loss(Vector predicted, Vector truth) {
        int index = 0;
        double max = 0;
        double[] arr = predicted.toArray();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                index = i;
            }
        }
        return truth.get(index) == 1 ? 0 : 1;
    }

    @Override
    public Vector dloss(Vector predicted, Vector truth) {
        throw new MLException("Attempted to compute gradient with classification error as a loss function.");
    }
}
