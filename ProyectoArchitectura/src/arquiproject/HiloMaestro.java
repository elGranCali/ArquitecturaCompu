/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue; 
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 *
 * @author Pc
 */
public class HiloMaestro {
    static int totalInstrucciones = 0;
    public static int hilosAprocesar = 0;
    static int cantidadMemInstrucciones = 640; 
    static int cantidadMemDatos = 352;
    static int [] memoriaInstrucciones = new int[640];
    static int [] memoriaDatos = new int [352];
    static int n1Invalidor = -1;
    static int n2Invalidor = -1; 
    static int [] n1LLactivo = new int [2];
    static int [] n2LLactivo = new int [2];
    private int ciclo;
    private static final CyclicBarrier lock = new CyclicBarrier(3);
    int quantumCiclos;
    int memoriaCiclos;
    int busCiclos; 
    private final Lock lockOcupado1 = new ReentrantLock();
    private final Lock lockOcupado2 = new ReentrantLock();
    private int inicioHilo = 0;
    private final static Semaphore busInstrucciones = new Semaphore(1);
    private final static Semaphore busDatos = new Semaphore(1);
    static Nucleo n1;
    static Nucleo n2; 
    /*
    Nucleo n1;
    Nucleo n2; 
    */
    private final ConcurrentLinkedQueue<Contexto> cola = new ConcurrentLinkedQueue<>();
    public static final ConcurrentLinkedQueue<Contexto> colaDeTerminados = new ConcurrentLinkedQueue<>();
    public static boolean stepByStep = false;
    public static javax.swing.JTextArea textArea;
    
    
   public static boolean attemptAccess() {
       return busInstrucciones.tryAcquire();
   }
   
   public static boolean releaseAccess() {
       busInstrucciones.release();
       return true;
   }
    
   public static boolean pedirBusDatos() {
       return busDatos.tryAcquire();
   }
   
   public static boolean soltarBusDatos() {
       busDatos.release();
       return true;
   }
   
    public static synchronized boolean hayTrabajo(){
        System.out.println("Hilos a procesar son: "+ hilosAprocesar);
        return !(hilosAprocesar==0);
    }
   
    public static synchronized void terminarHilo(){
        hilosAprocesar--;
    }
    
    public boolean nucleoVacio(Nucleo n) {
        
        if (n.id.equals("uno") ){
            lockOcupado1.lock();
            try {
                if (n.ocupado == false) {
                    //n.esFin = false;
                    return true;
                }
            } finally {
                lockOcupado1.unlock();
            }
            return false; 
        } else {
            lockOcupado2.lock();
            try {
                if (n.ocupado == false) {
                    //n.esFin = false;
                    return true;
                }
            } finally {
                lockOcupado2.unlock();
            }
            return false;
        } 
    }
    
    private void asignarContexto(Nucleo nucleo) {
        if (!nucleo.ocupado && !cola.isEmpty()) {
            nucleo.setContexto(cola.remove());
        }
    }
    
    
    // Se inician los hilos y se ejecutan el asignar contexto
      private void iniciarHilos(){
        System.out.println("Se inician los hilos (?)");
        n1.start();
        n2.start();
    }
    
    public String iniciar(){
        // debería preguntarse ademas de q si el nucleo esta ocupado.. de que si hay un contexto para cada nuclio
        // revisar q en la cola hayan hilos para asignar 2!! 
        asignarContexto(n1);
        asignarContexto(n2);
        initMemoriaDatos();
        // simplemenete inicializando en ceros el vectorsito de LL de cada nucleo
        n2LLactivo[0] = 0;  // 0 no hay LL .... 1 si hay un LL activo
        n2LLactivo[1] = 0;  // 1 numBloque extraido de la direccion del LL 
        n1LLactivo[0] = 0;
        n1LLactivo[1] = 0;
        iniciarHilos(); 
        while(true) {
            //System.out.println("Avance hilo maestro"); 
            try {
                if (nucleoVacio(n1)) {
                    asignarContexto(n1);      
                }
                if (nucleoVacio(n2)) {
                    asignarContexto(n2);    
                }
                if (hayTrabajo()) {
                    lock.await(3,TimeUnit.DAYS);
                } else {
                    if (lock.getNumberWaiting() != 0) {
                        lock.reset();
                    }
                    break;
                }
                if (n2Invalidor != -1){
                    //mandarainvalidar el numero de bloque q tenga eln2Invalidor en cache 2
                    n2.invalidarBloque(n2Invalidor);
                    
                } 
                if (n1Invalidor != -1) {
                    //mandarainvalidar el numero de bloque q tenga eln1Invalidor en cache 1
                    n1.invalidarBloque(n1Invalidor);
                    
                }
                
                /*
                    para lo de avisar la activacion del LL por cada nucleo, al igual que colocar nuevamente en ceros 
                    el vector de cada nucleo, se realiza en los 2 ultimos metodos de esta clase. 
                
                */
                
                if (n1LLactivo[0] == 1) { // si en el n1 hay un LL activo 
                    
                    if (n1LLactivo[1] == n2Invalidor){  // si el objetivo de invalidacion del n2 es igual al objetivo del LL activo 
                        n1.contexto.RL = -1; 
                        n1.contexto.registros[1] = 0;   // segun doc tambn debe ponerse en el reg destino un 0
                        // volver a dejar en 0's el vectorsito
                        n1LLactivo[0] = 0;
                        n1LLactivo[1] = 0; 
                    }
                }
                // mismo caso pero con n2
                if (n2LLactivo[0] == 1) { // si en el n2 hay un LL activo 
                    if (n2LLactivo[1] == n1Invalidor){
                        n2.contexto.RL = -1; 
                        n2.contexto.registros[1] = 0; // segun doc tambn debe ponerse en el reg destino un 0
                        // volver a dejar en 0's el vectorsito
                        n2LLactivo[0] = 0;
                        n2LLactivo[1] = 0; 
                    }
                }
                n1Invalidor =-1;
                n2Invalidor =-1;
                n2LLactivo[0] = 0;
                n2LLactivo[1] = 0; 
                n1LLactivo[0] = 0;
                n1LLactivo[1] = 0; 
                ciclo++;
                
            } catch (InterruptedException ex) {
                System.out.println("Interrupcion");
            } catch (BrokenBarrierException ex) {
                System.out.println("Se rompio la barrera");
            } catch (TimeoutException ex) {
                System.out.println("Se acabo el tiempo");
            } 
            
        }
        String ans = "EL CICLO ES: " + ciclo + "\n";
        //n1.imprimirCacheDatos(); 
        // Este es un delay para esperar que los hilillos se guarden en la cola de terminos y puedan ser impresos
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            System.out.println("Fallo en el delay de la cola de terminados");
        }
        while (colaDeTerminados.peek() != null) {
            ans += colaDeTerminados.poll().toString();
        }
        //imprimirMemDatos();
        return ans;
    }
    
    
    public String MemToString() {
        String ans = "";
        
        // parte oara memoria de instrucciones
        /*int bloques = 0; // llegar a 127
        int auxBloques = 0; // llegar a 16
        ans = "Memoria Instrucciones:\n";
        ans += "\nB"+Integer.toString(bloques)+":";
        bloques++; 
        for (int i=0; i < cantidadMemInstrucciones; i++){
            if (auxBloques  == 16) {
                ans += "\nB"+Integer.toString(bloques)+":";
                bloques++; 
                auxBloques = 0; 
            }
            auxBloques++;
            ans += "["+memoriaInstrucciones[i]+"] , ";   
        }*/
        
        // parte para memoria de datos 
        
        int bloquesD = 0; 
        int bloquePorReglon = 1; // llegar a 3 por reglon
        ans = "\nMemoria Datos:\n";
        ans += "\n B"+Integer.toString(bloquesD+40)+":";
        bloquesD++; 
        int auxBloquesD = 0; // llegar a 4
        
        for (int i=0; i < cantidadMemDatos; i++){
           if (bloquePorReglon == 2 && auxBloquesD == 4) {
                ans += "\n";
                bloquePorReglon=0;
            } 
            if (auxBloquesD == 4) {
                ans += "  B"+Integer.toString(bloquesD+40)+":";
                bloquesD++; 
                bloquePorReglon++;
                auxBloquesD = 0; 
            }
            auxBloquesD++; 
            ans += "["+memoriaDatos[i]+"] , ";   
            
        }
       
        
        // parte caches
        
        
        ans += "\n\n Cache Datos de Nucleo 1 \n";
        for (int i=0; i < 8; i++){
            for (int j=0; j < 6; j++) {
                 ans += "["+n1.cacheDatos[i][j]+"] , ";
            }
            ans += "\n";
        }
        ans += "\n Cache Datos de Nucleo 2 \n";
        for (int i=0; i < 8; i++){
            for (int j=0; j < 6; j++) {
                 ans += "["+n2.cacheDatos[i][j]+"] , ";
            }
            ans += "\n";
        }
        
        return ans; 
    }
        
    /* Evento que se dispara cuando se seleccionan los hilos 
    * a correr (Carga de archivos)
    */
    public String ReadFile(String filename){
        String line = null;
        inicioHilo = inicioHilo * 4; 
        System.out.println("Inicio de hilo: "+totalInstrucciones); 
        cola.add(new Contexto(totalInstrucciones, hilosAprocesar+1));
        try {
            File myfile = new File(filename);
            FileReader filereader = new FileReader(myfile);
            BufferedReader  reader = new BufferedReader(filereader);
            
            while((line = reader.readLine()) != null) {
                String [] instrucciones = line.split(" ");
                for (String instruccion : instrucciones) {
                    int number = Integer.parseInt(instruccion);
                    memoriaInstrucciones[totalInstrucciones] = number;
                    totalInstrucciones++; 
                }
                inicioHilo++;                
            }
            System.out.println("Archivo "+myfile.getName()+" procesado.");
            System.out.println("Contexto agregado");
        }catch (IOException | NumberFormatException e) {
            System.out.println("Error al leer archivo");
        }
        hilosAprocesar++;
        return line;
    }
    
    public static void initMemoriaInstrucciones() {
        for (int i=0; i < cantidadMemInstrucciones; i++){
            memoriaInstrucciones[i] = 0;
        }
    }
    
    public static void initMemoriaDatos() {
        for (int i=0; i < cantidadMemDatos; i++) {
            memoriaDatos[i] = 1;
        }
    }
	
    public static void imprimirMemoria() {
        String cajita = "Memoria Instrucciones:\n";
        for (int i=0; i < cantidadMemInstrucciones; i++){
            cajita += "["+memoriaInstrucciones[i]+"] , ";   
        }
        System.out.println(cajita);
    }
	
    public static void imprimirMemDatos() {
    String cajita = "Memoria Datos:\n";
    for (int i=0; i < cantidadMemDatos; i++){
        cajita += "["+memoriaDatos[i]+"] , ";   
    }
    System.out.println(cajita);
    }
    
    public static int leerMemoria(int posicion) {
        int valor = memoriaInstrucciones[posicion];
        return valor; 
    }
	
    public static int leerMemoriaDatos(int posicion) {
        int valor = memoriaDatos[posicion];
        return valor;
    }
    
    public static void escribirMemoriaDatos(int posicion, int valor) {
        memoriaDatos[posicion] = valor;
    }
    
    public void setStepByStep(boolean step){
        stepByStep = step;
    }
    
    public void setTextField(javax.swing.JTextArea jTextArea1){
        HiloMaestro.textArea = jTextArea1;
    }
    
    public void setParametrosCiclos(int q, int m, int b) {
        quantumCiclos = q;
        memoriaCiclos = m;
        busCiclos = b;
        Nucleo.QUAMTUM = q; 
        Nucleo.m = m;
        Nucleo.b = b; 
        System.out.println("q="+quantumCiclos+" m="+memoriaCiclos+" b="+busCiclos); 
    }
    
    // Metodo que permite pedir la cache del otro nucleo,usando al hilo
    // maestro como intermediario
    public static boolean pedirCacheDelOtroNucleo(String idNucleo) {
        if( idNucleo.equalsIgnoreCase("uno")){
            return n2.pedirCache();
        }else{
            return n1.pedirCache();
        }
    }

    public static void setEstadoEnOtraCache(String idNucleo, int bloque, int estado) {
         if( idNucleo.equalsIgnoreCase("uno")){
            n2.setEstadoBloque(bloque, estado);
        }else{
            n1.setEstadoBloque(bloque, estado);
        }
    }

    public HiloMaestro() {
        ciclo = 1;
        n1 = new Nucleo(lock, "uno", lockOcupado1, cola, colaDeTerminados);
        n2 = new Nucleo(lock, "dos", lockOcupado2, cola, colaDeTerminados); 
        System.out.println("Se crean los 2 nucleos");      
    }
     
    // OK Recibe el bloque de cache y el bloque de memoria que quiere ser escrito (0 a 87)
    public static void escribirEnMemoria(int [] bloque, int numBloqueMemoria) {
        int direccionFisicaBloque = numBloqueMemoria*4;
        System.arraycopy(bloque, 0, memoriaDatos, direccionFisicaBloque, Nucleo.PALABRASPORBLOQUE);
    }
    
    public static int [] leerDesdeMemoria(int direccion) {
        int [] bloque = new int[4];
        int numBloque = (direccion -640)/ 16;
        int direccionFisica = numBloque*4;
        for (int i = 0 ; i < Nucleo.PALABRASPORBLOQUE; i++) {
            bloque[i] = memoriaDatos[direccionFisica+i];
        }
        return bloque;
    }
    
    
     public static int [] leerDesdeLaOtraCache(String nucleoFuente, int numBloqueDatoM) {
        if( nucleoFuente.equalsIgnoreCase("uno")){
            return n2.getBloque(numBloqueDatoM%8);
        }else{
            return n1.getBloque(numBloqueDatoM%8);
        }
    }
    
    public static int snooping (String nucleoFuente, int numBloqueDatoM) {
        if( nucleoFuente.equalsIgnoreCase("uno")){
            return n2.cacheHit(numBloqueDatoM);
        }else{
            return n1.cacheHit(numBloqueDatoM);
        }
    }
    
    
    // metodos utilizados en SW  
    public static void subirBloqueCacheVecinaAMemDatos(String nucleoFuente, int numBloqueDatoM){
        if( nucleoFuente.equalsIgnoreCase("uno")){
             n2.subirBloqueMemDatos(numBloqueDatoM);
        }else{
             n1.subirBloqueMemDatos(numBloqueDatoM);
        }
    }
    
     public static void subirBloqueCacheVecinaACacheNeedly(String nucleoFuente, int numBloqueDatoM){
        int [] bloque = new int[4];
        if( nucleoFuente.equalsIgnoreCase("uno")){
             bloque = n2.compartirBloque(numBloqueDatoM, bloque);
             n1.actualizarBloque(numBloqueDatoM, bloque);
        }else{
             bloque = n1.compartirBloque(numBloqueDatoM, bloque);
             n2.actualizarBloque(numBloqueDatoM, bloque);
        }
    }
    
    public static void liberarCacheVecina (String nucleoFuente) {
        if( nucleoFuente.equalsIgnoreCase("uno")){
            n2.liberarCache();
        }else{
            n1.liberarCache();
        }
    }
    
    public static void avisarInvalidacion(int numBloque, String nucleoFuente) {
        if( nucleoFuente.equalsIgnoreCase("uno")){
            // invalidar el numbloque de la cache 2 
            n2Invalidor = numBloque;
        }else{
            // invalidar el numbloque de la cache 1
            n1Invalidor = numBloque;  
        }
    }
    
     public static void avisarLL(int numBloque, String nucleoFuente) {
        System.out.println("coloco en array un 1 de LL activo en "+nucleoFuente+" con numbloque"+numBloque);
         if( nucleoFuente.equalsIgnoreCase("uno")){
            n1LLactivo[0] = 1; // cuando es un 1 es q hay un LL activo en el n1
            n1LLactivo[1] = numBloque;
        }else{
            n2LLactivo[0] = 1;  // sino es en el nucleo 2
            n2LLactivo[1] = numBloque;  
        }
    }

     public static void avisarLLFallido(String nucleoFuente) {
        System.out.println("limpio array de nucleo "+nucleoFuente);
         if( nucleoFuente.equalsIgnoreCase("uno")){
            n1LLactivo[0] = 0;
            n1LLactivo[1] = 0;
        }else{
            n2LLactivo[0] = 0;  
            n2LLactivo[1] = 0;  
        }
    }
     
} // Final de clase
