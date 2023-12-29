/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.austgate;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.co.austgate.ToneAlgorithms;
/**
 *
 * @author iain
 */
public class testToneAlgorithms {
    
    private final ToneAlgorithms toneAlgorithm = new ToneAlgorithms();
    
    @Test
    public void shouldCreatePositiveCent () {
        double newCent = toneAlgorithm.createCent(440.0, 10);
        assertEquals(445,0, newCent);
    }
    
    @Test
    public void shouldCreateNegativeCent () {
        
    }
}
