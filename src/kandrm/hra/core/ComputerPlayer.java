package kandrm.hra.core;

import static kandrm.hra.core.Manager.*;

import java.util.List;

/**
 * Implementace pocitacoveho hrace.
 * 
 * @author Michal Kandr
 */
public class ComputerPlayer implements Player{
    private int level;
    private Rate rate;
    private PlayingArea area;
    private Arbiter arbiter;
    private MiniMax miniMax;
    private Manager manager;
    
    /**
     * @param l uroven hrace
     * @param ar aktualni hraci plocha
     * @param arb rozhodci
     * @param m manager
     */
    public ComputerPlayer(int l,PlayingArea ar,Arbiter arb,Manager m) {
        level = l;
        rate = new Rate();
        area = ar;
        arbiter = arb;
        miniMax = new MiniMax(arbiter);
        manager = m;
    }
    
    /**
     * zjisti zda jsou dva tahy opacne
     * @param m1 jeden tah
     * @param m2 druhy tah
     */
    private boolean compareMoves(Integer[][][] m1,Integer[][][] m2){
        return m1.length==3 && m2.length==3
                && m1[0][0][0]==m2[0][0][0] && m1[0][0][1]==m2[0][0][1]
                && m1[0][1][0]==m2[1][1][0] && m1[0][1][1]==m2[1][1][1]
                
                && m1[1][0][0]==m2[1][0][0] && m1[1][0][1]==m2[1][0][1]
                && m1[1][1][0]==m2[0][1][0] && m1[1][1][1]==m2[0][1][1]
                
                && m1[2][0][0]==m2[2][0][0] && m1[2][0][1]==m2[2][0][1];
    }
    
    /**
     * vybere tah bez synchronizace s hraci v jinych vlaknech
     * @param moves povolene tahy
     * @return index vybraneho tahu
     */
    public int getMoveNs(List<Integer[][][]> moves){
        int pom=0,cena=Integer.MIN_VALUE,ret=0,i=0;
        List<Integer[][][]> movesHistory = area.getMoveHistory();
        Integer[][][] prevMove = movesHistory.size()>=2 ? movesHistory.get(movesHistory.size()-2) : null;
        
        for(Integer[][][] move : moves){
            if(STOP){
                break;
            }
            pom = -miniMax.getRate(area.move(move),level-1,rate);
            area.backward();
            //opakovane tahy delat zrovna nemusime
            if(prevMove != null && compareMoves(prevMove,move)){
                if(pom > 0){
                    pom /= 1.5;
                } else {
                    pom *= 1.5;
                }
            }
            if(pom == -Integer.MIN_VALUE){
                //vyhral jsem, huraaa
                ret = i;
                break;
            } else if(pom>cena || (pom==cena && Math.random() > 0.5)){
                cena = pom;
                ret = i;
            }
            ++i;
        }
         
        area.clearNextMoves();
        
        return ret;
    }
    
    /**
     * vybere tah z kolekce povolenych tahu
     * @param moves povolene tahy
     * @return index vybraneho tahu
     */
    public int getMove(List<Integer[][][]> moves){
        int ret=0;
        synchronized(manager){
            manager.killRunning();
            RUNNING = true;
            ret = getMoveNs(moves);
            RUNNING = false;
        }
        return ret;
    }
    
    
    public int getLevel(){
        return level;
    }
}
