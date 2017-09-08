package kandrm.hra.core;


import java.util.List;

/**
 *
 * minimax - vr�t� hodnocen� stavu na desce zji�t�n� aplikac� algoritmu minimaxu do dan� hloubky
 * @author Michal Kandr
 */
public class MiniMax {
    private Arbiter arbiter;
    
    public MiniMax(Arbiter a){
        arbiter = a;
    }
    
    /**
     * ohodnot� aktualni stav hry na desce
     * 
     * @param area deska ktera se ma ohodnotit
     * @param depth hloubka vypoctu
     * @param rate hodnotici funkce
     * @return hodnoceni stavu
     */
    public int getRate(PlayingArea area,int depth,Rate rate){
        int ret=0,pom=0,cena=Integer.MIN_VALUE;
        if(depth < 1){
            ret = rate.getRate(area,area.getOnMove());
        } else if(arbiter.isGameOver(area)){
            ret=Integer.MIN_VALUE;
        } else {
            List<Integer[][][]> moves = arbiter.getAllValidMoves(area);
            for(Integer[][][] move : moves){
                if(Manager.STOP){
                    return 0;
                }
                pom = -getRate(area.move(move),depth-1,rate);
                area.backward();
                if(pom>cena){
                    cena = pom;
                }
            }
            ret = cena;
        }
        
        return ret;
    }

    
}
