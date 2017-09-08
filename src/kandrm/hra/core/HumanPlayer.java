package kandrm.hra.core;

import static kandrm.hra.core.Manager.*;
import kandrm.hra.gui.UIAdapter;

import java.util.List;

/**
 * Lidsky hrac.
 * @author Michal Kandr
 */
public class HumanPlayer implements Player{
    private UIAdapter ad;
    private Manager manager;
    
    /**
     * @param a adapter pro komunikaci s GUI
     * @param m aktualni instance managera
     */
    public HumanPlayer(UIAdapter a,Manager m){
        ad = a;
        manager = m;
    }
    
    /**
     * vybere tah z kolekce povolenych tahu
     * @param moves povolene tahy
     * @return index vybraneho tahu
     */
    public int getMove(List<Integer[][][]> moves){
        int ret=-1;
        
        synchronized(manager){
            manager.killRunning();
            RUNNING = true;

            ad.wantMove(moves);
            while((ret = ad.getMove()) == -1){
                try {
                    manager.wait();
                } catch (InterruptedException ex) {}
                if(STOP){
                    break;
                }
            }
            ad.dontWantMove();
            RUNNING = false;
        }
        return ret;
    }
}
