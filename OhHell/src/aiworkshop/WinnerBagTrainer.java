package aiworkshop;

import java.io.File;

import ml.BootstrapAggregator;
import ml.DataSet;
import ml.Tree;

public class WinnerBagTrainer {
    public static void main(String[] args) {
        int N = 2;
        int size = 100;
        int alpha = 6;
        int totalDataLimit = 500000;
        int bootstrapSampleSize = 100000;
        int printEvery = 1;
        
        boolean examineAlpha = false;
        double a1 = 0;
        double a2 = 50;
        double aStep = 1;
        boolean crossValidation = true;
        
        if (examineAlpha) {
            DataSet data = new DataSet("resources/ai workshop/AiData/Win/" + N + ".txt", totalDataLimit);
            
            if (crossValidation) {
                Tree.errorVsAlphaCv(N + 1, N, data.bootstrapSample(bootstrapSampleSize), 10, a1, a2, aStep);
            } else {
                Tree.errorVsAlpha(N + 1, N, data.bootstrapSample(bootstrapSampleSize), bootstrapSampleSize / 10, a1, a2, aStep);
            }
        } else {
            DataSet data = new DataSet("resources/ai workshop/AiData/Win/" + N + ".txt", totalDataLimit);
            
            BootstrapAggregator bag = new BootstrapAggregator(N + 1, N, size);
            bag.train(data, bootstrapSampleSize, alpha, printEvery);
            
            bag.saveToFile(new File("resources/ai workshop/OtherModels/WinBag/wb" + N + "sz" + size + "B" + bootstrapSampleSize + "a" + alpha + ".txt"));
        }
    }
}
