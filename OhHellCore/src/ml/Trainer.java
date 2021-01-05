package ml;

public abstract class Trainer extends Thread {
    public void notifyDatumNumber(Learner l, int datumNumber, int datumTotal) {}
    
    public void addLog(Learner l, String text) {}
}
