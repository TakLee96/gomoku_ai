import java.util.AbstractCollection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Map;

public class State {
    /* class constants */
    public static final int N = 15;
    public static final Action start = new Action(N/2, N/2);
    private static final Action[] neighbors = new Action[]{
        new Action(1, 0), new Action(-1, 0), new Action(0, 1), new Action(0, -1),
        new Action(1, 1), new Action(-1, 1), new Action(1, -1), new Action(-1, -1),
        new Action(2, 0), new Action(-2, 0), new Action(0, 2),  new Action(0, -2),
        new Action(2, 2),  new Action(-2, 2), new Action(2, -2),  new Action(-2, -2)
    };

    /* instance attributes & constructor */
    public int newX, newY;
    public String message;
    public LinkedList<Action> five;
    public LinkedList<Action> history;
    public HashMap<String, Integer> features;
    private int dx, dy;
    private boolean wins;
    private short numMoves;
    private Grid[][] board;
    private Random random;
    public State() {
        newX = -1; newY = -1;
        dx = 0; dy = 0;
        message = "It's Blue Circle's Turn.";
        wins = false;
        numMoves = 0;
        board = new Grid[N][N];
        five = new LinkedList<Action>();
        features = new HashMap<String, Integer>();
        history = new LinkedList<Action>();
        random = new Random(System.currentTimeMillis());
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                board[i][j] = new Grid();
            }
        }
    }

    public boolean isBlacksTurn() {
        return numMoves % 2 == 0;
    }

    public boolean canMove(Action a) {
        return a != null && canMove(a.x, a.y);
    }

    public boolean canMove(int x, int y) {
        return !end() && inBound(x, y) && board[x][y].isEmpty();
    }

    public void move(Action a) {
        move(a.x, a.y);
    }

    private String who() {
        return (isBlacksTurn()) ? "Blue Circle" : "Green Cross";
    }

    public void move(int x, int y) {
        newX = x; newY = y;
        board[x][y].put(isBlacksTurn());
        history.add(new Action(x, y));
        wins = win(isBlacksTurn());
        if (wins) {
            message = who() + " wins the game!";
            boolean b = board[x][y].isBlack();
            x += dx; y += dy; 
            while (inBound(x, y) && board[x][y].is(b)) {
                five.add(new Action(x, y));
                x = x + dx; y = y + dy;
            }
            x = newX - dx; y = newY - dy;
            while (inBound(x, y) && board[x][y].is(b)) {
                five.add(new Action(x, y));
                x = x - dx; y = y - dy;
            }
            five.add(new Action(newX, newY));
            newX = -1; newY = -1;
        } else {
            numMoves++;
            message = "It's " + who() + "'s Turn.";
        }
        System.out.println(this);
    }

    public Grid get(int x, int y) {
        return board[x][y];
    }

    public boolean end() {
        return wins || numMoves == N * N;
    }

    public boolean inBound(int x, int y) {
        return (x >= 0 && x < N && y >= 0 && y < N);
    }

    private int count(boolean isBlack, int x, int y, int dx, int dy) {
        int count = 0;
        x += dx; y += dy;
        while (inBound(x, y) && board[x][y].is(isBlack)) {
            count += 1;
            x += dx; y += dy;
        }
        return count;
    }

    public boolean blackWins() {
        return isBlacksTurn() && wins;
    }

    public boolean whiteWins() {
        return !isBlacksTurn() && wins;
    }

    private boolean win(boolean isBlack) {
        if (numMoves == 0 || isBlacksTurn() != isBlack) {
            return false;
        }
        if (1 + count(isBlack, newX, newY, (int) 1, (int) 0)
              + count(isBlack, newX, newY, (int)-1, (int) 0) == 5) {
            dx = 1; dy = 0;
            return true;
        }
        if (1 + count(isBlack, newX, newY, (int) 0, (int) 1)
              + count(isBlack, newX, newY, (int) 0, (int)-1) == 5) {
            dx = 0; dy = 1;
            return true;
        }
        if (1 + count(isBlack, newX, newY, (int) 1, (int) 1)
              + count(isBlack, newX, newY, (int)-1, (int)-1) == 5) {
            dx = 1; dy = 1;
            return true;
        }
        if (1 + count(isBlack, newX, newY, (int) 1, (int)-1)
              + count(isBlack, newX, newY, (int)-1, (int) 1) == 5) {
            dx = 1; dy = -1;
            return true;
        }
        return false;
    }

    public AbstractCollection<Action> getLegalActions() {
        int x, y, nx, ny;
        HashSet<Action> a = new HashSet<Action>();
        if (numMoves == 0) {
            a.add(start);
        }
        for (Action t : history) {
            for (Action n : neighbors) {
                nx = t.x + n.x; ny = t.y + n.y;
                if (inBound(nx, ny) && board[nx][ny].isEmpty()) {
                    a.add(new Action(nx, ny));
                }
            }
        }
        return a;
    }

    public Action randomAction() {
        AbstractCollection<Action> ac = getLegalActions();
        int i = 0, index = random.nextInt(ac.size());
        for (Action a : ac) {
            if (i == index) {
                return a;
            }
            i++;
        }
        return null;
    }

    public void rewindTill(Action a) {
        Action last;
        do {
            last = history.pop();
            board[last.x][last.y].clean();
            if (wins) {
                wins = false;
                five.clear();
            } else {
                numMoves--;    
            }
        } while (!a.equals(last));
        message = "It's " + who() + "'s Turn.";
        if (history.isEmpty()) {
            newX = -1; newY = -1;
        } else {
            Action temp = history.getLast();
            newX = temp.x; newY = temp.y;
        }
    }

    public Map<String, Integer> extractFeatures(Action a) {
        HashMap<String, Integer> features = new HashMap<String, Integer>();
        /* TODO: this is fucking hard */
        boolean isBlack = board[a.x][a.y].isBlack();
        
        return features;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("O ");
        for (int k = 1; k <= N; k++) {
            if (k < 10) {
                sb.append(k);
            } else {
                sb.append((char) (k - 10 + 'A'));
            }
            sb.append(" ");
        }
        sb.append("Y\n");
        for (int i = 0; i < N; i++) {
            if (i < 9) {
                sb.append(i + 1);
            } else {
                sb.append((char) (i - 9 + 'A'));
            }
            sb.append(" ");
            for (int j = 0; j < N; j++) {
                if (board[i][j].isEmpty()) {
                    sb.append("+ ");
                } else if (board[i][j].isBlack()) {
                    sb.append("o ");
                } else {
                    sb.append("x ");
                }
            }
            sb.append("|\n");
        }
        sb.append("X ");
        for (int l = 0; l <= N; l++) {
            sb.append("- ");
        }
        sb.append("\n");
        return sb.toString();
    }

}