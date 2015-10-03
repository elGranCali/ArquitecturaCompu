/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;


/**
 *
 * @author B12037
 */
public class Decodificador {
    
    static int M = 2;
    static Contexto contexto;
    
    
    public static void decodificacion(String instruccion , Contexto contexto){

        String [] sepHilo = instruccion.split(" "); 
        String codOp = sepHilo[0];
        String registro1 = sepHilo[1];
        String registro2 = sepHilo[2];
        String registro3 = sepHilo[3];
        contexto.PC += 4;
        /*
            registros[0] = R0
            registros[1] = R1
            registros[2] = R2
            .
            .
            .
            .
            registros[31] = R31
            registros[32] = RL
        */
        int r1 = Integer.parseInt(registro1);
        int r2 = Integer.parseInt(registro2);
        int r3 = Integer.parseInt(registro3);
        switch(codOp){
            case "8":
                contexto.registros[r2] = contexto.registros[r1]+r3;
                //System.out.print(contexto.registros[r2]);
                break;
            case "32":
                contexto.registros[r3]= contexto.registros[r1]+contexto.registros[r2];
                //System.out.print(contexto.registros[r3]);
                break;
            case "34":
                contexto.registros[r3]=contexto.registros[r1]-contexto.registros[r2];
                //System.out.print(contexto.registros[r3]);
                break;
            case "12":
                contexto.registros[r3]=contexto.registros[r1]*contexto.registros[r2];
                //System.out.print(contexto.registros[r3]);
                break;
            case "14":
                contexto.registros[r3]=contexto.registros[r1]/contexto.registros[r2];
                //System.out.print(contexto.registros[r3]);
                break;
            /*case "35":
                registros[r2]= M*(r3+registros[r1]);
                System.out.print(registros[r2]);
                break;*/
            /*case "43":
                break;*/
            case "4":
                if(contexto.registros[r1] == 0)
                    contexto.PC = contexto.PC+(r3*4);                             
                break;
            case "5":
                if(contexto.registros[r1]!= 0)
                    contexto.PC = contexto.PC+(r3*4);
                break;
            case "3":
                contexto.registros[31] = contexto.PC;
                contexto.PC += r3;
                //System.out.print("PC="+contexto.PC+" R31="+contexto.registros[31]);
                break;
            case "2":
                contexto.PC = contexto.registros[r1];
                //System.out.print("PC="+contexto.PC);
                break;
            /*case "11":
                registros[r2] = M*(r3+registros[r1]);
                registros[32] = r3+registros[r1];
                break;
            case "17": //verificar este codOp porque está repetido, era originalmente 12
                if(registros[32] == r3+registros[r1]){
                    //no entiendo donde se guarda registros[r2]
                }else{
                    registros[r2] = 0;
                }
                break;*/
            case "63":
                break;
            default:
                break;
        }
    }
    
    public static boolean esFin(String instruccion){
        String [] division = instruccion.split(" ");
        boolean esFin = false;
        if(division[0].equals("63")) {
            esFin = true; 
        } 
        return esFin;
    }
}
