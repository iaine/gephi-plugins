/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.austgate;

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * 
 * Tone Generator class
 * 
 * This class provides methods for producing a Sine wave. 
 *
 * @author Iain Emsley
 */
public class ToneGenerator {
    
    /**
     * stop defaults to false. Should stop sound.
     */
    private boolean stop = false;
    
    /**
     * setStop toggles the stop between true and false. 
     * @param state 
     */
    public void setStop (boolean state) {
        this.stop = state;
    }
    /**
     * generateTone.
     * 
     * The method generates the tone. It takes the initial frequency from SonicLayout
     * with the duration. The sine wave is created and then put into the LineOut.
     * 
     * The sample rate is set from 44.1KHz where the duration is set by SonicLayout 
     * and then halved. 
     * 
     * The code fills a byte buffer and then puts the buffer into the SourceLine. 
     * 
     * @param freq
     * @param dur
     * @throws InterruptedException
     * @throws LineUnavailableException 
     */
   public void generateTone(double freq, int dur) throws InterruptedException, 
           LineUnavailableException  {
       
       try {
           if (!this.stop) {
                float rate = (44100 * (dur/10)/2);

                byte[] buf;
                AudioFormat audioF;

                buf = new byte[2];
                audioF = new AudioFormat(rate,16,1,true,false);
                //sampleRate, sampleSizeInBits,channels,signed,bigEndian

                SourceDataLine sourceDL;
                sourceDL = AudioSystem.getSourceDataLine(audioF);
                sourceDL.open(audioF);
                sourceDL.start();

                for(int i=0; i<rate; i++){
                    double angle = (i/rate)*freq*2.0*Math.PI;
                    buf[0]=(byte)(Math.sin(angle)*dur);
                    buf[1] = (byte)(Math.sin(2* angle)*dur);
                    sourceDL.write(buf,0,2);
                }

                sourceDL.drain();
                sourceDL.stop();
                sourceDL.close();
           }
       } catch (LineUnavailableException lue) {
           System.out.println("Line Unavailable " + lue.toString());
       } catch (Exception e) {
           System.out.println("StandardError " + e.toString());
       }
   }
 
   /**
    * generateTone.
    * 
    * The method generates the tone. It takes the initial frequency from SonicLayout
    * with the duration. The sine wave is created and then put into the LineOut.
    * This function take a cent (see @package uk.ac.austgate.toneAlgorithms) and 
    * uses it to create a harmonic. 
    * 
    * The sample rate is set from 44.1KHz where the duration is set by SonicLayout 
    * and then halved. 
    * 
    * The code fills a byte buffer and then puts the buffer into the SourceLine. 
    *
    * 
    * @param freq
    * @param cent
    * @param dur
    * @throws InterruptedException
    * @throws LineUnavailableException 
    */
    public void generateTone(double freq, double cent, int dur) throws InterruptedException, LineUnavailableException  {
       
       try {
           if (!this.stop) {
                float rate = (44100 * (dur/10)/2);

                byte[] buf;
                AudioFormat audioF;

                buf = new byte[2];
                audioF = new AudioFormat(rate,16,1,true,false);
                //sampleRate, sampleSizeInBits,channels,signed,bigEndian

                SourceDataLine sourceDL;
                sourceDL = AudioSystem.getSourceDataLine(audioF);
                sourceDL.open(audioF);
                sourceDL.start();

                for(int i=0; i<rate; i++){
                    double angle = (i/rate)*freq*2.0*Math.PI;
                    buf[0]=(byte)(Math.sin(angle)*dur);
                    buf[1] = (byte)(Math.sin(cent)*dur);
                    sourceDL.write(buf,0,2);
                }

                sourceDL.drain();
                sourceDL.stop();
                sourceDL.close();
           }
       } catch (LineUnavailableException lue) {
           System.out.println("Line Unavailable " + lue.toString());
       } catch (Exception e) {
           System.out.println("StandardError " + e.toString());
       }
   }
   
}
