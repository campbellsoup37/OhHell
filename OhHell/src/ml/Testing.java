package ml;

public class Testing {
    public static void main(String[] args) {
        DataSet data = new DataSet("C:/Users/campb/Desktop/AiData/Win/5.txt", 1000000);
        
        data.allocateTestData(100000);
        
        BootstrapAggregator bag = new BootstrapAggregator("resources/models/wb5.txt");
        
        System.out.println(bag.testError(data.complement(), new ClassificationError()));
        
        GradientBooster boost = new GradientBooster("resources/ai workshop/OtherModels/WinBoost/boost900.txt");
        
        System.out.println(boost.testError(data.complement(), new ClassificationError()));
        
        /*BootstrapAggregator bag = new BootstrapAggregator(6, 5, 100);
        bag.train(data, 1000, 10, 1);
        System.out.println("bag error: " + bag.testError(data.complement(), new MeanSquaredError()));
        
        GradientBooster boost = new GradientBooster(6, 5, 300);
        boost.train(data, 2, 1);
        System.out.println("boost error: " + boost.testError(data.complement(), new MeanSquaredError()));
        
        
        //Tree.errorVsAlpha(7, 6, data.bootstrapSample(100000), 10000, 0, 50, 1);
        
        Tree tree = new Tree(6, 5);
        long time = System.currentTimeMillis();
        tree.trainBottomUp(data, 10);
        //tree.print();
        //System.out.println(tree.totalLoss());
        System.out.println(tree.numLeaves() + "-node tree error: " + tree.testError(data.complement(), new MeanSquaredError()));
        System.out.println(System.currentTimeMillis() - time + " ms");
        
        System.out.println("done");*/
    }
}
