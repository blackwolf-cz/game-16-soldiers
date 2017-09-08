package kandrm.hra.core;

import static kandrm.hra.core.Manager.*;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Ulozeni a nacteni aktualniho stavu hry do/z souboru ve formatu XML.
 * @author Michal Kandr
 */
public class Export {

    /**
     * vsechny metody staticke - instance vytvaret netreba
     */
    private Export() {}
    
    /**
     * Ulozeni hry do souboru.
     * @param file nazev souboru
     * @param area aktualni hraci deska
     * @param whitePlayer kdo ji bily hrac (clovek nebo pocitac)
     * @param whitePlayerLevel uroven bileho hrace
     * @param blackPlayer kdo ji cerny hrac (clovek nebo pocitac)
     * @param blackPlayerLevel uroven cerneho hrace
     * @throws java.lang.Exception
     */
    public static void save(
            String file,
            PlayingArea area,
            int whitePlayer,
            int whitePlayerLevel,
            int blackPlayer,
            int blackPlayerLevel) throws Exception{
        List <Integer[][][]> moves = area.getHistory();
        int active = area.getLastMove(),
                i=0;
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = fact.newDocumentBuilder();
        Document doc = parser.newDocument();
        Attr at;
        
        
        Node koren = doc.createElement("game");
        Node tahy = doc.createElement("moves");
        koren.appendChild(tahy);
        
        Node tah, step,stepItem;
        for(Object o:moves){
            Integer[][][] t = (Integer[][][])o;
            tah = doc.createElement("move");
            tahy.appendChild(tah);
            if(active == i++){
                at = doc.createAttribute("actual");
                at.setNodeValue("1");
                ((Element)tah).setAttributeNode(at);
            }
            for(int j=0;j<t.length;++j){
                step = doc.createElement("step");
                tah.appendChild(step);
                
                stepItem = doc.createElement("action");
                step.appendChild(stepItem);
                stepItem.appendChild(doc.createTextNode(t[j][0][0].toString()));
                
                stepItem = doc.createElement("color");
                step.appendChild(stepItem);
                stepItem.appendChild(doc.createTextNode(t[j][0][1].toString()));
                
                if(t[j][1].length == 2){
                    stepItem = doc.createElement("coorx");
                    step.appendChild(stepItem);
                    stepItem.appendChild(doc.createTextNode(t[j][1][0].toString()));

                    stepItem = doc.createElement("coory");
                    step.appendChild(stepItem);
                    stepItem.appendChild(doc.createTextNode(t[j][1][1].toString()));
                }
            }
        }
        
        Node hraci = doc.createElement("players");
        koren.appendChild(hraci);
        
        Node hrac = doc.createElement("playerWhite");
        hraci.appendChild(hrac);
        hrac.appendChild(doc.createTextNode(Integer.toString(whitePlayer)));
        at = doc.createAttribute("level");
        at.setNodeValue(Integer.toString(whitePlayerLevel));
        ((Element)hrac).setAttributeNode(at);
        
        hrac = doc.createElement("playerBlack");
        hraci.appendChild(hrac);
        hrac.appendChild(doc.createTextNode(Integer.toString(blackPlayer)));
        at = doc.createAttribute("level");
        at.setNodeValue(Integer.toString(blackPlayerLevel));
        ((Element)hrac).setAttributeNode(at);
        
        
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(koren.getTextContent().getBytes());
        
        Node h = doc.createElement("hash");
        koren.appendChild(h);
        h.appendChild(doc.createTextNode(new BigInteger(1,digest.digest()).toString(16)));
        
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.transform(new DOMSource(koren), new StreamResult(new FileOutputStream(file)));
        
    }

    /**
     * nacteni stavu hry ze souboru
     * @param file nazev souboru
     * @param area aktualni hraci deska (predchozi obsah bude smazan)
     * @param manager instance managera (predchozi nastaveni hracu bude zruseno)
     * @throws java.lang.Exception
     */
    public static void load(String file,PlayingArea area, Manager manager) throws Exception{
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = fact.newDocumentBuilder();
        Document document = parser.parse(new File(file));
        NodeList tahy = document.getElementsByTagName("move"),
                steps,stepItems;
        Node tah,step,stepItem,attr,hrac,
                hashN = document.getElementsByTagName("hash").item(0);
        
        int i,j,k,actual=-1,value;
        List<Integer[][][]> moves = new ArrayList<Integer[][][]>();
        String hash = hashN.getTextContent(),
                nodeName;
        
        document.getElementsByTagName("game").item(0).removeChild(hashN);
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(document.getElementsByTagName("game").item(0).getTextContent().getBytes());
        
        if(!(new BigInteger(1,digest.digest()).toString(16).equals(hash))){
            throw new Exception();
        }
        
        for(i=0;i<tahy.getLength();++i){
            tah = tahy.item(i);
            steps = tah.getChildNodes();
            Integer[][][] moveFinal = new Integer[steps.getLength()][2][2];
            
            attr = tah.getAttributes().getNamedItem("actual");
            if(attr != null){
                actual = i;
            }
            
            for(j=0;j<steps.getLength();++j){
                step = steps.item(j);
                stepItems = step.getChildNodes();
                
                for(k=0;k<stepItems.getLength();++k){
                    stepItem = stepItems.item(k);
                    nodeName = stepItem.getNodeName();
                    value = Integer.parseInt(stepItem.getTextContent());
                    
                    if(nodeName.equals("action")){
                        moveFinal[j][0][0] = value;
                    } else if(nodeName.equals("color")){
                        moveFinal[j][0][1] = value;
                    } else if(nodeName.equals("coorx")){
                        moveFinal[j][1][0] = value;
                    } else if(nodeName.equals("coory")){
                        moveFinal[j][1][1] = value;
                    }
                }
            }
            moves.add(moveFinal);
        }
         
        area.clear();
        for(Object o:moves){
            area.move((Integer[][][])o);
        }
        for(i=0;i<moves.size()-actual;++i){
            area.backward();
        }
        
        hrac = document.getElementsByTagName("playerWhite").item(0);
        manager.setPlayer(WHITE, Integer.parseInt(hrac.getTextContent())==COMPUTER_PLAYER, Integer.parseInt(hrac.getAttributes().getNamedItem("level").getTextContent()));
        hrac = document.getElementsByTagName("playerBlack").item(0);
        manager.setPlayer(BLACK, Integer.parseInt(hrac.getTextContent())==COMPUTER_PLAYER, Integer.parseInt(hrac.getAttributes().getNamedItem("level").getTextContent()));
    }
}
