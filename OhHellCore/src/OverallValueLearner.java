import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class OverallValueLearner extends Learner {
    Hashtable<Card, List<LayerVector>> ins = new Hashtable<>();
    Hashtable<Card, Double> outs = new Hashtable<>();
    private LinkedList<LinkedList<LayerVector>> dataAsList;
    
    public OverallValueLearner(int[] ds) {
        super(ds, new ActivationFunction[] {
                new ReLuFunction(),
                new SigmoidFunction()
                }, null);
    }
    
    public void putIn(Card c, LayerVector in) {
        if (ins.get(c) == null) {
            ins.put(c, new LinkedList<>());
        }
        ins.get(c).add(in);
    }
    
    public void putOut(Card c, int out) {
        outs.put(c, (double) out);
    }
    
    public void makeDataList() {
        dataAsList = new LinkedList<>();
        for (Card c : ins.keySet()) {
            for (LayerVector in : ins.get(c)) {
                dataAsList.add(new LinkedList<>(Arrays.asList(
                        in, 
                        (LayerVector) new BasicLayerVector(new double[] {outs.get(c)})
                        )));
            }
        }
        ins = new Hashtable<>();
        outs = new Hashtable<>();
    }
    
    public int dataSize() {
        return dataAsList.size();
    }
    
    public LinkedList<LayerVector> getDatum(LayerVector in) {
        if (in == null) {
            return dataAsList.pop();
        } else {
            return new LinkedList<>(Arrays.asList(in, null));
        }
    }
}
