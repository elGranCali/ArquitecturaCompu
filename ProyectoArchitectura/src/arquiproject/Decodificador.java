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
    
    
    public static void decodificacion(String instruccion , int[] registros, Contexto contextoRecibido){
        contexto = contextoRecibido;
        int posEspacio = instruccion.indexOf(" ");
        String codOp = instruccion.substring(0, posEspacio);
        instruccion = instruccion.substring(posEspacio+1, instruccion.length());
        posEspacio = instruccion.indexOf(" ");
        String registro1 = instruccion.substring(0, posEspacio);
        instruccion = instruccion.substring(posEspacio+1, instruccion.length());
        posEspacio = instruccion.indexOf(" ");
        String registro2 = instruccion.substring(0, posEspacio);
        instruccion = instruccion.substring(posEspacio+1, instruccion.length());
        String registro3 = instruccion;
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
                registros[r2] = registros[r1]+r3;
                System.out.print(registros[r2]);
                break;
            case "32":
                registros[r3]=registros[r1]+registros[r2];
                System.out.print(registros[r3]);
                break;
            case "34":
                registros[r3]=registros[r1]-registros[r2];
                System.out.print(registros[r3]);
                break;
            case "12":
                registros[r3]=registros[r1]*registros[r2];
                System.out.print(registros[r3]);
                break;
            case "14":
                registros[r3]=registros[r1]/registros[r2];
                System.out.print(registros[r3]);
                break;
            /*case "35":
                registros[r2]= M*(r3+registros[r1]);
                System.out.print(registros[r2]);
                break;*/
            /*case "43":
                break;*/
            case "4":
                if(registros[r1] == 0)
                    contexto.PC = contexto.PC+(r3*4);                             
                break;
            case "5":
                if(registros[r1]!= 0)
                    contexto.PC = contexto.PC+(r3*4);
                break;
            case "3":
                registros[31] = contexto.PC;
                contexto.PC += r3;
                System.out.print("PC="+contexto.PC+" R31="+registros[31]);
                break;
            case "2":
                contexto.PC = registros[r1];
                System.out.print("PC="+contexto.PC);
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
                System.out.println("Ya termino el hilo");
                //System.exit(0);
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
