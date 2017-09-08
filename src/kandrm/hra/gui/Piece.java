package kandrm.hra.gui;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import javax.swing.JButton;
import static kandrm.hra.core.Manager.*;

/**
 * Reprezentuje pozici na hrací ploše, která mùže být prázdná, obsahovat bílý nebo èerný kámen, pøípadnì být oznaèena v kombinaci s nìkterou z pøedchozích vlastností.
 * @author Michal Kandr
 */
public class Piece extends JButton{
    public static final int BLANK = WHITE+BLACK,
            RED = WHITE+BLACK+BLANK,
            RADIUS = 10;
    
    private int color=WHITE,
            oldColor=0,
            x=0,y=0;
    private Stroke whiteOutline; 
    
    /**
     * @param c barva pozice
     * @param x x-souradnice
     * @param y y-souradnice 
     */
    public Piece(int c,int x,int y){
        color = c;
        this.x = x;
        this.y = y;
        
        setBorder(null);
        setOpaque(false);
        setPreferredSize(new Dimension(RADIUS,RADIUS));
        setMaximumSize(new Dimension(RADIUS,RADIUS));
        setBounds(0,0,RADIUS,RADIUS);
        setSize(RADIUS,RADIUS);
        
        whiteOutline = new BasicStroke((float)1.5);
    }
    
    /**
     * nastavi bravu pozice
     * @param c barva na kterou se ma nastavit
     * @throws java.lang.IllegalArgumentException
     */
    public void setColor(int c) throws IllegalArgumentException{
        if(c != WHITE && c != BLACK && c != BLANK && c != RED){
            throw new IllegalArgumentException();
        }
        oldColor = color;
        color = c;
    }
    
    /**
     * barva pozice
     */
    public int getColor(){
        return color;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        Paint oldPaint = g2.getPaint();
        Ellipse2D e = new Ellipse2D.Double(x-RADIUS,y-RADIUS, (2*RADIUS), (2*RADIUS));
        
        if(color==WHITE || color==BLANK){
            g2.setPaint(Color.WHITE);
            g2.fill(e);
            g2.setPaint(color==BLANK?Color.GREEN:Color.BLACK);
            if(color==WHITE){
                Stroke oldOutline = g2.getStroke();
                g2.setStroke(whiteOutline);
                g2.draw(e);
                g2.setStroke(oldOutline);
            } else {
                g2.draw(e);
            }
        } else {
            g2.setPaint(color==BLACK?Color.BLACK:Color.RED);
            g2.fill(e);
            if(color==RED && (oldColor == WHITE || oldColor == BLACK)){
                g2.setPaint(Color.BLACK);
                Stroke oldOutline = g2.getStroke();
                g2.setStroke(whiteOutline);
                g2.draw(e);
                g2.setStroke(oldOutline);
            }
        }
        g2.setPaint(oldPaint);
    }
    
    @Override
    public boolean contains(int x,int y){
        return Math.pow((x-this.x),2)+Math.pow((y-this.y),2) <= Math.pow(RADIUS,2);
    }
}
