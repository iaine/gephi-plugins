/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.austgate;

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author iain
 */
public class ToneGenerator {
    private boolean stop = true;
    
    public void setStop (boolean state) {
        this.stop = state;
    }
    //@todo: parameterise frequency and duration
   public void generateTone(int freq, int dur) throws InterruptedException, LineUnavailableException  {
       
       try {
           
           if (!this.stop) {

            final int SAMPLING_RATE = 44100;            // Audio sampling rate
            final int SAMPLE_SIZE = 2;                  // Audio sample size in bytes

            SourceDataLine line;
            double fFreq = 440 + freq;                         // Frequency of sine wave in hz

            //Position through the sine wave as a percentage (i.e. 0 to 1 is 0 to 2*PI)
            double fCyclePosition = 0;        

            //Open up audio output, using 44100hz sampling rate, 16 bit samples, mono, and big 
            // endian byte ordering
            AudioFormat format = new AudioFormat(SAMPLING_RATE, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)){
               System.out.println("Line matching " + info + " is not supported.");
               throw new LineUnavailableException();
            }

            line = (SourceDataLine)AudioSystem.getLine(info);
            line.open(format);  
            line.start();

            // Make our buffer size match audio system's buffer
            ByteBuffer cBuf = ByteBuffer.allocate(line.getBufferSize());   

            int ctSamplesTotal = SAMPLING_RATE*(dur/10);         // Output for roughly 5 seconds


            //On each pass main loop fills the available free space in the audio buffer
            //Main loop creates audio samples for sine wave, runs until we tell the thread to exit
            //Each sample is spaced 1/SAMPLING_RATE apart in time
            while (ctSamplesTotal>0) {
               double fCycleInc = fFreq/SAMPLING_RATE;  // Fraction of cycle between samples

               cBuf.clear();                            // Discard samples from previous pass

                // Figure out how many samples we can add
               int ctSamplesThisPass = line.available()/SAMPLE_SIZE;   
               for (int i=0; i < ctSamplesThisPass; i++) {
                  cBuf.putShort((short)(Short.MAX_VALUE * Math.sin(2*Math.PI * fCyclePosition)));

                  fCyclePosition += fCycleInc;
                  if (fCyclePosition > 1)
                     fCyclePosition -= 1;
               }

               //Write sine samples to the line buffer.  If the audio buffer is full, this will 
               // block until there is room (we never write more samples than buffer will hold)
               line.write(cBuf.array(), 0, cBuf.position());            
               ctSamplesTotal -= ctSamplesThisPass;     // Update total number of samples written 

               //Wait until the buffer is at least half empty  before we add more
               while (line.getBufferSize()/2 < line.available())   
                  Thread.sleep(1);                                             
            }


            //Done playing the whole waveform, now wait until the queued samples finish 
            //playing, then clean up and exit
            line.drain();                                         
            line.close();
           }
       } catch (LineUnavailableException lue) {
           System.out.println("Line Unavailable " + lue.toString());
       } catch (InterruptedException ie) {
           System.out.println("Interrupted " + ie.toString());
       } catch (Exception e) {
           System.out.println("StandardError " + e.toString());
       }
   }
}
