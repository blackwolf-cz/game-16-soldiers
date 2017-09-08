package kandrm.hra.gui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JLayeredPane;

import static kandrm.hra.gui.Piece.*;
import static kandrm.hra.gui.UIAdapter.*;

/**
 * Komponenta grafického rozhraní reprezentující hrací plochu. Na pozadí je obrázek plochy a na nìm se kreslí jednotlivé kamenny.
 * @author Michal Kandr
 */
public class GraphicArea extends JLayeredPane {
    private BufferedImage background;
    private Piece[][] area;

    public GraphicArea(){
        try {
            background = ImageIO.read(new File("images/deska.gif"));
        } catch (IOException ex) {
            Logger.getLogger(GraphicArea.class.getName()).log(Level.SEVERE, null, ex);
        }

        area = new Piece[9][5];
        for(int i=0,iw=25;i<9;++i,iw+=50){
            for(int j=0,jh=25;j<5;++j,jh+=50){
                if((i==0 || i==1 || i==7 || i==8) && (j==0 || j==4)){
                    area[i][j] = null;
                }else{
                    if(i==0 || i==8){
                        if(j==1){
                            jh = 25;
                        } else if(j>1){
                            jh+=50;
                        }
                    }
                    area[i][j] = new Piece(BLANK,iw,jh);
                }
            }
        }
        
        setOpaque(true);
    }
    
    
    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(background,0,0,null);
        for(int i=0;i<area.length;++i){
            for(int j=0;j<area[i].length;++j){
                if(area[i][j] != null){
                    area[i][j].paint(g);
                }
            }
        }
    }    
    
    /**
     * zjsiti barvu kamene na urcite pozici
     * @param x x-souradnice kamene
     * @param y y-souradnice kamene
     * @return barva kamene na teto ozici
     */
    public int getPieceColor(int x,int y){
        return area[x][y].getColor();
    }
    
    /**
     * zmeni barvu kamene na pozici
     * @param x x-souradnice kamene
     * @param y y-souradnice kamene
     * @param color barva ktera ma byt kameni nastavena
     */
    public void setPieceColor(int x,int y,int color){
        area[x][y].setColor(color);
        repaint();
    }

    /**
     * zjisti jestli kamen na pozici obsahuje prislisny bod
     * @param pX x-souradnice kamene
     * @param pY y-souradnice kamene
     * @param x x-souradnice bodu
     * @param y y-souradnice bodu
     */
    public boolean pieceContains(int pX,int pY,int x,int y){
        return pX>=0 && pX<=8 && pY>=0 && pY <=4 && area[pX][pY] != null && area[pX][pY].contains(x, y);
    }
    
    /**
     * zmeni podobu plochy (barvu kamenu) podle zadane polochy
     * @param ar plocha ktera se ma zobrazit
     */
    public void changeArea(int[][] ar){
        for(int i=0;i<ar.length&&i<area.length;++i){
            for(int j=0;j<ar[i].length&&j<area[i].length;++j){
                if(ar[i][j] == BLACK || ar[i][j] == WHITE){
                    area[i][j].setColor(ar[i][j]);
                } else if(area[i][j]!=null){
                    area[i][j].setColor(BLANK);
                }
            }
        }
        repaint();
    }
}
