package kandrm.hra.gui;

import static kandrm.hra.gui.UIAdapter.*;
import static kandrm.hra.gui.Piece.*;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import javax.help.*;



/**
 * Graficke rozhrani komunikujici s uzivatelem.
 * @author  Michal Kandr
 */
public class Gui extends javax.swing.JFrame {

    private UIAdapter ad = null;
    
    private static final int MAX_COMPUTER_LEVEL = 4;

    private int whitePlayer=COMPUTER_PLAYER,blackPlayer=HUMAN_PLAYER,
            whitePlayerLevel=1,blackPlayerLevel=0,
            DOUBLE_CLICK_INTERVAL = 400,
            
            moveColor = 0,
            moveId = -1,
            oldMoveId = -1;

    private boolean wantMove = false,
            moveFinished = false,
            replay = false,
            end = false,
            first = true;
    private List<Integer[][][]> moves = null;
    private List<List<Integer[][]>> allowedMoves = null;
    private List<Integer[][]> move = null;
    
    private long lastClick;
    
    private HelpSet hs;
    private HelpBroker hb;
    
    
    private class XMLFileFilter extends FileFilter{
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().endsWith("xml");
        }

        @Override
        public String getDescription() {
            return "XML soubory";
        }
    }

    /** Creates new form Gui */
    public Gui() {
        initComponents();
        ad = new UIAdapter(this);
        areaGA = (GraphicArea)area;
        
        ClassLoader cl = this.getClass().getClassLoader();
        try {
            hs = new HelpSet(cl, HelpSet.findHelpSet(cl, "HraHelp.hs"));
            hb = hs.createHelpBroker();
        } catch (Exception ee) {
            return;
        }
    }
    
    
    
    
    public void wantMove(List<Integer[][][]> moves){
        if(moves.size() < 1){
            return;
        }
        wantMove = true;
        move = new ArrayList<Integer[][]>();
        moveId = -1;
        this.moves = moves;
        allowedMoves = new ArrayList<List<Integer[][]>>();
        
        ArrayList<Integer[][]> ma;
        moveColor = moves.get(0)[0][0][1];
        for(Integer[][][] m : moves){
            ma = new ArrayList<Integer[][]>();
            for(int i=0;i<m.length;++i){
                if(m[i][0][1] == moveColor && (i==0 || m[i-1][0][1] != moveColor || m[i-1][0][0] != MOVE_ADD)){
                    ma.add(m[i]);
                }
            }
            allowedMoves.add(ma);
        }
        menuHelpMove.setEnabled(true);
        showStatus(moveColor==BLACK?"moveBlack":"moveWhite");
    }

    public void dontWantMove(){
        wantMove = false;
        move = null;
        moveId = -1;
        allowedMoves = null;
        menuHelpMove.setEnabled(false);
        if(replay){
            showStatus("replay");
        } else {
            showStatus();
        }
    }
    
    public int getMove(){
        return moveFinished?moveId:-1;
    }
    
    public void endOfGame(int status){
        String who = null;
        switch(status){
            case WHITE:
                if(whitePlayer==HUMAN_PLAYER && blackPlayer==COMPUTER_PLAYER){
                    who = "Vyhrál jste (bílý).";
                } else if(whitePlayer==COMPUTER_PLAYER && blackPlayer==HUMAN_PLAYER){
                    who = "Prohrál jste, vyhrál bílý.";
                } else {
                    who = "Vyhrál bílý.";
                }
                break;
            case BLACK:
                if(blackPlayer==HUMAN_PLAYER && whitePlayer==COMPUTER_PLAYER){
                    who = "Vyhrál jste (èerný).";
                } else if(blackPlayer==COMPUTER_PLAYER && whitePlayer==HUMAN_PLAYER){
                    who = "Prohrál jste, vyhrál èerný.";
                } else {
                    who = "Vyhrál èerný.";
                }
                break;
            case GAME_DRAW:
                who = "Remíza.";
                break;
        }
        if(who != null){
            end = true;
            showStatus("Konec hry: "+who);
            JOptionPane.showMessageDialog(this, who, "Konec hry", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    public void paintArea(int[][] ar){
        areaGA.changeArea(ar);
        showHideHistory();
        if(replay && !ad.canForward()){
            replay = false;
            showStatus("replayEnded");
        }
    }
    
    public void paintMoveHistory(List<Integer[][][]> moves){
        int i=0,j,color;

        moveHistory.setText("");
        try{
            for(Integer[][][] m : moves){
                if(m.length > 2){
                    StringBuilder str = new StringBuilder();
                    color = m[0][0][1];
                    str.append(++i+") "+(color==WHITE?"bílý":"èerný")+": ");
                    for(j=0;j<m.length;++j){
                        if(m[j][0][1] == color && (j==0 || m[j-1][0][1] != color || m[j-1][0][0] != MOVE_ADD)){
                            if(j!=0){
                                str.append(" - ");
                            }
                            str.append((char)('a'+m[j][1][0]));
                            str.append(m[j][1][1]+1);
                        }
                    }
                    moveHistory.append(str.toString()+"\n");
                }
            }
            moveHistory.setCaretPosition(moveHistory.getText().length());
        } catch(ConcurrentModificationException e){
            moveHistory.setText("");
        }
    }
    
    public void paintPiecesCount(int[] count){
        piecesCount.setText(count[0]+"  bíl"+(count[0]==1?"á":(count[0]>1&&count[0]<5?"é":"ých"))+
                ";    "+count[1]+"  èern"+(count[1]==1?"á":(count[1]>1&&count[1]<5?"é":"ých")));
    }
    
    private void showHideHistory(){
        boolean cB = ad.canBackward(),
                cF = ad.canForward();
        menuHistoryBack.setEnabled(cB);
        menuHistoryNext.setEnabled(cF);
        menuHistoryBegin.setEnabled(cB);
        menuHistoryEnd.setEnabled(cF);
        menuHistoryReplayStart.setEnabled(cB||cF);
        menuHistoryReplayAkt.setEnabled(cF);
    }
    
    private void paintMove(){
        for(Integer[][] m : move){
             areaGA.setPieceColor(m[1][0], m[1][1], Piece.RED);
        }
    }
    
    private void showStatus(String what){
        if(wantMove && !what.equals("moveWhite") && !what.equals("moveBlack")){
            return;
        }
        String text = "";
        if(what.equals("stopped")){
            text = "Hra je pozastavena. Pro její spuštìní (v aktuálním stavu) kliknìte na hrací plochu.";
        }else if(what.equals("replay")){
            text = "Probíhá pøehrávání historie tahù. Pøehrávání zastavíte kliknutím na hrací plochu.";
        }else if(what.equals("replayStopped")){
            text = "Pøehrávání historie tahù bylo zastaveno. Pro spuštìní hry z aktuálního stavu kliknìte znovu na plochu";
        }else if(what.equals("replayEnded")){
            text = "Pøehrávání historie tahù bylo dokonèeno. Pro pokraèování ve høe kliknìte na hrací plochu.";
        } else if(what.equals("moveWhite") || what.equals("moveBlack")){
            text = "Táhne "+(what.equals("moveBlack")?"èerný":"bílý")+". Zvolte tah který chcete provést.";
        } else if(what.equals("computers")){
            text = "Hra dvou poèítaèù";
        } else if(!what.equals("clear")){
            text = what;
        }
        statusBar.setText(text);
    }
    private void showStatus(){
        showStatus("clear");
    }
    
    private void showPlayersSetting(){
        whitePlayerHuman.setSelected(whitePlayer == HUMAN_PLAYER);
        whitePlayerComputer.setSelected(whitePlayer == COMPUTER_PLAYER);
        whitePlayerComputerLevel.setEnabled(whitePlayer == COMPUTER_PLAYER);
        whitePlayerComputerLevel.setValue((Object)(whitePlayerLevel>0?whitePlayerLevel:1));
        
        blackPlayerHuman.setSelected(blackPlayer == HUMAN_PLAYER);
        blackPlayerComputer.setSelected(blackPlayer == COMPUTER_PLAYER);
        blackPlayerComputerLevel.setEnabled(blackPlayer == COMPUTER_PLAYER);
        blackPlayerComputerLevel.setValue((Object)(blackPlayerLevel>0?blackPlayerLevel:1));        
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        groupPlayersWhite = new javax.swing.ButtonGroup();
        groupPlayersBlack = new javax.swing.ButtonGroup();
        playersSettings = new javax.swing.JDialog();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        whitePlayerHuman = new javax.swing.JRadioButton();
        whitePlayerComputer = new javax.swing.JRadioButton();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        whitePlayerComputerLevel = new javax.swing.JSpinner();
        javax.swing.JPanel jPanel3 = new javax.swing.JPanel();
        blackPlayerHuman = new javax.swing.JRadioButton();
        blackPlayerComputer = new javax.swing.JRadioButton();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        blackPlayerComputerLevel = new javax.swing.JSpinner();
        javax.swing.JButton playersSettingApply = new javax.swing.JButton();
        javax.swing.JButton playersSettingCancel = new javax.swing.JButton();
        fc = new javax.swing.JFileChooser();
        area = new GraphicArea();
        javax.swing.JToolBar jToolBar1 = new javax.swing.JToolBar();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        moveHistory = new javax.swing.JTextArea();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        statusBar = new javax.swing.JLabel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        piecesCount = new javax.swing.JLabel();
        menu = new javax.swing.JMenuBar();
        menuGame = new javax.swing.JMenu();
        menuGameNew = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator1 = new javax.swing.JSeparator();
        menuGameLoad = new javax.swing.JMenuItem();
        menuGameSave = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator2 = new javax.swing.JSeparator();
        menuGameEnd = new javax.swing.JMenuItem();
        menuHistory = new javax.swing.JMenu();
        menuHistoryBack = new javax.swing.JMenuItem();
        menuHistoryNext = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator3 = new javax.swing.JSeparator();
        menuHistoryBegin = new javax.swing.JMenuItem();
        menuHistoryEnd = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator4 = new javax.swing.JSeparator();
        menuHistoryReplayStart = new javax.swing.JMenuItem();
        menuHistoryReplayAkt = new javax.swing.JMenuItem();
        menuSettings = new javax.swing.JMenu();
        javax.swing.JMenuItem menuSettingsPlayers = new javax.swing.JMenuItem();
        javax.swing.JMenu menuHelp = new javax.swing.JMenu();
        javax.swing.JMenuItem menuHelpContent = new javax.swing.JMenuItem();
        menuHelpMove = new javax.swing.JMenuItem();

        playersSettings.setTitle("Nastavení hráèù");
        playersSettings.setModal(true);
        playersSettings.setResizable(false);

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel2.setToolTipText("Bílý hráè");
        jPanel2.setName("Bílý hráè"); // NOI18N

        groupPlayersWhite.add(whitePlayerHuman);
        whitePlayerHuman.setText("Èlovìk");
        whitePlayerHuman.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                whitePlayerChanged(evt);
            }
        });

        groupPlayersWhite.add(whitePlayerComputer);
        whitePlayerComputer.setSelected(true);
        whitePlayerComputer.setText("Poèítaè úroveò:");
        whitePlayerComputer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                whitePlayerChanged(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 14));
        jLabel1.setText("Bílý hráè:");

        whitePlayerComputerLevel.setFont(new java.awt.Font("Courier", 0, 14));
        whitePlayerComputerLevel.setModel(new javax.swing.SpinnerNumberModel(1, 1, MAX_COMPUTER_LEVEL, 1));

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 20, Short.MAX_VALUE)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(whitePlayerComputer)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(whitePlayerComputerLevel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(whitePlayerHuman))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(23, 23, 23)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(whitePlayerComputerLevel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(whitePlayerComputer)))
                    .add(whitePlayerHuman)
                    .add(jLabel1))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel3.setToolTipText("Bílý hráè");
        jPanel3.setName("Bílý hráè"); // NOI18N

        groupPlayersBlack.add(blackPlayerHuman);
        blackPlayerHuman.setSelected(true);
        blackPlayerHuman.setText("Èlovìk");
        blackPlayerHuman.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blackPlayerChanged(evt);
            }
        });

        groupPlayersBlack.add(blackPlayerComputer);
        blackPlayerComputer.setText("Poèítaè úroveò:");
        blackPlayerComputer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blackPlayerChanged(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 14));
        jLabel2.setText("Èerný hráè:");

        blackPlayerComputerLevel.setFont(new java.awt.Font("Courier", 0, 14));
        blackPlayerComputerLevel.setModel(new javax.swing.SpinnerNumberModel(1, 1, MAX_COMPUTER_LEVEL, 1));
        blackPlayerComputerLevel.setEnabled(false);

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 3, Short.MAX_VALUE)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(blackPlayerComputer)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(blackPlayerComputerLevel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(blackPlayerHuman))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(23, 23, 23)
                        .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(blackPlayerComputer)
                            .add(blackPlayerComputerLevel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(blackPlayerHuman)
                    .add(jLabel2))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        playersSettingApply.setText("OK");
        playersSettingApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playersSettingApply(evt);
            }
        });

        playersSettingCancel.setText("Cancel");
        playersSettingCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playersSettingCancel(evt);
            }
        });

        org.jdesktop.layout.GroupLayout playersSettingsLayout = new org.jdesktop.layout.GroupLayout(playersSettings.getContentPane());
        playersSettings.getContentPane().setLayout(playersSettingsLayout);
        playersSettingsLayout.setHorizontalGroup(
            playersSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(playersSettingsLayout.createSequentialGroup()
                .add(playersSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(playersSettingsLayout.createSequentialGroup()
                        .add(64, 64, 64)
                        .add(playersSettingApply, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 63, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(playersSettingCancel))
                    .add(playersSettingsLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(playersSettingsLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        playersSettingsLayout.setVerticalGroup(
            playersSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(playersSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(11, 11, 11)
                .add(playersSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(playersSettingCancel)
                    .add(playersSettingApply))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("16 vojákù");
        setResizable(false);

        fc.setEnabled(false);
        fc.setVerifyInputWhenFocusTarget(false);
        fc.addChoosableFileFilter(new XMLFileFilter());

        area.setMinimumSize(new java.awt.Dimension(450, 250));
        area.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                areaClick(evt);
            }
        });

        jToolBar1.setRollover(true);

        moveHistory.setColumns(15);
        moveHistory.setEditable(false);
        moveHistory.setRows(5);
        jScrollPane1.setViewportView(moveHistory);

        jLabel3.setText("Historie tahù:");

        statusBar.setText("Novou hru vytvoøíte kliknutím na \"Soubor\" > \"Nová hra\"");
        statusBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel4.setText("Figurek ve høe:");

        piecesCount.setText("--  bílých;    --  èerných");

        menuGame.setText("Soubor");

        menuGameNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        menuGameNew.setText("Nová hra");
        menuGameNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newGame(evt);
            }
        });
        menuGame.add(menuGameNew);
        menuGame.add(jSeparator1);

        menuGameLoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        menuGameLoad.setLabel("Otevøít");
        menuGameLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                load(evt);
            }
        });
        menuGame.add(menuGameLoad);

        menuGameSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        menuGameSave.setText("Uložit...");
        menuGameSave.setEnabled(false);
        menuGameSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save(evt);
            }
        });
        menuGame.add(menuGameSave);
        menuGame.add(jSeparator2);

        menuGameEnd.setText("Konec");
        menuGameEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                end(evt);
            }
        });
        menuGame.add(menuGameEnd);

        menu.add(menuGame);

        menuHistory.setText("Historie");

        menuHistoryBack.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        menuHistoryBack.setText("Zpìt");
        menuHistoryBack.setEnabled(false);
        menuHistoryBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backward(evt);
            }
        });
        menuHistory.add(menuHistoryBack);

        menuHistoryNext.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        menuHistoryNext.setText("Vpøed");
        menuHistoryNext.setEnabled(false);
        menuHistoryNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forward(evt);
            }
        });
        menuHistory.add(menuHistoryNext);
        menuHistory.add(jSeparator3);

        menuHistoryBegin.setText("Na zaèátek");
        menuHistoryBegin.setEnabled(false);
        menuHistoryBegin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveToStart(evt);
            }
        });
        menuHistory.add(menuHistoryBegin);

        menuHistoryEnd.setText("Na konec");
        menuHistoryEnd.setEnabled(false);
        menuHistoryEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveToEnd(evt);
            }
        });
        menuHistory.add(menuHistoryEnd);
        menuHistory.add(jSeparator4);

        menuHistoryReplayStart.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        menuHistoryReplayStart.setText("Pøehrát od zaèátku");
        menuHistoryReplayStart.setEnabled(false);
        menuHistoryReplayStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replayStart(evt);
            }
        });
        menuHistory.add(menuHistoryReplayStart);

        menuHistoryReplayAkt.setText("Pøehrát od akt. pozice");
        menuHistoryReplayAkt.setEnabled(false);
        menuHistoryReplayAkt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replayAkt(evt);
            }
        });
        menuHistory.add(menuHistoryReplayAkt);

        menu.add(menuHistory);

        menuSettings.setText("Nastavení");

        menuSettingsPlayers.setText("Hráèi");
        menuSettingsPlayers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showPlayersSetting(evt);
            }
        });
        menuSettings.add(menuSettingsPlayers);

        menu.add(menuSettings);

        menuHelp.setText("Nápovìda");

        menuHelpContent.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        menuHelpContent.setText("Obsah nápovìdy");
        menuHelpContent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpContent(evt);
            }
        });
        menuHelp.add(menuHelpContent);

        menuHelpMove.setText("Nejlepší tah");
        menuHelpMove.setEnabled(false);
        menuHelpMove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                suggestMove(evt);
            }
        });
        menuHelp.add(menuHelpMove);

        menu.add(menuHelp);

        setJMenuBar(menu);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(area, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 450, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel3)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(50, 50, 50))
            .add(statusBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .add(129, 129, 129)
                .add(jLabel4)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(piecesCount)
                .addContainerGap(333, Short.MAX_VALUE))
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(layout.createSequentialGroup()
                    .add(0, 325, Short.MAX_VALUE)
                    .add(fc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(0, 325, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(area, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 250, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(layout.createSequentialGroup()
                        .add(jLabel3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane1)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(piecesCount))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(layout.createSequentialGroup()
                    .add(0, 155, Short.MAX_VALUE)
                    .add(fc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(0, 154, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void newGame(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newGame
        if(!first){
            Object[] options = {"Ano", "Ne"};
            int n = JOptionPane.showOptionDialog(this,
                    "Vytvoøením nové hry pøijdete o aktuální partii.\n"
                    +"Chcete vytvoøit novou hru a smazat souèasnou?",
                    "Vytvoøit novou hru?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            if(n != JOptionPane.YES_OPTION){
                return;
            }
        }
        end = false;
        
        ad.newGame(whitePlayer,whitePlayerLevel,blackPlayer,blackPlayerLevel);
        
        if(first){
            ad.start();
        }
        first = false;

        menuGameSave.setEnabled(true);
        showStatus("stopped");
    }//GEN-LAST:event_newGame

    private void moveToStart(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveToStart
        end = false;
        ad.moveToStart();
        showStatus("stopped");
        menuHelpMove.setEnabled(false);
    }//GEN-LAST:event_moveToStart
    
    private void moveToEnd(java.awt.event.ActionEvent evt) {
        end = false;
        ad.moveToEnd();
        showStatus("stopped");
        menuHelpMove.setEnabled(false);
    }

    
    private void areaClick(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_areaClick
        if(end){
            return;
        }
        if(ad.start()){
            showStatus();
            if(replay){
                showStatus("replayStopped");
                replay = false;
            } else if(whitePlayer == COMPUTER_PLAYER && blackPlayer == COMPUTER_PLAYER){
                showStatus("computers");
            }
            showHideHistory();
            return;
        }
        
        if(!wantMove || allowedMoves==null || allowedMoves.size()<1){
            return;
        }
        int x=evt.getX(), y=evt.getY(),
                xS=(int) Math.round((x-25)/50.0), yS=(int) Math.round(((y-25)/50.0));
        float xMod=(x-25)%50, yMod=(y-25)%50;
        
        if(yS==0 && (xS==0 || xS==8)){
            ++yS;
        }
        if(yS==4 && (xS==0 || xS==8)){
            --yS;
        }
       
        if(xMod<(50-RADIUS) && xMod>RADIUS || yMod<(50-RADIUS) && yMod>RADIUS
                || !areaGA.pieceContains(xS,yS,x,y)){
            return;
        }
        int pieceColor = areaGA.getPieceColor(xS, yS);
        if(pieceColor != moveColor && pieceColor != RED && pieceColor != BLANK){
            return;
        }
                
        //double click na koncovou pozici
        if(wantMove && (moveId!=-1 && move.size() > 0 || oldMoveId!=-1) && System.nanoTime()-lastClick < DOUBLE_CLICK_INTERVAL*1000000){
            Integer[][] lastPosition = move.get(move.size()-1);
            if(lastPosition[1][0]==xS && lastPosition[1][1]==yS){
                moveFinished = true;
                showStatus();
                ad.notifyPlayer();
                return;
            } else if(oldMoveId != -1){
                List<Integer[][]> lastMove = allowedMoves.get(oldMoveId);
                lastPosition = lastMove.get(lastMove.size()-1);
                if(lastPosition[1][0]==xS && lastPosition[1][1]==yS){
                    moveId = oldMoveId;
                    oldMoveId = -1;
                    moveFinished = true;
                    showStatus();
                    ad.notifyPlayer();
                    return;
                }
            }
        }
        oldMoveId = moveId;
        
        
        //pokud je figurka na kterou se kliklo ve vybranem tahu - smazani
        boolean remove=false;
        for(ListIterator<Integer[][]> it = move.listIterator(); it.hasNext();){
            Integer[][] next = it.next();
            if(remove || next[1][0]==xS && next[1][1]==yS){
                areaGA.setPieceColor(next[1][0], next[1][1], it.previousIndex()==0&&!remove?moveColor:BLANK);
                it.remove();
                remove = true;
            }
        }
        if(remove){
            moveId = -1;
            lastClick = System.nanoTime();
            return;
        }
        
        //overeni zda zadany tah odpovida nekteremu z moznych
        int moveNextStep = move.size(),
                i=0,
                matchedSecondId = -1;
        boolean matched = false,
                matchedSelected = true,
                matchedSecondFinished = false;
        Integer[][] matchedFirst = null,
                matchedSecond = null,
                ns = null,
                nm = null;
        for(List<Integer[][]> m : allowedMoves){
            if(m.size() > moveNextStep){
                //zda aktualne prochazeny tah zacina stejne jako vybrany
                matchedSelected = true;
                for(int j=0;j<moveNextStep;++j){
                    ns = m.get(j);
                    nm = move.get(j);
                    if(ns[1][0] != nm[1][0] || ns[1][1] != nm[1][1]){
                        matchedSelected = false;
                        break;
                    }
                }
                //zda aktualni tah pokracuje mistem kam uzivatel klikl
                if(matchedSelected){
                    ns = m.get(moveNextStep);
                    if(ns[1][0] == xS && ns[1][1] == yS){
                        move.add(ns);
                        areaGA.setPieceColor(xS, yS, Piece.RED);
                        if(m.size() == moveNextStep+1){
                            moveId = i;
                        } else {
                            moveId = -1;
                        }
                        matched = true;
                        break;
                    }
                }
            }
            //kliklo se na jinou figurku kterou lze hrat
            if(matchedFirst==null){
                ns = m.get(0);
                if(ns[1][0] == xS && ns[1][1] == yS){
                    matchedFirst = ns;
                }
            }
            //druhy krok jineho tahu stejnou figurkou
            if(matchedSecond==null && move!=null && move.size()>=2){
                ns = m.get(0);
                nm = move.get(0);
                if(ns[1][0] == nm[1][0] && ns[1][1] == nm[1][1]){
                    ns = m.get(1);
                    if(ns[1][0] == xS && ns[1][1] == yS){
                        matchedSecond = ns;
                        if(m.size() == 2){
                            matchedSecondFinished = true;
                            matchedSecondId = i;
                        }
                    }
                }
            }
            ++i;
        }
        //smazani vybraneho tahu klikl-li uzivatel na jinou svou figurku kterou muze tahnout
        if(moveNextStep>0 && !matched && (pieceColor == moveColor && matchedFirst != null || pieceColor == BLANK && matchedSecond != null)){
            paintArea(ad.getArea());
            areaGA.setPieceColor(xS, yS, Piece.RED);
            if(matchedFirst != null){
                move.clear();
                move.add(matchedFirst);
                moveId = -1;
            } else if(matchedSecond != null){
                move.subList(1, move.size()).clear();
                ns = move.get(0);
                areaGA.setPieceColor(ns[1][0], ns[1][1], Piece.RED);
                move.add(matchedSecond);
                areaGA.setPieceColor(matchedSecond[1][0], matchedSecond[1][1], Piece.RED);
                if(matchedSecondFinished){
                    moveId = matchedSecondId;
                }
            }
        }
        
        lastClick = System.nanoTime();
    }//GEN-LAST:event_areaClick

    private void backward(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backward
        end = false;
        ad.backward();
        showStatus("stopped");
        menuHelpMove.setEnabled(false);
    }//GEN-LAST:event_backward

    private void forward(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forward
        end = false;
        ad.forward();
        showStatus("stopped");
        menuHelpMove.setEnabled(false);
    }//GEN-LAST:event_forward

    private void end(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_end
        System.exit(0);
    }//GEN-LAST:event_end

    private void suggestMove(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_suggestMove
        int suggested = -1;
        if(wantMove && moves != null){
            suggested = ad.suggestMove(moves);
        }
        if(suggested == -1 || allowedMoves.size() <= suggested){
            return;
        }
        paintArea(ad.getArea());
        moveId = suggested;
        move = new ArrayList<Integer[][]>(allowedMoves.get(suggested));
        paintMove();
    }//GEN-LAST:event_suggestMove

    private void load(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_load
        end = false;
        ad.stop();
        fc.setDialogTitle("Otevøít");
        fc.setApproveButtonText("Otevøít");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                ad.load(file.getPath());
                menuGameSave.setEnabled(true);
                whitePlayer = ad.getPlayerType(WHITE);
                whitePlayerLevel = ad.getPlayerLevel(WHITE);
                blackPlayer = ad.getPlayerType(BLACK);
                blackPlayerLevel = ad.getPlayerLevel(BLACK);
                showPlayersSetting();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Zadaný soubor se nepodaøilo naèíst.\nZkontrolujte zda existuje a má povoleno ètení.", "Chyba pøi naèítání", JOptionPane.ERROR_MESSAGE);
            }
        }
        showStatus("stopped");
    }//GEN-LAST:event_load

    private void save(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save
        ad.stop();
        fc.setDialogTitle("Uložit");
        fc.setApproveButtonText("Uložit");
        fc.setCurrentDirectory(new java.io.File("."));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                ad.save(file.getPath());
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                JOptionPane.showMessageDialog(this, "Hru se nepodaøilo uložit.\nZkontrolujte zda existuje zadaný adresáø a zda je povoleno do nìj zapisovat.", "Chyba pøi ukládání", JOptionPane.ERROR_MESSAGE);
            }
        }
        ad.start();
    }//GEN-LAST:event_save

    private void showPlayersSetting(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showPlayersSetting
        ad.stop();
        playersSettings.pack();
        playersSettings.setVisible(true);
}//GEN-LAST:event_showPlayersSetting
    
    private void playersSettingApply(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playersSettingApply
        playersSettings.setVisible(false);
        
        int wp = whitePlayerHuman.isSelected()?HUMAN_PLAYER:COMPUTER_PLAYER,
                wl = (Integer)whitePlayerComputerLevel.getValue(),
                bp = blackPlayerHuman.isSelected()?HUMAN_PLAYER:COMPUTER_PLAYER,
                bl = (Integer)blackPlayerComputerLevel.getValue();
        boolean changed = false;
        
        if(wp != whitePlayer || (wp == COMPUTER_PLAYER && wl != whitePlayerLevel)){
            whitePlayer = wp;
            whitePlayerLevel = wp==COMPUTER_PLAYER ? wl : 0;
            ad.setPlayer(WHITE, wp==COMPUTER_PLAYER, wl, true);
            changed = true;
        }
        
        if(bp != blackPlayer || (bp == COMPUTER_PLAYER && bl != blackPlayerLevel)){
            blackPlayer = bp;
            blackPlayerLevel = bp==COMPUTER_PLAYER ? bl : 0;
            ad.setPlayer(BLACK, bp==COMPUTER_PLAYER, bl, true);
            changed = true;
        }
        if(changed){
            ad.showArea();
        }
        
        if(!first){
            showStatus("stopped");
        }
    }//GEN-LAST:event_playersSettingApply

    private void whitePlayerChanged(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_whitePlayerChanged
        whitePlayerComputerLevel.setEnabled(whitePlayerComputer.isSelected());
    }//GEN-LAST:event_whitePlayerChanged

    private void blackPlayerChanged(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blackPlayerChanged
        blackPlayerComputerLevel.setEnabled(blackPlayerComputer.isSelected());
    }//GEN-LAST:event_blackPlayerChanged

    private void playersSettingCancel(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playersSettingCancel
        playersSettings.setVisible(false);
        
        showPlayersSetting();
        
        if(!first){
            showStatus("stopped");
        }
    }//GEN-LAST:event_playersSettingCancel

  private void replayStart(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replayStart
      end = false;
      replay = true;
      showStatus("replay");
      ad.replay(true);
      menuHelpMove.setEnabled(false);
  }//GEN-LAST:event_replayStart

  private void replayAkt(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replayAkt
      end = false;
      replay = true;
      showStatus("replay");
      ad.replay(false);
      menuHelpMove.setEnabled(false);
  }//GEN-LAST:event_replayAkt

  private void helpContent(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpContent
      hb.setDisplayed(true);//GEN-LAST:event_helpContent
  }
    

    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Gui().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JLayeredPane area;
    javax.swing.JRadioButton blackPlayerComputer;
    javax.swing.JSpinner blackPlayerComputerLevel;
    javax.swing.JRadioButton blackPlayerHuman;
    javax.swing.JFileChooser fc;
    javax.swing.ButtonGroup groupPlayersBlack;
    javax.swing.ButtonGroup groupPlayersWhite;
    javax.swing.JMenuBar menu;
    javax.swing.JMenu menuGame;
    javax.swing.JMenuItem menuGameEnd;
    javax.swing.JMenuItem menuGameLoad;
    javax.swing.JMenuItem menuGameNew;
    javax.swing.JMenuItem menuGameSave;
    javax.swing.JMenuItem menuHelpMove;
    javax.swing.JMenu menuHistory;
    javax.swing.JMenuItem menuHistoryBack;
    javax.swing.JMenuItem menuHistoryBegin;
    javax.swing.JMenuItem menuHistoryEnd;
    javax.swing.JMenuItem menuHistoryNext;
    javax.swing.JMenuItem menuHistoryReplayAkt;
    javax.swing.JMenuItem menuHistoryReplayStart;
    javax.swing.JMenu menuSettings;
    javax.swing.JTextArea moveHistory;
    private javax.swing.JLabel piecesCount;
    javax.swing.JDialog playersSettings;
    javax.swing.JLabel statusBar;
    javax.swing.JRadioButton whitePlayerComputer;
    javax.swing.JSpinner whitePlayerComputerLevel;
    javax.swing.JRadioButton whitePlayerHuman;
    // End of variables declaration//GEN-END:variables
    
    private GraphicArea areaGA;
    
}
