package kandrm.hra.core;

import kandrm.hra.gui.UIAdapter;

import java.util.Collections;
import java.util.List;


/**
 * Manager - reprezentuje hru a ridi ji. Ostatni casti spolu komunikuji pres nej.
 * @author Michal Kandr
 */
public class Manager implements Runnable{
    private Player whitePlayer=null,blackPlayer=null;
    private ComputerPlayer suggestPlayer=null;
    private int whitePlayerLevel,blackPlayerLevel;
    private PlayingArea area;
    private Arbiter arbiter;
    private UIAdapter ad;
    
    public static final int REPLAY_INTERVAL = 2,
            HUMAN_PLAYER = 1,
            COMPUTER_PLAYER = 2,
            WHITE = 1,
            BLACK = 2,
            EMPTY = 0,
            OUT_OF_AREA = -1,
            MOVE_ADD = 0,
            MOVE_DELETE = 1,
            CHANGE_PLAYER = 2,
            GAME_RUNNING = 0,
            GAME_DRAW = 3;
    
    public static volatile boolean STOP = false,
            STOP_MAIN = false,
            RUNNING = false;
    
    /**
     * @param UIAdapter a adapter pro komunikaci s GUI
     */
    public Manager(UIAdapter a) {
        area = new PlayingArea();
        arbiter = new Arbiter();
        ad = a;
    }
    
    /**
     * zastavit bezici ulohy - hraci nebo replay
     */
    public synchronized void killRunning(){
        if(RUNNING){
            STOP = true;
            notifyAll();
            while(RUNNING){
                try {
                    wait();
                } catch (InterruptedException ex) {}
            }
        }
    }
    
    /**
     * pozastavit hru - zastavi bezici ulohy a pozastavi hlavni smycku
     */
    public synchronized void stop(){
        STOP_MAIN = true;
        killRunning();
    }
    
    
    /**
     * nastavi hrace
     * 
     * @param color barva nastavovaneho hrace
     * @param computer zda se ma hrac nastavit na pocitacoveho (jinak se nastavi na lidskeho)
     * @param level uroven na kterou se ma hrac nastavit (pro lidskeho je ignorovano)
     * @param stop zastavit hru a znovu nepoustet
     */
    public synchronized void setPlayer(int color,boolean computer,int level,boolean stop){
        STOP_MAIN = true;
        killRunning();
        Player pl = computer ? new ComputerPlayer(level,area,arbiter,this) : new HumanPlayer(ad,this);
        if(color == WHITE){
            whitePlayer = pl;
            whitePlayerLevel = computer ? level : 0;
        } else if(color == BLACK){
            blackPlayer = pl;
            blackPlayerLevel = computer ? level : 0;
        }
        if(!stop){
            STOP_MAIN = false;
            notifyAll();
        }
    }
    
    /**
     * nastavi hrace
     * 
     * @param color barva nastavovaneho hrace
     * @param computer zda se ma hrac nastavit na pocitacoveho (jinak se nastavi na lidskeho)
     * @param level uroven na kterou se ma hrac nastavit (pro lidskeho je ignorovano)
     */
    public synchronized void setPlayer(int color,boolean computer,int level){
        setPlayer(color,computer,level,true);
    }

    /**
     * zjisti jakeho typu je hrac urcite barvy
     * 
     * @param color barva
     * @return typ hrace
     */
    public synchronized int getPlayerType(int color){
        return (color==WHITE ? whitePlayer : blackPlayer) instanceof ComputerPlayer ? COMPUTER_PLAYER : HUMAN_PLAYER;
    }
    
     /**
     * zjisti uroven pocitacoveho hrace urcite barvy
     * 
     * @param color barva
     * @return uroven hrace
     */
    public synchronized int getPlayerLevel(int color){
        Player p = (color==WHITE ? whitePlayer : blackPlayer);
        return p instanceof ComputerPlayer ? ((ComputerPlayer)p).getLevel() : 0;
    }
      
    /**
     * spusteni hlavni smycky hry
     */
    public void run(){
        List<Integer[][][]> moves;
        int onMove = area.getOnMove(), move, selectedMove;
        while(true){
            synchronized(this){
                if(STOP_MAIN){
                    while(STOP_MAIN){
                        try {
                            wait();
                        } catch (InterruptedException ex) {}
                    }
                    onMove = area.getOnMove();
                }
            }
            moves = Collections.unmodifiableList(arbiter.getAllValidMoves(area));
            if(moves.size()<1){
                continue;
            }
            synchronized(this){
                selectedMove = (onMove==WHITE?whitePlayer:blackPlayer).getMove(moves);
                if(STOP){
                    STOP = false;
                    onMove = area.getOnMove();
                    notifyAll();
                    continue;
                }
            }
            if(selectedMove<0 || selectedMove >= moves.size()){
                continue;
            }
            synchronized(this){
                if(STOP){
                    STOP = false;
                    onMove = area.getOnMove();
                    notifyAll();
                    continue;
                }
            }
            area.move(moves.get(selectedMove));
            onMove = area.getOnMove();
            showActualArea();
            if(arbiter.isGameOver(area)){
                ad.endOfGame(arbiter.getState(area));
                STOP_MAIN = true;
                synchronized(this){
                    while(STOP_MAIN){
                        try {
                            wait();
                        } catch (InterruptedException ex) {}
                    }
                }
            }
        }
    }
    
    /**
     * provedení posunu zpet nebo vpred bez zastaveni behu ostatnich veci
     * @param forward posunout dopredu nebo dozanu
     * @return zda doslo k posunu, nebo jsme na konci/zacatku
     */
    private synchronized boolean fbNS(boolean forward){
        boolean ret = forward ? area.forward() : area.backward();
        if(ret){
            showActualArea();
        }
        return ret;
    }
    
    /**
     * provedení posunu zpet nebo vpred se zastavenim behu hracu
     * @param forward posunout dopredu nebo dozanu
     * @param kill zastavit ostatni bezici procesy
     * @return zda doslo k posunu, nebo jsme na konci/zacatku
     */
    private synchronized boolean fbS(boolean forward,boolean kill){
        if(kill){
            STOP_MAIN = true;
            STOP = true;
            killRunning();
        }
        boolean ret = fbNS(forward);
        if(kill && !ret){
            STOP_MAIN = false;
            STOP = false;
        }
        return ret;
    }

    /**
     * Zahraj znovu vraceny tah
     * @param kill zastavit ostatni bezici procesy
     * @return zda existoval a zahral se vraceny tah
     */
    public synchronized boolean forward(boolean kill){
        return fbS(true,kill);
    }
    /**
     * Zahraj znovu vraceny tah
     * @return zda existoval a zahral se vraceny tah
     */
    public synchronized boolean forward(){
        return forward(true);
    }
    
    /**
     * vrat posledni zahrany tah
     * @param kill zastavit ostatni bezici procesy
     * @return zda bylo co vratit
     */
    public synchronized boolean backward(boolean kill){
        return fbS(false,kill);
    }    
    
    /**
     * vrat posledni zahrany tah
     * @return zda bylo co vratit
     */
    public synchronized boolean backward(){
        return backward(true);
    }
    
    /**
     * overi existenci vraceneho tahu ktery lze znovu zahrat
     */
    public boolean canForward(){
        return area.canForward();
    }
    
    /**
     * overi existenci zahraneho tahu ktery lze vratit
     */
    public boolean canBackward(){
        return area.canBackward();
    }  
    
    /**
     * posune historii hry na zacatek partie - vrati vsechy zahrane tahy
     */
    public synchronized void moveToStart(boolean kill){
        if(kill){
            STOP_MAIN = true;
            STOP = true;
            killRunning();
        }
        area.moveToStart();
        showActualArea();
        if(kill){
            STOP_MAIN = true;
            STOP = false;
        }
    }
    /**
     * posune historii hry na zacatek partie - vrati vsechy zahrane tahy
     */
    public synchronized void moveToStart(){
        moveToStart(true);
    }
    
    /**
     * posune historii na konec - znovu zahraje vsechny vracene tahy
     */
    public synchronized void moveToEnd(){
        STOP_MAIN = true;
        while(area.forward());
        showActualArea();
    }    
    
    /**
     * spusti prehravani historie tahu
     * @param fromStart zda se ma prehravat od zacatku nebo od aktualni pozice v historii
     */
    public void replay(boolean fromStart){
        synchronized(this){
            STOP_MAIN = true;
            killRunning();
            RUNNING = true;
            STOP = false;
        }
        if(fromStart){
            moveToStart(false);
        }
        while(canForward()){
            try {
                Thread.sleep(REPLAY_INTERVAL*1000);
                if(STOP){
                    STOP = false;
                    break;
                }
                forward(false);
            } catch (InterruptedException ex) {}
            if(STOP){
                STOP = false;
                break;
            }
        }
        synchronized(this){
            RUNNING = false;
            notifyAll();
        }
    }
    
    /**
     * vytvori novou hru z jiz existujici - vynuluje aktualnistav do zakladniho nastaveni
     */
    public synchronized void newGame(){
        area.clear();
    }
    
    /**
     * zjisti aktualni stav hry - zda probiha nebo jiz zkoncila a jak zkoncila
     */
    public synchronized int getState(){
        return arbiter.getState(area);
    }
    
    /**
     * napovi lidskemu hraci nejlepsi tah jaky muze provest
     * @param moves povolene tahy
     * @return index nejlepsiho tahu
     */
    public int suggestMove(List<Integer[][][]> moves){
        if(suggestPlayer == null){
            suggestPlayer = new ComputerPlayer(3,area,arbiter,this);
        }
        if(STOP){
            STOP = false;
        }
        int ret = suggestPlayer.getMoveNs(moves);
        if(STOP){
            STOP = false;
            ret = -1;
        }
        return ret;
    }
    
    /**
     * pocet figurek na plose
     * @return [bile;cerne]
     */
    public int[] getCount(){
        return area.getCount();
    }    
    
    /**
     * vrati aktualni podobu hraci plochy jako pole
     */
    public synchronized int[][] getArea(){
        return area.getArea();
    }
    
    /**
     * vrati historii tahu predchazejicich aktualnimu tahu (tahy ktere byly vraceny kolekce neobsahuje)
     * @return kolekce provedenych tahu
     */
    public synchronized List<Integer[][][]> getMoveHistory(){
        return area.getMoveHistory();
    }
    
    /**
     * zobrazi aktualni stav plochy
     */
    private void showActualArea(){
        ad.showArea(area.getArea(),area.getMoveHistory(),area.getCount());
    }
    
    /**
     * nacte stav hry ze souboru
     * @param file nazev souboru
     * @throws java.lang.Exception
     */
    public void load(String file) throws Exception{
        Export.load(file, area, this);
    }
    
     /**
     * ulozi stav hry do souboru
     * @param file nazev souboru
     * @throws java.lang.Exception
     */
     public void save(String file) throws Exception{
        int wp,bp;
        if(whitePlayer instanceof ComputerPlayer){
            wp = COMPUTER_PLAYER;
        } else {
            wp = HUMAN_PLAYER;
        }
        if(blackPlayer instanceof ComputerPlayer){
            bp = COMPUTER_PLAYER;
        } else {
            bp = HUMAN_PLAYER;
        }
        Export.save(file, area, wp,whitePlayerLevel,bp,blackPlayerLevel);
    }
}
