package ml;

public class Testing {
    public static void main(String[] args) {
        DataSet data = new DataSet("C:/Users/campb/Desktop/AiData/Win/5.txt", 100000);
        
        //Tree.errorVsAlpha(7, 6, data.bootstrapSample(100000), 10000, 0, 50, 1);
        
        DataSet sample = data.bootstrapSample(10000);
        Tree tree = new Tree(6, 5);
        long time = System.currentTimeMillis();
        tree.trainTopDown(sample, 2);
        tree.print();
        System.out.println(tree.numLeaves());
        System.out.println(tree.totalLoss());
        System.out.println(tree.testError(sample, new MeanSquaredError()));
        System.out.println(tree.testError(data, new MeanSquaredError()));
        System.out.println(System.currentTimeMillis() - time + " ms");
        
        System.out.println("done");
    }
}
