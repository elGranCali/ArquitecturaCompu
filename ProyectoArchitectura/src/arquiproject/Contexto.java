/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

/**
 *
 * @author CALI
 */
public class Contexto {
    
    private int PC;
    private int[] registros = new int[33];
    
    public Contexto(){
        PC = 0;
        for(int i = 0; i<33; i++){
            registros[i]=0;
        }
    }
    
   // Setters y getter de eso bichos
            
}
