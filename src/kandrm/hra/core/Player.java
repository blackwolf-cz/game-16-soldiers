package kandrm.hra.core;

import java.util.List;

/**
 * Interface hracu.
 * @author Michal Kandr
 */
public interface Player {
    /**
     * Vybere ktery tah z povolenych tahu chce zahrat.
     * @param moves povolene tahy
     * @return index vybraneho tahu
     */
    public int getMove(List<Integer[][][]> moves);
}
