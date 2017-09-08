package kandrm.hra.core;

import static kandrm.hra.core.Manager.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Rozhodnèí - generuje povolene tahy a zjistuje stav hry.
 * 
 * @author Michal Kandr
 */
public class Arbiter {
    private static final int[][][][] VALID_MOVES = {
        {
            {{EMPTY}},
            {{1,1},{0,2}},
            {{0,1},{0,3},{1,2}}},
        {
            {{EMPTY}},
            {{0,1},{1,2},{2,2}},
            {{0,2},{1,1},{1,3},{2,2}}},
        {
            {{2,1},{3,0},{3,1}},
            {{2,0},{2,2},{3,1}},
            {{1,1},{1,2},{1,3},{2,1},{2,3},{3,1},{3,2},{3,3}}},
        {
            {{2,0},{3,1},{4,0}},
            {{2,0},{2,1},{2,2},{3,0},{3,2},{4,0},{4,1},{4,2}},
            {{2,2},{3,1},{3,3},{4,2}}},
        {
            {{3,0},{3,1},{4,1},{5,0},{5,1}},
            {{3,1},{4,0},{4,2},{5,1}},
            {{3,1},{3,2},{3,3},{4,1},{4,3},{5,1},{5,2},{5,3}}}
    };
    
     
     
     /**
      * najde povolene tahy pro kamen na specifikovane pozici
      * @param area aktualni hraci plocha
      * @param fromX x-souradnice pozice
      * @param fromY y-souradnice pozice
      * @param onlyJumps zda se maji nalezt pouze preskoky souperova kamene
      * @param secondInTri zda se hledaji tahy na 2. pozici v trojuhelnicich
      * @return povolene tahy
      */
     private List<Integer[][][]> getValidMoves(PlayingArea area,int fromX,int fromY,boolean onlyJumps,boolean secondInTri){
         int[][] areal = area.getArea();
         List<Integer[][][]> ret = new ArrayList<Integer[][][]>();
         int[] tempMove=new int[2];
         int aktFromX = fromX>4?8-fromX:fromX,
                 aktFromY = fromY>2?4-fromY:fromY,
                 i,
                 color = area.getOnMove(),
                 opositColor = color==WHITE?BLACK:WHITE;
         for(i=0;i<VALID_MOVES[aktFromX][aktFromY].length;++i){
             if(fromX>4){
                 tempMove[0] = 8-VALID_MOVES[aktFromX][aktFromY][i][0];
             } else {
                 tempMove[0] = VALID_MOVES[aktFromX][aktFromY][i][0];
             }
             if(fromY>2){
                 tempMove[1] = 4-VALID_MOVES[aktFromX][aktFromY][i][1];
             } else {
                 tempMove[1] = VALID_MOVES[aktFromX][aktFromY][i][1];
             }
             
             if(areal[tempMove[0]][tempMove[1]] == EMPTY && !onlyJumps){
                 Integer[][][] move = {
                     {{MOVE_DELETE,color},{fromX,fromY}},
                     {{MOVE_ADD,color},{tempMove[0],tempMove[1]}},
                     {{CHANGE_PLAYER,opositColor},{}}};
                 ret.add(move);
             } else if(!secondInTri && areal[tempMove[0]][tempMove[1]] == opositColor){
                 int endX = fromX==tempMove[0]?fromX:(fromX>tempMove[0]?tempMove[0]-1:tempMove[0]+1);
                 int endY = fromY==tempMove[1]?fromY:(fromY>tempMove[1]?tempMove[1]-1:tempMove[1]+1);
                 if((fromX==0 || fromX==8) && (fromY==1 || fromY==3) && (endY==1 || endY==3) && (endX==2 || endX==6)){
                     endY = 2;
                 } else if(fromY==2 && (fromX==2 || fromX==6) && (endX==0 || endX==8) && (endY==0 || endY==4)){
                     endY = endY==0 ? 1 : 3;
                 }
                 if(endX>=0 && endX<=8 && endY>=0 && endY <=4 && areal[endX][endY] == EMPTY && 
                         (fromX!=3 && fromX!=5 || endX!=1 && endX!=7 || fromY!=endY || fromY!=1 && fromY!=3)){
                     Integer[][][] move = {
                         {{MOVE_DELETE,color},{fromX,fromY}},
                         {{MOVE_DELETE,opositColor},{tempMove[0],tempMove[1]}},
                         {{MOVE_ADD,color},{endX,endY}},
                         {{CHANGE_PLAYER,opositColor},{}}};
                     ret.add(move);
                     ret.addAll(getJumps(area,move));
                 }
             }
         }
         if(!secondInTri && (fromX<2 || fromX>6 || ((fromX==2 || fromX==6) && fromY==2))){
             //doplneni skoku o dva pro figurku v trojuhelnicich
             ArrayList<Integer[][][]> ret2 = new ArrayList<Integer[][][]>();
             boolean add;
             for(Integer[][][] move : ret){
                 if(move.length == 3){
                     for(Object nm : getValidMoves(area,move[1][1][0],move[1][1][1],false,true)){
                         Integer[][][] nmi = ((Integer[][][])nm);
                         Integer[][][] nmove = new Integer[3][2][2];
                         nmove[0] = move[0];
                         nmove[1] = nmi[nmi.length-2];
                         nmove[2] = move[2];
                         add = true;
                         for(Integer[][][] a : ret2){
                             if(a[a.length-2][1][0] == nmove[1][1][0] && a[a.length-2][1][1] == nmove[1][1][1]){
                                add = false;
                                break;
                             }
                         }
                         if(add){
                            ret2.add(nmove);
                         }
                     }
                 }
             }
             ret.addAll(ret2);
         }
         
         return ret;
     }
     /**
      * najde povolene tahy pro kamen na specifikovane pozici
      * @param area aktualni hraci plocha
      * @param fromX x-souradnice pozice
      * @param fromY y-souradnice pozice
      * @return povolene tahy
      */
     public List<Integer[][][]> getValidMoves(PlayingArea area,int fromX,int fromY){
         int[][] areal = area.getArea();
         return areal[fromX][fromY]!=area.getOnMove()?null:getValidMoves(area,fromX,fromY,false,false);
     }
     
     /**
      * najde mozna prodlouzeni o dalsi preskoky, ktere je mozne udelat z pozice na ktere je kamen po preskoku souperova kamene
      * @param area aktualni hraci plocha
      * @param move predchozi tah ktery byl preskokem a pro ktery se maji hledat mozna prodlouzeni o dalsi preskoky
      * @return tahy ktere jsou prodlouzenim aktualniho
      */
     private List<Integer[][][]> getJumps(PlayingArea area,Integer[][][] move){
         List<Integer[][][]> ret = new ArrayList<Integer[][][]>();
         int i;
         Integer[][][] tempMove = new Integer[move.length-1][2][2];
         System.arraycopy(move,0,tempMove,0,move.length-1);
         
         List<Integer[][][]> jumps = getValidMoves(area.move(tempMove),move[move.length-2][1][0],move[move.length-2][1][1],true,false);
         area.backward();
         
         for(Integer[][][] jump : jumps){
             Integer[][][] nmove = new Integer[jump.length+tempMove.length][2][2];
             for(i=0;i<tempMove.length;++i){
                 nmove[i] = tempMove[i];
             }
             for(i=0;i<jump.length;++i){
                 nmove[i+tempMove.length] = jump[i];
             }
             ret.add(nmove);
         }
         
         return ret;
     }
     
     
     
     /**
      * najde vsechny povolene tahy pro vsechny figurky hrace ktery je momentalne na tahu
      * @param area aktualni hraci plocha
      * @return povolene tahy
      */
     public List<Integer[][][]> getAllValidMoves(PlayingArea area){
         int[][] areal = area.getArea();
         List<Integer[][][]> ret = new ArrayList<Integer[][][]>();
         int color = area.getOnMove(),i,j;
                  
         for(i=0;i<9;++i){
             for(j=0;j<5;++j){
                 if(areal[i][j]==color){
                     List<Integer[][][]> vm = getValidMoves(area,i,j);
                     if(vm != null){
                        ret.addAll(vm);
                     }
                 }
             }
         }
         area.clearNextMoves();
         return ret;
     }
     
     
    
    /**
     * ovìøí zda je konec hry (vyhrál bílý/èerný/remíza)
     * @param area hraci plocha
     */
    public boolean isGameOver(PlayingArea area){
        int[] count = area.getCount();
        return count[0] == 0 || count[1] == 0 || (count[0] == 1 && count[1] == 1);
    }
    
    /**
     * aktualni stav hry
     * @return hodnota konstanty odpovidajici aktualnimu stavu hry
     */
    public int getState(PlayingArea area){
        int[] count = area.getCount();
        if(count[0] == 0){
            return BLACK;
        } else if(count[1] == 0){
            return WHITE;
        } else if(count[0] == 1 && count[1] == 1){
            return GAME_DRAW;
        }
        return GAME_RUNNING;
    }
    
    
}
