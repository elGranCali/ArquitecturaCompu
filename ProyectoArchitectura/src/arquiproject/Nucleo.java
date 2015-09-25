/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentLinkedQueue; 
import java.util.Queue;

/**
 *
 * @author CALI
 */
public class Nucleo extends Thread {
    private final CyclicBarrier barreraLock;
    public String id;
    private boolean hayTrabajo;
    public boolean ocupado;
    public Contexto contexto;
    int [] registros; 
    int bloquesEnCache = 8; 
    int [][] cacheInstrucciones = new int[8][17];
    boolean esFin = false;
    private Lock lockFin;
    public static int q = 20;
    public static int m = 2;
    public static int b = 2;
    private ConcurrentLinkedQueue cola;
    
    public Nucleo(CyclicBarrier lock, String id, Lock lockFin, ConcurrentLinkedQueue cola){
        this.cola = cola;
        this.barreraLock = lock;
        this.lockFin = lockFin;
        this.id = id;
        hayTrabajo = true;
        ocupado = true;
        registros = new int[33];
        for(int i = 0; i<33; i++){
            registros[i]=0;
        }
    }
    
    public void setContexto(Contexto contexto) {
        this.contexto = contexto;
    }
    
    public void setEstado(boolean hayTrabajo){
        this.hayTrabajo = hayTrabajo;
    }
    
    // Este método puede ser llamado en cualquier momento que se termine un ciclo
    private void avanzarReloj() throws InterruptedException{
        try {
            barreraLock.await();
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    boolean estaEnCache(int numBloque){
    // revisamos si existe la tag en la columna 16 de la matriz de cache igual a numBloque
        for(int i=0; i < 8 ; i++) {
            for(int j=16; ; j++) {
                if (cacheInstrucciones[j][i] == numBloque) {
                    return true;    
                } 
            }
        }
        return false; 
    }
    
    String leerInstruccionDeCache(int numBloque, int numPalabra) {
        String hilillo = "";
        int op = cacheInstrucciones[numBloque][numPalabra];
        int reg1 = cacheInstrucciones[numBloque][numPalabra+1];
        int reg2 = cacheInstrucciones[numBloque][numPalabra+2];
        int result = cacheInstrucciones[numBloque][numPalabra+3];
        hilillo = op+" "+reg1+" "+reg2+" "+result; 
        return hilillo;
    }
    
    // es necesario que del hilo maestro conozca ya quantum, b y m. No le entrarían por 
    // parametros, era solo para que no diese error usar las variables. pc tampoco.  
    void procesar () {
        
        int tiempoTrayendoBloque = 4*(b+m+b);
        int pc = contexto.PC; // dirección apartir de la cuál leer la instruccion
        int numBloque = pc/16;
        int numPalabra = pc%16;
        // grupo de variables que realmente no son variables bus -> lock cacheMiss -> metodo etc
        boolean busBusy = false; 
        boolean completadoEnEsteCiclo = true; 
        while (q != 0)  {
            if (estaEnCache(numBloque)) {  // la instrucción está en cache?
                // leer instruccion de cache 
                String hilillo = leerInstruccionDeCache(numBloque, numPalabra);
                registros = Decodificador.decodificacion(hilillo, registros);
                
                if (!completadoEnEsteCiclo) {
                    /*int e = tiempoTrayendoBloque;   // calculo de ciclos e
                    while (busBusy) {
                       
                        try {
                            avanzarReloj();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                    }
                    // ya puedo leer memoria y traer bloque
                    int nuevoNumBloque = numBloque%bloquesEnCache;
                    for (int j=0; j<4; j++) {  // 4 palabras
                        for (int i=0; i<4; i++) {  // 4 campitos de 1 palabra
                            cacheInstrucciones[nuevoNumBloque][j*4+i]= HiloMaestro.leerMemoria(pc*j+i);
                        }
                    }
                    if (e != 0){
                        e--;
                        //avanzarReloj();
                    }  // Al salir de aquí ya cargó el bloque a cache
                    */
                }else {
                    q--;
                    pc = pc+4;
                    lockFin.lock();
                    esFin = Decodificador.esFin(hilillo);
                    lockFin.unlock();
                    try {
                        avanzarReloj();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {  // Calcular los ciclos que conlleva traer bloque de memoria
                int c = tiempoTrayendoBloque; 
                if (busBusy) {
                    try {
                        avanzarReloj();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    int nuevoNumBloque = numBloque%bloquesEnCache;  // averiguar donde colocar bloque
                    // traer de memoria las 4 palabras del bloque 
                    for (int j=0; j<4; j++) {  // 4 palabras
                        for (int i=0; i<4; i++) {  // 4 campitos de 1 palabra
                            cacheInstrucciones[nuevoNumBloque][j*4+i]= HiloMaestro.leerMemoria(pc*j+i);
                        }
                    }
                    if (c != 0){
                        c--;
                        try {
                            avanzarReloj();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } 
                    // Al salir de aquí ya cargó el bloque a cache
                }
            }
        }//final de while
        //GuardarContexto();
        contexto.PC = pc;
        contexto.registros = registros;
        cola.add(contexto);
    }
    
    
    @Override
    public void run(){
        while (hayTrabajo) { // Seria hasta que ya no exista trabajo
            System.out.println("Núcleo " + id + " iniciando el ciclo");
        
            // AQUI SE MANEJARIA LA LOGICA DE CADA NUCLEO
            // llamar a metodo procesar creado arriba, aun esta en desarrollo
            //procesar(2,2,2,0);
            try {
                avanzarReloj();
            } catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Núcleo " + id + " terminado ciclo");  
        }
    }        
}
