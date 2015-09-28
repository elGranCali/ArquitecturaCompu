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
    
    void procesar () {
        
        int tiempoTrayendoBloque = 4*(b+m+b);
        System.out.println("El PC actual es: "+contexto.PC); 
        int pc = contexto.PC; // dirección apartir de la cuál leer la instruccion
        System.out.println("El PC actual es: "+pc); 
        int numBloque = pc/16;
        int numPalabra = pc%16;

        boolean busBusy = false;              
        
        boolean completadoEnEsteCiclo = true;   // Para la primera entrega siempre es true
        
        while (q != 0)  {
            if (estaEnCache(numBloque)) {       // la instrucción está en cache?
                System.out.println("Instruccion esta en cache"); 
                String hilillo = leerInstruccionDeCache(numBloque, numPalabra);
                System.out.println("Hilillo actual: "+hilillo); 
                registros = Decodificador.decodificacion(hilillo, registros);
                if (!completadoEnEsteCiclo) {   // 2da Entrega
                    // es una operación SW o LW pues dura mas de un ciclo
                    // Volver a calcular los ciclos de espera, pedir el bus, leer memoria, avanzar reloj cuando termine espera
                }else {
                    q--;                        // Disminuimos Quatum 
                    pc = pc+4;                  // Aumentamos pc 
                    lockFin.lock();             // Bloqueamos para escribir "esFin", que nos dice si es hora de meter otro hilo en el procesador
                    esFin = Decodificador.esFin(hilillo);
                    System.out.println("Variable esFin esta en "+esFin); 
                    lockFin.unlock();
                    
                    // Avanzar reloj 
                    try {
                        avanzarReloj();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {    // Instruccion no esta en cache 
                int c = tiempoTrayendoBloque; 
                if (busBusy) {
                    // Avanzar reloj hasta que bus esté desocupado
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
                    // mientras que la cantidad de ciclos nueva no termine 
                    if (c != 0){
                        c--;    // Solo avance reloj 
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
        
        // Guardar el contexto actual
        contexto.PC = pc;
        contexto.registros = registros;
        // Como aun no termina, se mete el contexto a la cola para luego volver a procesarlo
        cola.add(contexto);
    }
    
    
    @Override
    public void run(){
        while (hayTrabajo) {    // Seria hasta que ya no exista trabajo
            System.out.println("Núcleo " + id + " iniciando el ciclo");
            // logica ?? 
            System.out.println("Ver si entra en el run del nucleo");
            try {
                avanzarReloj();
            } catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Núcleo " + id + " terminado ciclo");  
        }
    }        
}
