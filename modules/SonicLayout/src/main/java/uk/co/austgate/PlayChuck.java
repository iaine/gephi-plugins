/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.austgate;

import java.io.IOException;

/**
 *
 * @author iain
 */
public class PlayChuck {
    
    private static String filePath;
    
    protected void PlayChuck(String baseDir) {
        filePath = baseDir;
        
        startChuck();
    }

    /**
     * Call a Chuck sound from command line
     * @param chuckCommands 
     */
    public void playSound (String[] chuckCommands) {
      try {
       
        Runtime.getRuntime().exec(chuckCommands);
      } catch (IOException ioe) {
            System.out.println("IOE " + ioe);
      }
    }

    
    /**
     * Function to start a Chuck server. 
     * 
     * We assume that it is in the ChucK directory. 
     * @param baseDir 
     */
    private void startChuck() {
      try {
        String[] command =
        {
            "cmd",
        };
        
        Runtime.getRuntime().exec(command);
      } catch (IOException ioe) {
            System.out.println("IOE " + ioe);
      }
    }
}
