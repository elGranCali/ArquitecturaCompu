/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Pc
 */
public class HiloMaestro {
    static int totalInstrucciones = 0; 
    static int cantidadMemInstrucciones = 639; 
    static int [] memoriaInstrucciones = new int[639];
    static int M = 2;
    static int PC = 2;
    private int ciclo;
    private static final CyclicBarrier lock = new CyclicBarrier(3);
    private static final Object lock2 = new Object();
    Nucleo n1;
    Nucleo n2; 
    private Queue<Contexto> cola = new ConcurrentLinkedQueue<Contexto>();
    

    private void iniciarHilos(){
        n1.start();
        n2.start();
    }
    
    private boolean hayTrabajo(){
        return !cola.isEmpty();
    }
    
    
    private void cargarHilos(){
        // Parte que habria q subir los hilos y darles un contexto
        agregarContexto(new Contexto());
        agregarContexto(new Contexto());
    }
    
    public void agregarContexto(Contexto nuevoContexto){
        if (cola.add(nuevoContexto)) {
            System.out.println("Contexto agregado");
        }
    }
    
    
    public void modificarVariableInstancia() {
            synchronized(lock2){
            try {
                lock2.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
            n1.trabajoActual = false;
        }
    }
    
    public void iniciar(){
        iniciarHilos(); // Siempre ejecutaran primero el wait
        cargarHilos();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        while(hayTrabajo()) {     
            try {
                new Scanner(System.in).nextLine();
                System.out.println("El ciclo actual es " + ciclo);
                lock.await();
                ciclo++;
            } catch (InterruptedException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(HiloMaestro.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
        
     
    public String ReadFile(String filename){
        String line = null;
        try {
            File myfile = new File(filename);
            FileReader filereader = new FileReader(myfile);
            BufferedReader  reader = new BufferedReader(filereader);
            
            while((line = reader.readLine()) != null) {
                String [] single = line.split(" ");
                for (int i=0; i < single.length; i++){
                    int number = Integer.parseInt(single[i]);
                    memoriaInstrucciones[totalInstrucciones] = number;
                    totalInstrucciones++; 
                }
                //System.out.println(line);
            }
            imprimirMemoria();
            
        }catch (Exception e) {
            
        }
        return line;
    }
    
    public static void initMemoriaInstrucciones() {
        for (int i=0; i < cantidadMemInstrucciones; i++){
            memoriaInstrucciones[i] = 0;
        }
    }
    
    public void imprimirMemoria() {
        String cajita = "";
        for (int i=0; i < cantidadMemInstrucciones; i++){
            cajita += "["+memoriaInstrucciones[i]+"] , ";   
        }
        System.out.println(cajita);
    }
    
    public static void decodificacion(String instruccion){
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
        int[] registros; 
        registros = new int[33];
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
        for(int i = 0; i<33; i++){
            registros[i]=i;
        }
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
            case "35":
                registros[r2]= M*(r3+registros[r1]);
                System.out.print(registros[r2]);
                break;
            /*case "43":
                break;*/
            /*case "4":
                if(registros[r1]==0)                                
                break;*/
            /*case "5":
                if(registros[r1]!= 0)
                break;*/
            case "3":
                registros[31] = PC;
                PC += r3;
                System.out.print("PC="+PC+" R31="+registros[31]);
                break;
            case "2":
                PC = registros[r1];
                System.out.print("PC="+PC);
                break;
            case "11":
                registros[r2] = M*(r3+registros[r1]);
                registros[32] = r3+registros[r1];
                break;
            case "17": //verificar este codOp porque está repetido, era originalmente 12
                if(registros[32] == r3+registros[r1]){
                    //no entiendo donde se guarda registros[r2]
                }else{
                    registros[r2] = 0;
                }
                break;
            case "63":
                System.exit(0);
                break;
            default:
                break;
        }
    }
    
     public HiloMaestro() {
        ciclo = 1;
        n1 = new Nucleo(lock, "uno", lock2);
        n2 = new Nucleo(lock, "dos", lock2); 
    }
    
    
     /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        initMemoriaInstrucciones();
        java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
                new interfaz().setVisible(true);
            }
        });
        decodificacion("8 1 4 9");
        HiloMaestro maestro = new HiloMaestro();
        maestro.iniciar();
    }
    
} // Final de clase
