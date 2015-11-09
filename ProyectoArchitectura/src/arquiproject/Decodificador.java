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
        
        /*if (instruccion.equals("8 0 1 1")){
            System.out.println("eeeeeeeeeeeeeentro");
        }*/
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
            case "35":
                contexto.registros[r2]= M*(r3+contexto.registros[r1]);
                //System.out.print(registros[r2]);
                break;
            case "43":
                break;
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
            /*case "50":
                break;
            case "51": //verificar este codOp porque está repetido, era originalmente 12
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

    public static int instruccionMemoria(String instruccion){
        String [] division = instruccion.split(" ");
        int codigoInstr = 0;
        if(division[0].equals("35")) {
            codigoInstr=35;
        }
        if (division[0].equals("43")) {   
            codigoInstr = 43; 
        } 
        if (division[0].equals("50")) {   
            codigoInstr = 50; 
        } 
        if (division[0].equals("51")) {   
            codigoInstr = 51; 
        } 
        return codigoInstr;
    }
    
    public static int getDireccion(String hilillo, Contexto contexto){
        String [] division = hilillo.split(" ");
        int direccionVirtual= Integer.parseInt(division[3]) + contexto.registros[Integer.parseInt(division[1])]; 
        return direccionVirtual; 
    }
    
    public static int procesarDireccion(String hilillo) {  // SW R1 n(R2)
            int direccionDato;
            String [] pedazos = hilillo.split(" ");
            String direccion = pedazos[2]; // n(R2)
            String[] offset = direccion.split("()");  // n   R2
            System.out.println("n:"+offset[0]+" r:"+offset[1]);

            int n = Integer.parseInt(offset[0]);
            int r = Integer.parseInt(offset[1]);
                
            direccionDato = contexto.registros[r] + n;

            return direccionDato;
    }

    // Recibe la cache de datos, la direccion virtual, el contexto del hilillo y el string del hilillo
    static void ejecutarLectura(int[][] cacheDatos, int direccion, Contexto contexto, String hilillo) {
        // Settea en el registro del contexto el valor leido desde la cache 
        String [] sepHilo = hilillo.split(" ");
        String registro2 = sepHilo[2]; // String que representa el registro destino
        int r2 = Integer.parseInt(registro2);
        int numBloque = (direccion-640)/16;     // Este es el numero de bloque de memoria (De 0 a 87)
        int numPalabra = (direccion-640)/4%4;   // Primero se saca la direccion fisica ((direccion-640)/4)  ----  
        contexto.registros[r2] = cacheDatos[numBloque%8][numPalabra]; // Notar el %8 que permite hacer el mapeo directo
    }
    
    static void ejecutarEscritura(int[][] cacheDatos, int direccion, Contexto contexto, String hilillo){
        String [] sepHilo = hilillo.split(" ");
        String registro2 = sepHilo[2];
        int r2 = Integer.parseInt(registro2);
        int numBloque = (direccion-640)/16;
        int numPalabra = (direccion-640)/4%4;
        cacheDatos[numBloque][numPalabra] = contexto.registros[r2];     
    }
    
}
