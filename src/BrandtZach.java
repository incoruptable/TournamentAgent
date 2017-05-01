import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.TreeSet;

import static java.lang.Math.*;

/**
 * Created by zach on 4/24/2017.
 */
public class BrandtZach implements IAgent {

    public static float maxSpeed = -2000000;
    boolean goBerzerkMode;
    int enemiesAlive, friendliesAlive;
    int numAttackers = 0;
    int index;

    public BrandtZach() {
        reset();
    }

    private static float sq_dist(float xSource, float ySource, float xDest, float yDest) {
        return (xDest - xSource) * (xDest - xSource) + (yDest - ySource) * (yDest - ySource);
    }

    private void getMaxSpeed(Model m) {
        for (int i = 0; i < Model.XMAX; i++) {
            for (int j = 0; j < Model.YMAX; i++) {
                maxSpeed = max(maxSpeed, m.getTravelSpeed(i, j));
            }
        }
    }

    @Override
    public void reset() {
        goBerzerkMode = false;
        enemiesAlive = 3;
        friendliesAlive = 3;
        numAttackers = 0;
    }

    @Override
    public void update(Model m) {
        if (maxSpeed > -200000) {
            getMaxSpeed(m);
        }

        numAttackers = 0;
        getAliveCount(m);
        for (int i = 0; i < m.getSpriteCountSelf(); i++) {
            chooseType(m, i);
        }

    }

    private void getAliveCount(Model m) {
        int j = 0;
        for (int i = 0; i < m.getSpriteCountOpponent(); i++) {
            if (m.getEnergyOpponent(i) >= 0) {
                j++;
            }
        }
        enemiesAlive = j;
        j = 0;
        for (int i = 0; i < m.getSpriteCountSelf(); i++) {
            if (m.getEnergySelf(i) >= 0) {
                j++;
            }
        }
        friendliesAlive = j;
    }
    private void chooseType(Model m, int indexOfSprite) {

        if (!anyEnemiesAlive(m)) {
            goBerzerkMode = true;
            if (numAttackers >= 1) {
                beFlagAttacker(m, indexOfSprite);
            } else {
                beAttacker(m, indexOfSprite);
                numAttackers++;
            }

        } else if (enemiesAlive == 1 && friendliesAlive >= 2) {
            if (numAttackers >= 1) {
                beAttacker(m, indexOfSprite);
            } else {
                beAttacker(m, indexOfSprite);
                numAttackers++;
            }
        } else if (enemiesAlive >= 2) {
            bestDefender(m);
            if (index == indexOfSprite) {
                beDefender(m, indexOfSprite);
            } else {
                beAttacker(m, indexOfSprite);
            }
        }
    }

    float bestDefender(Model m) {
        index = -1;
        float dd = Float.MAX_VALUE;
        for (int i = 0; i < m.getSpriteCountSelf(); i++) {
            if (m.getEnergySelf(i) < 0)
                continue; // don't care about dead agents
            float d = sq_dist(Model.XFLAG, Model.YFLAG, m.getX(i), m.getY(i));
            if (d < dd) {
                dd = d;
                index = i;
            }
        }
        return dd;
    }

    private boolean anyEnemiesAlive(Model m) {
        for (int i = 0; i < m.getSpriteCountOpponent(); i++) {
            if (m.getEnergyOpponent(i) > 0) {

                return true;
            }
        }
        return false;
    }

    float nearestOpponent(Model m, float x, float y) {
        index = -1;
        float dd = Float.MAX_VALUE;
        for (int i = 0; i < m.getSpriteCountOpponent(); i++) {
            if (m.getEnergyOpponent(i) < 0)
                continue; // don't care about dead opponents
            float d = sq_dist(x, y, m.getXOpponent(i), m.getYOpponent(i));
            if (d < dd) {
                dd = d;
                index = i;
            }
        }
        return dd;
    }

    private void beFlagAttacker(Model m, int i) {

        // Head for the opponent's flag
        moveAlongBestPath(m, i, Model.XFLAG_OPPONENT - Model.MAX_THROW_RADIUS + 1, Model.YFLAG_OPPONENT);

        // Avoid opponents
        float myX = m.getX(i);
        float myY = m.getY(i);
        float dx, dy;
        nearestOpponent(m, myX, myY);
        if (index >= 0) {
            float enemyX = m.getXOpponent(index);
            float enemyY = m.getYOpponent(index);
            float distanceToEnemy = sq_dist(myX, myY, enemyX, enemyY);
            if (sq_dist(enemyX, enemyY, myX, myY) <= (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS * .4) * (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS * .4)) {
                if (sq_dist(enemyX, enemyY, myX, myY) <= (Model.MAX_THROW_RADIUS * Model.MAX_THROW_RADIUS)) {
                    m.throwBomb(i, enemyX, enemyY);
                }
                dx = enemyX - myX;
                dy = enemyY - myY;
                float scale = (float) (Model.MAX_THROW_RADIUS / Math.sqrt(distanceToEnemy));
                float throwX = dx * scale + myX;
                float throwY = dy * scale + myY;
                m.throwBomb(i, throwX, throwY);
            }
        }

        // Shoot at the flag if I can hit it
        if (sq_dist(m.getX(i), m.getY(i), Model.XFLAG_OPPONENT, Model.YFLAG_OPPONENT) <= Model.MAX_THROW_RADIUS * Model.MAX_THROW_RADIUS) {
            m.throwBomb(i, Model.XFLAG_OPPONENT, Model.YFLAG_OPPONENT);
        }

        // Try not to die
        avoidBombs(m, i);
    }

    void beDefender(Model m, int i) {
        float myX = m.getX(i);
        float myY = m.getY(i);

        float dx, dy;
        // Find the opponent nearest to my flag
        nearestOpponent(m, Model.XFLAG, Model.YFLAG);
        if (index >= 0) {
            float enemyX = m.getXOpponent(index);
            float enemyY = m.getYOpponent(index);
            float distanceToEnemy = sq_dist(enemyX, enemyY, myX, myY);

            // Stay between the enemy and my flag
            moveAlongBestPath(m, i, 0.5f * (Model.XFLAG + enemyX), 0.5f * (Model.YFLAG + enemyY));

            // Throw boms if the enemy gets close enough
            if (sq_dist(enemyX, enemyY, myX, myY) <= (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS * .4) * (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS * .4)) {
                if (sq_dist(enemyX, enemyY, myX, myY) <= (Model.MAX_THROW_RADIUS * Model.MAX_THROW_RADIUS)) {
                    m.throwBomb(i, enemyX, enemyY);
                }
                dx = enemyX - myX;
                dy = enemyY - myY;
                float scale = (float) (Model.MAX_THROW_RADIUS / Math.sqrt(distanceToEnemy));
                float throwX = dx * scale + myX;
                float throwY = dy * scale + myY;
                m.throwBomb(i, throwX, throwY);
            }
        } else {
            // Guard the flag
            moveAlongBestPath(m, i, Model.XFLAG + Model.MAX_THROW_RADIUS, Model.YFLAG);
        }

        // If I don't have enough energy to throw a bomb, rest
        if (m.getEnergySelf(i) < Model.BOMB_COST)
            m.setDestination(i, m.getX(i), m.getY(i));

        // Try not to die
        avoidBombs(m, i);
    }

    private void beAttacker(Model m, int indexOfSprite) {
        float myX = m.getX(indexOfSprite);
        float myY = m.getY(indexOfSprite);

        // Find the opponent nearest to me
        nearestOpponent(m, myX, myY);
        if (index >= 0) {
            float enemyX = m.getXOpponent(index);
            float enemyY = m.getYOpponent(index);
            float distanceToEnemy = sq_dist(myX, myY, enemyX, enemyY);

            if (m.getEnergySelf(indexOfSprite) >= m.getEnergyOpponent(index)) {

                // Get close enough to throw a bomb at the enemy
                float dx = myX - enemyX;
                float dy = myY - enemyY;
                float t = 1.0f / Math.max(Model.EPSILON, (float) Math.sqrt(dx * dx + dy * dy));
                dx *= t;
                dy *= t;
                moveAlongBestPath(m, indexOfSprite, enemyX + dx * (Model.MAX_THROW_RADIUS - Model.EPSILON), enemyY + dy * (Model.MAX_THROW_RADIUS - Model.EPSILON));

                // Throw bombs
                if (sq_dist(enemyX, enemyY, myX, myY) <= (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS * .4) * (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS * .4)) {
                    if (sq_dist(enemyX, enemyY, myX, myY) <= (Model.MAX_THROW_RADIUS * Model.MAX_THROW_RADIUS)) {
                        m.throwBomb(indexOfSprite, enemyX, enemyY);
                    }
                    dx = enemyX - myX;
                    dy = enemyY - myY;
                    float scale = (float) (Model.MAX_THROW_RADIUS / Math.sqrt(distanceToEnemy));
                    float throwX = dx * scale + myX;
                    float throwY = dy * scale + myY;
                    m.throwBomb(indexOfSprite, throwX, throwY);
                }
            } else {

                // If the opponent is close enough to shoot at me...
                if (sq_dist(enemyX, enemyY, myX, myY) <= (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS) * (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS) + 10) {
                    moveAlongBestPath(m, indexOfSprite, myX + 10.0f * (myX - enemyX), myY + 10.0f * (myY - enemyY)); // Flee
                } else {
                    m.setDestination(indexOfSprite, myX, myY); // Rest
                }
            }
        } else {
            // Head for the opponent's flag
            moveAlongBestPath(m, indexOfSprite, Model.XFLAG_OPPONENT - Model.BLAST_RADIUS - 60, Model.YFLAG_OPPONENT + 70);

            // Shoot at the flag if indexOfSprite can hit it
            if (enemyAboutToRevive(m)) {
                m.throwBomb(indexOfSprite, m.getXOpponent(index), m.getYOpponent(index));
            } else if (sq_dist(m.getX(indexOfSprite), m.getY(indexOfSprite), Model.XFLAG_OPPONENT, Model.YFLAG_OPPONENT) <= Model.MAX_THROW_RADIUS * Model.MAX_THROW_RADIUS) {
                m.throwBomb(indexOfSprite, Model.XFLAG_OPPONENT, Model.YFLAG_OPPONENT);
            }
        }

        // Try not to die
        avoidBombs(m, indexOfSprite);

    }

    private boolean enemyAboutToRevive(Model m) {
        float distanceToRevive = 50;
        for (int i = 0; i < m.getSpriteCountOpponent(); i++) {
            if (m.getEnergyOpponent(i) < 0) {
                if (sq_dist(m.getXOpponent(i), m.getYOpponent(i), Model.XFLAG_OPPONENT, Model.YFLAG_OPPONENT) < distanceToRevive) {
                    index = i;
                    return true;
                }
            }
        }
        return false;
    }

    void avoidBombs(Model m, int i) {
        if (nearestBombTarget(m, m.getX(i), m.getY(i)) <= 2.0f * Model.BLAST_RADIUS * Model.BLAST_RADIUS) {
            float bestX = m.getX(i);
            float bestY = m.getY(i);
            float modelX = bestX;
            float modelY = bestY;
            float best_sqdist = 0;
            for (int sim = 0; sim < 8; sim++) { // try 4 candidate destinations

                float x = modelX;
                float y = modelY;
                // Pick a random destination to simulate
                if (sim == 0) {
                    x += 100;
                } else if (sim == 1) {
                    x += 100;
                    y += 100;
                } else if (sim == 2) {
                    y += 100;
                } else if (sim == 3) {
                    x -= 100;
                    y += 100;
                } else if (sim == 4) {
                    x -= 100;
                } else if (sim == 5) {
                    x -= 100;
                    y -= 100;
                } else if (sim == 6) {
                    y -= 100;
                } else {
                    x += 100;
                    y -= 100;
                }

                // Fork the universe and simulate it for 10 time-steps
                Controller cFork = m.getController().fork(new BrandtZachShadow(x, y), new OpponentShadow());
                Model mFork = cFork.getModel();
                for (int j = 0; j < 10; j++)
                    cFork.update();

                // See how close the current sprite got to the opponent's flag in the forked universe
                float sqd = sq_dist(mFork.getX(i), mFork.getY(i), m.getBombTargetX(index), m.getBombTargetY(index));
                if (sqd > best_sqdist) {
                    best_sqdist = sqd;
                    bestX = x;
                    bestY = y;
                }
            }
            m.setDestination(i, bestX, bestY);
        }
    }

    float nearestBombTarget(Model m, float x, float y) {
        index = -1;
        float dd = Float.MAX_VALUE;
        for (int i = 0; i < m.getBombCount(); i++) {
            float d = sq_dist(x, y, m.getBombTargetX(i), m.getBombTargetY(i));
            if (d < dd) {
                dd = d;
                index = i;
            }
        }
        return dd;
    }

    private boolean closestEnemyToFlag(Model m) {
        float bestDistance = Float.MAX_VALUE;
        index = -1;
        for (int i = 0; i < m.getSpriteCountOpponent(); i++) {
            if (!(m.getEnergyOpponent(i) < 0)) {
                float spriteDistance = sq_dist(Model.XFLAG_OPPONENT, Model.YFLAG_OPPONENT, m.getXOpponent(i), m.getYOpponent(i));
                if (bestDistance > spriteDistance) {
                    index = i;
                    bestDistance = spriteDistance;
                }
            }
        }
        return index != -1;
    }

    private void moveAlongBestPath(Model m, int indexOfSprite, float destX, float destY) {
        State bestPath = A_Search.findBestPath(m, indexOfSprite, destX, destY);

        if (bestPath.parent != null) {
            State prevState = bestPath;
            State currentState = bestPath.parent;
            while (currentState.parent != null) {
                prevState = currentState;
                currentState = currentState.parent;
            }
            m.setDestination(indexOfSprite, prevState.x, prevState.y);
        }
    }


    public static class A_Search {
        private static Model model;
        private static TreeSet<State> visitedStates;
        private static PriorityQueue<State> frontier;

        public static State findBestPath(Model m, int indexOfSprite, float destX, float destY) {
            StateComparatorPriority comparatorPriority = new StateComparatorPriority();
            StateComparator comparator = new StateComparator();
            frontier = new PriorityQueue<>(comparatorPriority);
            visitedStates = new TreeSet<>(comparator);
            model = m;
            State startState = new State(null);
            startState.cost = 0.0;
            float modifiedSourceX = (int) (m.getX(indexOfSprite) / 10) * 10;
            float modifiedSourceY = (int) (m.getY(indexOfSprite) / 10) * 10;
            startState.x = modifiedSourceX;
            startState.y = modifiedSourceY;
            destX = (int) (destX / 10) * 10;
            destY = (int) (destY / 10) * 10;

            frontier.add(startState);
            visitedStates.add(startState);

            while (frontier.size() > 0) {
                State current = frontier.poll();
                if (current.x == destX && current.y == destY) {
                    return current;
                }
                State next = new State(current);

                checkChildAStar(next, 10, -10, destX, destY);
                next = new State(current);
                checkChildAStar(next, 10, 0, destX, destY);
                next = new State(current);
                checkChildAStar(next, 10, 10, destX, destY);
                next = new State(current);
                checkChildAStar(next, 0, 10, destX, destY);
                next = new State(current);
                checkChildAStar(next, 0, -10, destX, destY);
                next = new State(current);
                checkChildAStar(next, -10, -10, destX, destY);
                next = new State(current);
                checkChildAStar(next, -10, 0, destX, destY);
                next = new State(current);
                checkChildAStar(next, -10, 10, destX, destY);

            }

            return new State(null);
        }

        public static void checkChildAStar(State nextState, int xDelta, int yDelta, float destX, float destY) {
            nextState.x = nextState.parent.x + xDelta;
            nextState.y = nextState.parent.y + yDelta;
            nextState.heuristicValue = sq_dist(nextState.x, nextState.y, destX, destY) / maxSpeed;
            if (nextState.x < Model.XMAX && nextState.x >= 0 && nextState.y < Model.YMAX && nextState.y >= 0)
                if (abs(xDelta) + abs(yDelta) == 20)
                    nextState.cost = (10 * sqrt(2) / model.getTravelSpeed(nextState.x, nextState.y)) + nextState.parent.cost;
                else
                    nextState.cost = (10 / model.getTravelSpeed(nextState.x, nextState.y)) + nextState.parent.cost;
            else
                return;

            if (visitedStates.contains(nextState)) {
                State oldChild = visitedStates.floor(nextState);
                if (nextState.cost < oldChild.cost) {
                    oldChild.cost = nextState.cost;
                    oldChild.parent = nextState.parent;
                }
            } else {
                frontier.add(nextState);
                visitedStates.add(nextState);
            }
        }
    }

    public static class State {
        public float x;
        public float y;
        public double cost;
        public float heuristicValue;
        public State parent;

        public State(State parent) {
            this.parent = parent;
            heuristicValue = 0;
        }
    }

    public static class StateComparatorPriority implements Comparator<State> {
        public int compare(State a, State b) {
            if (a.cost + a.heuristicValue < b.cost + b.heuristicValue)
                return -1;
            else if (a.cost + a.heuristicValue > b.cost + b.heuristicValue)
                return 1;
            return 0;
        }
    }

    public static class StateComparator implements Comparator<State> {
        public int compare(State a, State b) {
            Float x1 = a.x;
            Float x2 = b.x;
            int floatCompare1 = x1.compareTo(x2);

            if (floatCompare1 != 0) {
                return floatCompare1;
            } else {
                Float y1 = a.y;
                Float y2 = b.y;
                return y1.compareTo(y2);
            }
        }
    }

    public class BrandtZachShadow implements IAgent {

        float dx;
        float dy;

        BrandtZachShadow(float destX, float destY) {
            dx = destX;
            dy = destY;
        }

        public void reset() {
        }

        public void update(Model m) {
            for (int i = 0; i < 3; i++) {
                m.setDestination(i, dx, dy);
            }
        }
    }

    public class OpponentShadow implements IAgent {

        int index;

        void avoidBombs(Model m, int i) {
            if (nearestBombTarget(m, m.getX(i), m.getY(i)) <= 2.0f * Model.BLAST_RADIUS * Model.BLAST_RADIUS) {
                float dx = m.getX(i) - m.getBombTargetX(index);
                float dy = m.getY(i) - m.getBombTargetY(index);
                if (dx == 0 && dy == 0)
                    dx = 1.0f;
                m.setDestination(i, m.getX(i) + dx * 10.0f, m.getY(i) + dy * 10.0f);
            }
        }

        float nearestOpponent(Model m, float x, float y) {
            index = -1;
            float dd = Float.MAX_VALUE;
            for (int i = 0; i < m.getSpriteCountOpponent(); i++) {
                if (m.getEnergyOpponent(i) < 0)
                    continue; // don't care about dead opponents
                float d = sq_dist(x, y, m.getXOpponent(i), m.getYOpponent(i));
                if (d < dd) {
                    dd = d;
                    index = i;
                }
            }
            return dd;
        }

        float nearestBombTarget(Model m, float x, float y) {
            index = -1;
            float dd = Float.MAX_VALUE;
            for (int i = 0; i < m.getBombCount(); i++) {
                float d = sq_dist(x, y, m.getBombTargetX(i), m.getBombTargetY(i));
                if (d < dd) {
                    dd = d;
                    index = i;
                }
            }
            return dd;
        }

        @Override
        public void reset() {

        }

        @Override
        public void update(Model m) {
            for (int i = 0; i < m.getSpriteCountSelf(); i++) {
                nearestOpponent(m, m.getX(i), m.getY(i));

                if (this.index >= 0) {
                    if (sq_dist(m.getXOpponent(index), m.getYOpponent(index), m.getX(i), m.getY(i)) >= (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS) * (Model.MAX_THROW_RADIUS + Model.BLAST_RADIUS)) {
                        m.throwBomb(i, m.getXOpponent(index), m.getYOpponent(index));
                    }
                }

                avoidBombs(m, i);
            }

        }
    }

}
