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
 * @author iain
 */
public class ToneGenerator {
    private boolean stop = false;
    
    public void setStop (boolean state) {
        this.stop = state;
    }
    //@todo: parameterise frequency and duration
   public void generateTone(double freq, int dur) throws InterruptedException, LineUnavailableException  {
       
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
               //}
           }
       } catch (LineUnavailableException lue) {
           System.out.println("Line Unavailable " + lue.toString());
       } catch (Exception e) {
           System.out.println("StandardError " + e.toString());
       }
   }
 
   /**
    * Function to create harmonics based on given frequencies. 
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
                    buf[1] = (byte)(Math.sin(2* angle)*dur);
                    sourceDL.write(buf,0,2);
                }

                sourceDL.drain();
                sourceDL.stop();
                sourceDL.close();
               //}
           }
       } catch (LineUnavailableException lue) {
           System.out.println("Line Unavailable " + lue.toString());
       } catch (Exception e) {
           System.out.println("StandardError " + e.toString());
       }
   }
   
   /**
    * Function to create a simple cent. 
    * @param base
    * @param cents
    * @return cent - an integer derived from the base frequency
    */
   public double createCent (double base, int cents) {
       double cent;
       //baseFreq * Math.pow(2, (cents/1200));
       cent = base * Math.pow(2.0, (cents/1200));
       return cent;
   }
}
