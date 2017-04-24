
/**
 * Created by jisazb1 on 4/24/2017.
 */
public class BrandtZach implements IAgent {

    private int attackerTarget;

    public BrandtZach() {
        attackerTarget = -1;
    }

    public void reset() {

    }

    public void update(Model m) {
        for (int i = 0; i < m.getSpriteCountSelf(); i++) {
            chooseRole(m, i);
        }
    }

    public void chooseRole(Model m, int indexOfSprite) {

        if (!anyEnemiesAlive(m)) {
            beAttacker(m, indexOfSprite);
        }
        beDefender(m, indexOfSprite);

    }

    public boolean anyEnemiesAlive(Model m) {
        for (int i = 0; i < m.getSpriteCountOpponent(); i++) {
            if (!(m.getEnergyOpponent(i) < 0)) {
                return true;
            }
        }
        return false;
    }

    public void beAttacker(Model m, int indexOfSprite) {
        float x = m.getX(indexOfSprite);
        float y = m.getY(indexOfSprite);

        findAttackingTarget(m);

    }

    private void findAttackingTarget(Model m) {

    }
}
