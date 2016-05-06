package gomoku;

import java.util.ArrayList;
import java.util.Random;
import java.util.Map;

/** Reinforcement Learning Agent
 * @author TakLee96 */
public class LearningAgent extends Agent {

    private static final int N = State.N;
    private static final double alpha = 0.2;
    private static final double gamma = 0.9;
    private static final double epsilon = 0.1;
    private static final double infinity = 1E5;
    private static final int numTraining = 10000;
    private static final int numTesting = 1;
    private static final Random random = new Random();

    private static double alpha(int episodes) {
        return Math.min(alpha / Math.log(episodes + 0.1), alpha);
    }

    private Counter weights;
    private boolean doneTraining;
    public LearningAgent(boolean isBlack) {
        super(isBlack);
        weights = new Counter();
        doneTraining = false;
    }

    private double value(State s) {
        Map<String, Integer> features = s.extractFeatures();
        return value(s, features);
    }

    private double value(State s, Map<String, Integer> features) {
        double total = 0.0;
        for (String key : s.extractFeatures().keySet())
            total += features.get(key) * weights.get(key);
        return total;
    }

    private double nextMinValue(State s) {
        double minVal = infinity; double val = 0.0;
        for (Action a : s.getLegalActions()) {
            s.move(a);
            val = value(s);
            if (val < minVal)
                minVal = val;
            s.rewind();
        }
        return minVal;
    }

    private double nextMaxValue(State s) {
        double maxVal = -infinity; double val = 0.0;
        for (Action a : s.getLegalActions()) {
            s.move(a);
            val = value(s);
            if (val > maxVal)
                maxVal = val;
            s.rewind();
        }
        return maxVal;
    }

    private Action nextMinAction(State s) {
        double minVal = infinity; double val = 0.0;
        ArrayList<Action> actions = new ArrayList<Action>();
        for (Action a : s.getLegalActions()) {
            s.move(a);
            val = value(s);
            if (val < minVal) {
                minVal = val;
                actions.clear();
                actions.add(a);
            } else if (val == minVal) {
                actions.add(a);
            }
            s.rewind();
        }
        if (actions.size() == 1) return actions.get(0);
        return actions.get(random.nextInt(actions.size()));
    }

    private Action nextMaxAction(State s) {
        double maxVal = -infinity; double val = 0.0;
        ArrayList<Action> actions = new ArrayList<Action>();
        for (Action a : s.getLegalActions()) {
            s.move(a);
            val = value(s);
            if (val > maxVal) {
                maxVal = val;
                actions.clear();
                actions.add(a);
            } else if (val == maxVal) {
                actions.add(a);
            }
            s.rewind();
        }
        if (actions.size() == 1) return actions.get(0);
        return actions.get(random.nextInt(actions.size()));
    }

    private double nextValue(State s) {
        if (s.isBlacksTurn())
            return nextMaxValue(s);
        return nextMinValue(s);
    }

    // TODO: be careful with this
    private void observe(State s, double reward, int episodes) {
        Map<String, Integer> features = s.extractFeatures();
        double delta = reward + gamma * nextValue(s) - value(s, features);
        for (String key : features.keySet())
            weights.put(key, weights.get(key) + alpha(episodes) * delta * features.get(key));
    }

    private void doneTraining() {
        doneTraining = true;
    }

    private Action getPolicy(State s) {
        if (isBlack) return nextMaxAction(s);
        return nextMinAction(s);
    }

    @Override
    public Action getAction(State s) {
        if (doneTraining || random.nextDouble() > epsilon)
            return getPolicy(s);
        return s.randomAction();
    }

    private static void playGame(LearningAgent b, LearningAgent w, boolean withGraphics, int episodes) {
        State s = new State();
        System.out.print(episodes);
        while (!s.ended()) {
            if (s.isBlacksTurn()) {
                s.move(b.getAction(s));
            } else {
                s.move(w.getAction(s));
            }
            if (withGraphics) GUIDriver.drawBoard(s);
            System.out.print(".");
            // b.observe(s, 0.0, episodes);
            // w.observe(s, 0.0, episodes);
        }
        double blackReward = 0.0;
        if (s.win(true)) {
            System.out.println("win");
            blackReward = 1.0;
        } else if (s.win(false)) {
            System.out.println("lose");
            blackReward = -1.0;
        } else {
            System.out.println("tie");
            blackReward = 0.0;
        }
        b.observe(s, blackReward, episodes);
        w.observe(s, -blackReward, episodes);
        if (withGraphics) System.out.println(s.history);
    }

    public static void main(String[] args) {
        boolean withGraphics = args != null && args.length > 0;
        LearningAgent b = new LearningAgent(true);
        LearningAgent w = new LearningAgent(false);
        if (withGraphics) GUIDriver.init();

        for (int i = 1; i <= numTraining; i++) {
            playGame(b, w, withGraphics, i);
        }

        System.out.println(b.weights);
        b.doneTraining();
        w.doneTraining();
        if (!withGraphics && numTesting > 0) GUIDriver.init();
        for (int j = 0; j < numTesting; j++) {
            playGame(b, w, true, 0);
        }

        System.exit(0);
    }

}
