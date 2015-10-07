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
    
    public int PC;
    public int[] registros = new int[33];
    public String id;
    
    public Contexto(int pc, int name){
        this.PC = pc;
        this.id = "HILILLO " + name;
        for(int i = 0; i<33; i++){
            registros[i]=0;
        }
    }
    
    @Override
    public String toString(){
        String respuesta = "\n***********HILO" + id + "*****************************************";
        respuesta += "\nValor del PC: " + PC + "\n Valor de registros: ";
        for (int i = 0 ; i< 33; i++ ) {
            respuesta += "[R-" + i +"] = " + registros[i] + "\t\t";
            if (i%2 == 0) {
                respuesta += "\n";
            }
        }
        respuesta += "\n****************************************************";
        return respuesta;
    }
    
   // Setters y getter de eso bichos
            
}
