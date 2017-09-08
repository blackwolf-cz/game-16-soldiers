package kandrm.hra.core;

import java.util.ArrayList;
import java.util.List;

import static kandrm.hra.core.Manager.*;

/**
 * Ohodnocovaci funkce
 * @author Michal Kandr
 */
public class Rate{

    /**
     * ohodnoti aktualni stav na desce
     * @param area aktualni plocha
     * @param color barva hrace z jehoz pohledu se ma stav hodnotit
     * @return hodnoceni stavu
     */
    public int getRate(PlayingArea area, int color) {
        int i,j;
        int[][] areal = area.getArea();
        int[] pieces = area.getCount();
        double vzd = 0,
                min,tmp,
                rate = 10.0,
                piecesMy = (double)pieces[(color==WHITE?0:1)],
                piecesHis = (double)pieces[(color==WHITE?1:0)];
        List<Integer[]> whitePos = new ArrayList<Integer[]>(),
                blackPos = new ArrayList<Integer[]>();
        
        //rozdil mezi mymi figurkami a souperovymi
        rate = (piecesMy-piecesHis)/16.0+1.0;
        //cim mene figurek celkem tim lepe - trosku vycistime plochu;)
        rate *= 2.0-piecesHis/16.0;
        
        //priblizujeme se k souperi:)
        for(i=0;i<9;++i){
            for(j=0;j<5;++j){
                if(areal[i][j]==WHITE){
                    Integer[] p = {i,j};
                    whitePos.add(p);
                } else if(areal[i][j]==BLACK){
                    Integer[] p = {i,j};
                    blackPos.add(p);
                }
            }
        }
        for(Integer[] p1 : whitePos){
            min = Integer.MAX_VALUE;
            for(Integer[] p2 : blackPos){
                tmp = Math.sqrt(Math.pow(p1[0]-p2[0],2) + Math.pow(p1[1]-p2[1],2));
                if(tmp < min){
                    min = tmp;
                }
            }
            vzd += min;
        }
        
        rate *= (10.0-vzd/(double)whitePos.size())/5.0+1.0;
        
        if(piecesMy==1 || piecesHis==1){
            rate *= (piecesMy-piecesHis)/8.0+1.0;
            rate *= (10.0-vzd/(double)whitePos.size())/2.0;
        }
        
        return (int)(rate*100);
    }

}
