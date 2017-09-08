package kandrm.hra.gui;

import kandrm.hra.core.Manager;
import static kandrm.hra.core.Manager.*;

import java.util.List;

/**
 * Zprostredkovava komunikaci mezi jadrem hry a uzivatelskym rozhranim.
 * @author Michal Kandr
 */
public class UIAdapter {
    private Gui gui = null;
    private Manager game = null;
    private Thread tGame = null;
    
    private boolean replayRunning = false;
    
    public static final int HUMAN_PLAYER = Manager.HUMAN_PLAYER,
            COMPUTER_PLAYER = Manager.COMPUTER_PLAYER,
            MOVE_ADD = Manager.MOVE_ADD,
            WHITE = Manager.WHITE,
            BLACK = Manager.BLACK,
            GAME_DRAW = Manager.GAME_DRAW;
    
    
    /**
     * @param g graficke rozhrani
     */
    public UIAdapter(Gui g){
        gui = g;
    }
    
    /**
     * inicializuje hru - vytvori novou pokud neni jeste vytvorena nebo vynuluje stavajici
     */
    private void init(){
        if(tGame == null){
            game = new Manager(this);
            tGame = new Thread(game);
        }
        game.newGame();
    }
        
    /**
     * Vytvori novou hru
     * @param whiteP typ bileho hrace - pocitac nebo clovek
     * @param whiteLevel uroven bileho hrace
     * @param blackP  typ cerneho hrace - pocitac nebo clovek
     * @param blackLevel uroven cerneho hrace
     */
    public void newGame(int whiteP,int whiteLevel,int blackP,int blackLevel) {                         
        init();
        
        setPlayer(WHITE,whiteP==COMPUTER_PLAYER, whiteLevel,true);
        setPlayer(BLACK,blackP==COMPUTER_PLAYER, blackLevel,true);
        
        showArea();
    }
    
    /**
     * nastavi hrace
     * 
     * @param color barva nastavovaneho hrace
     * @param computer zda se ma hrac nastavit na pocitacoveho (jinak se nastavi na lidskeho)
     * @param level uroven na kterou se ma hrac nastavit (pro lidskeho je ignorovano)
     * @param stop zastavit hru a znovu nepoustet
     */
    public void setPlayer(int color,boolean computer,int level,boolean stop){
        if(game != null){
            game.setPlayer(color, computer, level, stop);
        }
    }

    /**
     * nastavi hrace
     * 
     * @param color barva nastavovaneho hrace
     * @param computer zda se ma hrac nastavit na pocitacoveho (jinak se nastavi na lidskeho)
     * @param level uroven na kterou se ma hrac nastavit (pro lidskeho je ignorovano)
     */
    public void setPlayer(int color,boolean computer,int level){
        setPlayer(color,computer,level,false);
    }
    
    /**
     * zjisti jakeho typu je hrac urcite barvy
     * 
     * @param color barva
     * @return typ hrace
     */
    public int getPlayerType(int color){
        return game.getPlayerType(color);
    }
    
     /**
     * zjisti uroven pocitacoveho hrace urcite barvy
     * 
     * @param color barva
     * @return uroven hrace
     */
    public int getPlayerLevel(int color){
        return game.getPlayerLevel(color);
    }
    
    /**
     * znovu spusti hru pokud jepozastavena, nebo vytvori novou hru nebyla-li jiste zadna vytvorena
     * @return zda se podarilo hru spustit
     */
    public boolean start(){
        if(tGame == null){
            return false;
        }
        if(!tGame.isAlive()){
            tGame.start();
            return true;
        } else if(replayRunning && RUNNING){
            synchronized(game){
                STOP = true;
                game.notifyAll();
                replayRunning = false;
            }
            return true;
        } else if(STOP_MAIN){
            synchronized(game){
                STOP = false;
                STOP_MAIN = false;
                game.notifyAll();
            }
            return true;
        }

        return false;
    }

    /**
     * pozastavit hru - zastavi bezici ulohy a pozastavi hlavni smycku
     */
    public void stop(){
        if(game != null){
            game.stop();
        }
    }
    
    /**
     * Zahraj znovu vraceny tah
     * @return zda existoval a zahral se vraceny tah
     */
    public boolean forward(){
        return game.forward();
    }
    
    /**
     * vrat posledni zahrany tah
     * @return zda bylo co vratit
     */
    public boolean backward(){
        return game.backward();
    }

    /**
     * overi existenci vraceneho tahu ktery lze znovu zahrat
     */
    public boolean canForward(){
        return game.canForward();
    }
    
    /**
     * overi existenci zahraneho tahu ktery lze vratit
     */
    public boolean canBackward(){
        return game.canBackward();
    }  
    
    /**
     * posune historii hry na zacatek partie - vrati vsechy zahrane tahy
     */
    public void moveToStart(){
        game.moveToStart();
    }
    
    /**
     * posune historii na konec - znovu zahraje vsechny vracene tahy
     */
    public void moveToEnd(){
        game.moveToEnd();
    }    
    
    /**
     * spusti prehravani historie tahu
     * @param fromStart zda se ma prehravat od zacatku nebo od aktualni pozice v historii
     */
    public void replay(final boolean fromStart){
        new Thread(new Runnable(){
            public void run(){ game.replay(fromStart); }
        }).start();
        replayRunning = true;
    }
    
    /**
     * lidsky hrac zada o tah
     * @param moves povolene tahy
     */
    public void wantMove(List<Integer[][][]> moves){
        gui.wantMove(moves);
    }
    
    /**
     * LIdsky hrac nechce tah - bud zahral vybrany tah, nebo byl jeho beh prerusen
     */
    public void dontWantMove(){
        gui.dontWantMove();
    }
    
    /**
     * vrati cislo zivatelem vybraneho tahu
     * @return cislo tahu nebo -1 nebyl-li zadny vybran
     */
    public int getMove(){
        return gui.getMove();
    }
    
    /**
     * oznami cekajicim hracum ze se neco deje
     */
    public void notifyPlayer(){
        synchronized(game){
            game.notifyAll();
        }
    }
       
    /**
     * oznami uzivatelkemu rozhrani ze uz je konec hry
     * @param status stav hry - vyhral bily, vyhral cerny nebo remiza
     */
    public void endOfGame(int status){
        gui.endOfGame(status);
    }
    
    /**
     * napovi uzivateli nejlepsi tah
     * @param moves tahy ze kterych se ma vybirat
     * @return cislo nejlepsiho tahu
     */
    public int suggestMove(List<Integer[][][]> moves){
        return game.suggestMove(moves);
    }
    
    /**
     * nacte hru ze souboru
     * @param file adresa souboru
     * @throws java.lang.Exception
     */
    public void load(String file) throws Exception{
        init();
        game.load(file);
        showArea();
    } 
    
    /**
     * ulozi hru do souboru
     * @param file adresa souboru
     * @throws java.lang.Exception
     */
    public void save(String file) throws Exception{
        game.save(file);
    }
    
    /**
     * pocet figurek na plose
     * @return [bile;cerne]
     */
    public int[] getCount(){
        return game.getCount();
    }
    
    /**
     * vrati aktualni stav desky jako pole
     */
    public int[][] getArea(){
        return game.getArea();
    }
    
    /**
     * zobrazi predanou hraci plochu a historii tahu
     * @param area hraci plocha
     * @param moveHistory historie tahu
     * @param count pocet figurek na desce [bile;cerne]
     */
    public void showArea(int[][] area, List<Integer[][][]> moveHistory,int[] count){
        gui.paintArea(area);
        gui.paintMoveHistory(moveHistory);
        gui.paintPiecesCount(count);
    }
    
    /**
     * zobrazi plochu a historii aktualniho stavu hry
     */
    public void showArea(){
        if(game != null){
            showArea(getArea(),game.getMoveHistory(),game.getCount());
        }
    }
    
}
