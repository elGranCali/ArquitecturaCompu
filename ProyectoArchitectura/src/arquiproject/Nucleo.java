/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CALI
 */
public class Nucleo extends Thread {
    private final CyclicBarrier lock;
    private String id;
    private boolean hayTrabajo;
    public boolean ocupado;
    public Contexto contexto;
    int [] registros; 
    int bloquesEnCache = 8; 
    int [][] cacheInstrucciones = new int[8][17];
    
    
    
    public Nucleo(CyclicBarrier lock, String id){
        
        this.lock = lock;
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
            lock.await();
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
    
    // es necesario que del hilo maestro conozca ya quantum, b y m. No le entrarían por 
    // parametros, era solo para que no diese error usar las variables. pc tampoco.  
    void procesar (int quantum, int b, int m, int pc) {
        int tiempoTrayendoBloque = 4*(b+m+b);
        int direccion = pc; // dirección apartir de la cuál leer la instruccion
        int numBloque = direccion/16;
        int numPalabra = direccion%16;
        // grupo de variables que realmente no son variables bus -> lock cacheMiss -> metodo etc
        boolean busBusy = false; 
        boolean cacheMiss = false; 
        boolean terminado = false;
        while (quantum != 0)  {
            if (estaEnCache(numBloque)) {  // la instrucción está en cache?
                registros = Decodificador.decodificacion("8 1 4 9", registros);

                if (cacheMiss) {
                    int e = tiempoTrayendoBloque;   // calculo de ciclos e
                    if (busBusy) {
                       //avanzarReloj();
                    } else {  // ya puedo leer memoria y traer bloque
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
                    }
                }else {
                    quantum--;
                    pc = pc+4;
                    terminado = true; 
                    //avanzarReloj();
                }
            } else {  // Calcular los ciclos que conlleva traer bloque de memoria
                int c = tiempoTrayendoBloque; 
                if (busBusy) {
                    //avanzarReloj();
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
                        //avanzarReloj();
                    } 
                    // Al salir de aquí ya cargó el bloque a cache
                }
            }
        }
        //GuardarContexto();
    }
    
    
    @Override
    public void run(){
        while (hayTrabajo) { // Seria hasta que ya no exista trabajo
            System.out.println("Núcleo " + id + " iniciando el ciclo");
        
            // AQUI SE MANEJARIA LA LOGICA DE CADA NUCLEO
            // llamar a metodo procesar creado arriba, aun esta en desarrollo
            
            try {
                avanzarReloj();
            } catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Núcleo " + id + " terminado ciclo");  
        }
    }        
}
