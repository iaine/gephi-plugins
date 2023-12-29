/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.austgate;

/**
 *
 * @author iain
 */
public class ToneAlgorithms {

    /**
     * Function to create a simple cent.
     * @param base
     * @param cents
     * @return cent - an integer derived from the base frequency
     */
    public double createCent(double base, int cents) {
        double cent;
        //baseFreq * Math.pow(2, (cents/1200));
        cent = base * Math.pow(2.0, cents / 1200);
        return cent;
    }
    
}
