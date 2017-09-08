package kandrm.hra.core;

import static kandrm.hra.core.Manager.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reprezentace hracÌ plochy - uchov·v· aktu·lnÌ rozloûenÌ figurek na hracÌ ploöe a poËet figurek obou hr·Ë˘ (optimalizace kv˘li zjiöùov·nÌ konce hry)
 *
 * @author Michal Kandr
 */
public class PlayingArea {
    public static final int[][] AREA_DEFAULT = {
            {OUT_OF_AREA,BLACK,BLACK,BLACK,OUT_OF_AREA},
            {OUT_OF_AREA,BLACK,BLACK,BLACK,OUT_OF_AREA},
            {BLACK,BLACK,BLACK,BLACK,BLACK},
            {BLACK,BLACK,BLACK,BLACK,BLACK},
            {EMPTY,EMPTY,EMPTY,EMPTY,EMPTY},
            {WHITE,WHITE,WHITE,WHITE,WHITE},
            {WHITE,WHITE,WHITE,WHITE,WHITE},
            {OUT_OF_AREA,WHITE,WHITE,WHITE,OUT_OF_AREA},
            {OUT_OF_AREA,WHITE,WHITE,WHITE,OUT_OF_AREA}
        };
    
    private int[][] area = new int[9][5];
    private int white=16,
            black=16,
            lastMove=-1,
            onMove=BLACK;
    private ArrayList <Integer[][][]> moveHistory = new ArrayList<Integer[][][]>();
    
    public void PlayingArea(){
        init();
    }
    
    /**
     * inicializuje rozlozeni kamenu do vychoziho stavu (stav na zacatku hry)
     */
    private void init(){
        for(int i=0;i<AREA_DEFAULT.length;++i){
            for(int j=0;j<AREA_DEFAULT[i].length;++j){
                area[i][j] = AREA_DEFAULT[i][j];
            }
        }        
    }
    
    /**
     * resetuje vsechna nastaveni plochy do vychoziho stavu ale nevymaze obsah historie
     */
    public void moveToStart(){
        init();
        white=16;
        black=16;
        lastMove=-1;
        onMove=BLACK;
    }
   
    /**
     * resetuje vsechna nastaveni plochy do vychoziho stavu
     */
    public void clear(){
        moveToStart();
        moveHistory.clear();
    }
    

    /**
     * pocet figurek na plose
     * @return [bile;cerne]
     */
    public int[] getCount(){
        int ret[] = {white,black};
        return ret;
    }
    
    /**
     * plocha jako pole
     * 
     */
    public int[][] getArea(){
        return area;
    }
    
    /**
     * historie tahu do posledniho zahraneho (aktualniho)
     */
    public List<Integer[][][]> getMoveHistory(){
        return Collections.unmodifiableList(moveHistory.subList(0, lastMove+1));
    }
    
    /**
     * kompletni historie tahu vcetne tahu vracenych
     */
    public List <Integer[][][]> getHistory(){
        return Collections.unmodifiableList(moveHistory);
    }
    
    /**
     * ktery hra je na tahu
     */
    public int getOnMove(){
        return onMove;
    }
    
    /**
     * poradi posledniho zahraneho (aktualniho) tahu v historii
     */
    public int getLastMove(){
        return lastMove;
    }
    
    /**
     * smaze vracene tahy (tahy za aktualnim)
     */
    public void clearNextMoves(){
         if(lastMove < moveHistory.size()-1){
             for(int i=moveHistory.size()-1;i>lastMove;--i){
                 moveHistory.remove(i);
             }
         }
    }
    
    /**
     * prida tah do historie
     * @param move tah htery se ma pridat
     */
    private void historyAddMove(Integer[][][] move){
         clearNextMoves();
         moveHistory.add(move);
         ++lastMove;
    }

    /**
     * provede tah
     * @param move tah ktery se ma provest
     * @param save ulozit tah do historie
     * @return deska po provedeni tahu
     */
    public PlayingArea move(Integer[][][] move,boolean save){
         for(int i=0;i<move.length;++i){
             switch(move[i][0][0]){
                 case MOVE_ADD:
                     addPiece(move[i][1],move[i][0][1]);
                     break;
                 case MOVE_DELETE:
                     deletePiece(move[i][1]);
                     break;
                 case CHANGE_PLAYER:
                     setOnMove(move[i][0][1]==WHITE?WHITE:BLACK);
                     break;
             }
         }
         if(save){
            historyAddMove(move);
         }
         return this;
     }
    /**
     * provede tah a ulozi ho do historie tahu
     * @param move tah ktery se ma provest
     * @return deska po provedeni tahu
     */
    public PlayingArea move(Integer[][][] move){
        return move(move,true);
    }
    
    /**
     * provede opacny tah k predanemu tahu
     * @param move tah jehoz opak se ma provest
     * @return deska po provedeni tahu
     */
    private PlayingArea reverseMove(Integer[][][] move){
         for(int i=move.length-1;i>=0;--i){
             switch(move[i][0][0]){
                 case MOVE_ADD:
                     deletePiece(move[i][1]);
                     break;
                 case MOVE_DELETE:
                     addPiece(move[i][1],move[i][0][1]);
                     break;
                 case CHANGE_PLAYER:
                     setOnMove(move[i][0][1]==WHITE?BLACK:WHITE);
                     break;
             }
         }
         return this;
     }

    
    /**
     * overi existenci vraceneho tahu ktery lze znovu zahrat
     */
    public boolean canForward(){
        return lastMove < moveHistory.size()-1 && moveHistory.size() > 0;
    }
    /**
     * Zahraj znovu vraceny tah
     * @return zda existoval a zahral se vraceny tah
     */
    public boolean forward(){
        if(canForward()){
            move(moveHistory.get(++lastMove),false);
            return true;
        }
        return false;
    }
    
    /**
     * overi existenci zahraneho tahu ktery lze vratit
     */
    public boolean canBackward(){
        return lastMove > -1;
    }  
    /**
     * vrat posledni zahrany tah
     * @return zda bylo co vratit
     */
    public boolean backward(){
        if(canBackward()){
            reverseMove(moveHistory.get(lastMove--));
            return true;
        }
        return false;
    }    

    
    /**
     * prida do desky kamen dane barvy na danou pozici
     * @param where souradnice [x;y]
     * @param color barva kamene ktery se ma vlozit
     * @return deska po pridani kamene
     */
    private PlayingArea addPiece(Integer[] where,int color){
        if(color==WHITE){
            white++;
        } else {
            black++;
        }
        area[where[0].intValue()][where[1].intValue()] = color;
        return this;
    }

    /**
     * odstrani z desky kamen na danych douradnicich
     * @param where souradnice [x;y]
     * @return deska po odebrani kamene
     */
    private PlayingArea deletePiece(Integer[] where){
        if(area[where[0].intValue()][where[1].intValue()]==WHITE){
            white--;
        } else if(area[where[0].intValue()][where[1].intValue()]==BLACK){
            black--;
        }
        area[where[0].intValue()][where[1].intValue()] = EMPTY;
        return this;
    }

    /**
     * nastavi hrace ktery je na tahu
     * @param who barva hrace na tahu
     */
    public void setOnMove(int who){
        if(who != BLACK && who != WHITE){
            throw new IllegalArgumentException();
        }
        onMove = who;
    }

}
