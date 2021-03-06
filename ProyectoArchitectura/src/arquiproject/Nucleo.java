/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arquiproject;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.ConcurrentLinkedQueue; 
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author CALI
 */
public class Nucleo extends Thread {
    private final CyclicBarrier barreraLock;
    public String id;
    public boolean ocupado;
    public Contexto contexto;
    int bloquesEnCacheI = 8; 
    public static final int NUMERODEBLOQUESENCACHE = 8; 
    public static final int PALABRASPORBLOQUE = 4;
    int [][] cacheInstrucciones = new int[8][17];
    int [][] cacheDatos = new int[8][6]; // COLUMNA 4 sera numBloque  y 5 estado -1: invalido 0: compartido 1: modificado 
    boolean esFin = false;
    private final Lock lockOcupado;
    private final Semaphore lockCache = new Semaphore(1);
    public static int QUAMTUM = 20;
    public static int m = 2;
    public static int b = 2;
    private final ConcurrentLinkedQueue<Contexto> cola;
    private final ConcurrentLinkedQueue<Contexto> coladeTerminados;
    public static boolean cacheDatosEnUso = false; 
		
    public Nucleo(CyclicBarrier lock, String id, Lock lockFin, ConcurrentLinkedQueue<Contexto> cola, ConcurrentLinkedQueue<Contexto> coladeTerminados){
        contexto = null;
        this.cola = cola;
        this.coladeTerminados = coladeTerminados;
        this.barreraLock = lock;
        this.lockOcupado = lockFin;
        this.id = id;
        ocupado = false;
        // inicializacion de la cache en -1's
        for (int i=0; i < 8; i++){
            for (int j=0; j < 17; j++) {
                cacheInstrucciones[i][j] = -1;
            }
        }
	for (int i=0; i < 8; i++){
            for (int j=0; j < 6; j++) {
                cacheDatos[i][j] = -1;
            }
        }  
        // PRUEBA PARA VER EL WRITE BACK DE UN BLOQUE DE CACHE
        /* cacheDatos[24%8][0] = 999;
        cacheDatos[24%8][1] = 999;
        cacheDatos[24%8][2] = 999;
        cacheDatos[24%8][3] = 999;
        cacheDatos[24%8][4] = 24;
        cacheDatos[24%8][5] = 1;    // Se settea como modificado 
        */
        /*
        Prueba de si el bloque esta en la otra y esta modificado snooping=
        if (id.equals("dos")){
            cacheDatos[7][0] = 999;
            cacheDatos[7][1] = 999;
            cacheDatos[7][2] = 999;
            cacheDatos[7][3] = 999;
            cacheDatos[7][4] = 23;
            cacheDatos[7][5] = 1;    // Se settea como modificado
        } */
        
    }
    
        // Este método puede ser llamado en cualquier momento que se termine un ciclo
    private void avanzarReloj() throws InterruptedException{
        if(HiloMaestro.stepByStep){
            if(this.contexto != null){
                HiloMaestro.textArea.append(this.contexto.toString());
                //Thread.sleep(3000);
            }
        }
        try {
            barreraLock.await();
        } catch (BrokenBarrierException ex) {
            System.out.println("Se reseteo la barrera");
        }
    }
    
   public boolean pedirCache() {
       return lockCache.tryAcquire();
   }
   
   public void liberarCache() {
       lockCache.release();  
   }
    
     public void imprimirCache() {
        String cajita = "Cache Instrucciones de Nucleo "+ id +"\n";
        for (int i=0; i < 8; i++){
            for (int j=0; j < 17; j++) {
                 cajita += "["+cacheInstrucciones[i][j]+"] , ";
            }
        }
        System.out.println(cajita);
    }
	
	public void imprimirCacheDatos() {
        String cajita = "Cache Datos de Nucleo "+ id +"\n";
        for (int i=0; i < 8; i++){
            for (int j=0; j < 6; j++) {
                 cajita += "["+cacheDatos[i][j]+"] , ";
            }
            cajita += "\n";
        }
        System.out.println(cajita);
    }
    
    public void reiniciarQuatum(){
        
    }
    
    public void setContexto(Contexto contexto) {
        this.contexto = contexto;
        lockOcupado.lock();
        try {
            ocupado = true;
        } finally {
            lockOcupado.unlock();
        }
    }
      
    boolean estaEnCache(int numBloque){
        // revisamos si existe la tag en la columna 16 de la matriz de cache igual a numBloque        
        boolean result = false; 
        for (int i=0; i<8; i++) {
            if (cacheInstrucciones[i][16] == numBloque) {
                result = true; 
            } 
        }
        return result; 
    }
    
    	
    boolean bloqueDatosModificado (int numBloque){       
        boolean result = false; 
        if (cacheDatos[numBloque][5] == 1) {
            result = true; 
         }      
        return result; 
    }
    
    void imprimirTags() {
        String cajita = "Tags Cache nucleo: " +id+":\n";
        for (int i=0; i<8; i++) {
            cajita += "[" + cacheInstrucciones[i][16] + "],";  
        }
         System.out.println(cajita);
    }
	void imprimirTagsDatos() {
        String cajita = "Tags Cache Datos nucleo: " +id+":\n";
        for (int i=0; i<8; i++) {
            cajita += "[" + cacheDatos[i][4] + "],";  
        }
         System.out.println(cajita);
        
    }
    
    String leerInstruccionDeCache(int numBloque, int numPalabra) {
        String hilillo;
        int op = cacheInstrucciones[numBloque][numPalabra];
        int reg1 = cacheInstrucciones[numBloque][numPalabra+1];
        int reg2 = cacheInstrucciones[numBloque][numPalabra+2];
        int result = cacheInstrucciones[numBloque][numPalabra+3];
        hilillo = op+" "+reg1+" "+reg2+" "+result; 
        return hilillo;
    }
	
    public int [] getBloqueCache( int numBloqueC) {
        int [] bloque = new int [4];
        System.arraycopy(cacheDatos[numBloqueC], 0, bloque, 0, PALABRASPORBLOQUE);
        return bloque;
    }
    
	
    public void procesarQuantum () throws InterruptedException {
        imprimirCacheDatos(); 
        int tiempoTrayendoBloque = 4*(b+m+b);
        int direccionDato = 0;
        boolean agregarATerminados = false;
        int q = QUAMTUM;
        
        while (q != 0)  {       // Ejecución de todas las instrucciones que comprenden un quantum
            System.out.println("El PC actual del nucleo " + id.toUpperCase() + " con el "+ contexto.id+ " es: "+contexto.PC);       
            
            int numBloque = contexto.PC/16; // Bloque en memoria
            int numPalabra = contexto.PC%16; // busca palabra
 
            int nuevoNumBloque = numBloque%bloquesEnCacheI;  // averiguar donde colocar bloque
            if (estaEnCache(numBloque)) {       // la instrucción está en cache?
                System.out.println("Nucleo: " + id + " Bloque "+ numBloque + " esta en cache"); 
                System.out.println("Buscando en cache el nuevo bloque: " + nuevoNumBloque + " Palabra "+ numPalabra); 
                
                String hilillo = leerInstruccionDeCache(nuevoNumBloque, numPalabra);
                Decodificador.decodificacion(hilillo, contexto);
                System.out.println("Instruccion "+ hilillo +" del nucleo " + id.toUpperCase() + " con el " + contexto.id);
                
                int tipo = Decodificador.instruccionMemoria(hilillo);
                int complete = 0;
                if (hilillo.equals("51 0 1 1988")) {
                    int m = 3; 
                }
                if (tipo > 0) {   // 2da Entrega
                    direccionDato = Decodificador.getDireccion(hilillo, contexto);
                    int numBloqueDatoM = (direccionDato-640)/16; // Bloque en memoria de datos numerados de 0 a 87                  
                    if (tipo == 35 || tipo == 50) {  // Se ejecuta el LW o LL si el parametro enviado es true
                        if (tipo == 50) {
                             System.out.println("Estoy avisando que tengo un LL en el bloque"+numBloqueDatoM);
                            // avise al hilo maestro q tiene un LL corriendo
                            // guardamos numBloqueDatoM y que mandamos nuestro id para avisar q nosotros tenemos el LL activo 
                            HiloMaestro.avisarLL(numBloqueDatoM, id); 
                            // al final del while coloque lo de q si se le acaba el quantum entonces igual ponga en -1 el RL 
                        }
                        while ( !ejecutarLW_LL(tipo==50, direccionDato, numBloqueDatoM, hilillo)){
                            avanzarReloj();
                        }                       
                    } else {
                          
                         while ( !ejecutarSW_SC(tipo==51, direccionDato, numBloqueDatoM, hilillo, complete)){
                            avanzarReloj();
                            // deberia saber si me sali xq me cambiaron con -1 el coso 
                            // o si fue xq termine 
                         }
                    }
                    
                    /*
                        ocupo diferenciar entre un true de completado y un true de q no puedo hacer nada entonces siga 
                        complete 0 es que le dio fallo de store 
                        complete 1 ya termino
                        complete 2 simplemente fallo de otra cosa 
                    */
                    //if (complete == 0){ 
                        if (q == 0 && contexto.RL != -1) { 
                            contexto.RL = -1; // poner rl en -1 por si se le acabo el quantum a una instruccion LL-SC
                            HiloMaestro.avisarLLFallido(id);  
                        }
                    //}
                    q--; // reducir quantum, pues termina de ejecutarse la instruccion lw/sw/ll/sc 
                    // agregado
                    avanzarReloj();
                }else {
                    q--;                        // Disminuimos Quatum 
                    try {
                        esFin = Decodificador.esFin(hilillo);
                        if (esFin){
                            agregarATerminados = true;
                            HiloMaestro.terminarHilo(); 
                            System.out.println("El nucleo "+ id + " agrega el contexto del hilillo " + contexto.id +" a la cola de terminados");
                            coladeTerminados.add(contexto);
                            avanzarReloj();
                            break;
                        }
                    } finally {
                       // lockOcupado.unlock();
                    }
                    avanzarReloj();
                }
            } else {    // Instruccion no esta en cache 
               
                System.out.println("Nucleo: "+ id + ". Trayendo de memoria: "+ numBloque + " en el bloque de cache: "+nuevoNumBloque);  
                 // ponemos en cache la tag del numero de bloque
                
                int c = tiempoTrayendoBloque; 
                if (!HiloMaestro.attemptAccess()) {
                    System.out.println("Nucleo: "+ id + ", Bus esta ocupado.");
                    // Avanzar reloj hasta que bus esté desocupado
                    avanzarReloj();
                } else {
                    // traer de memoria las 4 palabras del bloque 
                   for (int j=0; j<4; j++) {  // 4 palabras
                        for (int i=0; i<4; i++) {  // 4 campitos de 1 palabra
                            cacheInstrucciones[nuevoNumBloque][j*4+i]= HiloMaestro.leerMemoria((numBloque*16)+((4*j)+i));
                        }
                    }
                    cacheInstrucciones[nuevoNumBloque][16] = numBloque;
                    imprimirCache();
                    imprimirCacheDatos();
                    HiloMaestro.imprimirMemoria();
                    HiloMaestro.releaseAccess();
                    // mientras que la cantidad de ciclos nueva no termine 
                    while (c != 0){
                        c--;    // Solo avance reloj 
                        avanzarReloj();
                    } 
                    // Al salir de aquí ya cargó el bloque a cache
                }
            }
            
           
            
        }//final de while
        
        
        
        System.out.println("El nucleo "+ id+ " termina de procesar quantum. El contexto guardado es: PC = "+contexto.PC);  
        // Como aun no termina, se mete el contexto a la cola para luego volver a procesarlo  
        if (!agregarATerminados) {
            System.out.println("El nucleo "+ id + " agrega el contexto del hilillo " + contexto.id +" a la cola");
            cola.add(contexto); // Guardamos el contexto
        } 
        contexto = null; // Retiramos el contexto del nuecleo
    }
    
    public void invalidarCache(int bloqueMemoria){
        cacheDatos[bloqueMemoria%8][5]=-1;
    }
    
    public int[] getBloque(int bloque) {
        return cacheDatos[bloque];
    }
    
    public int cacheHit(int bloqueMemoria){
        if (cacheDatos[bloqueMemoria%8][4]==bloqueMemoria){
            return cacheDatos[bloqueMemoria%8][5]; // envia estado del bloque 
        }
        return -1;
    }
   
    
    @Override
    public void run(){
        while (HiloMaestro.hayTrabajo()) {                
            try {    // Seria hasta que ya no exista trabajo
            // Caso de que no tenga un contexto asignado el debe seguir corriendo  
            if (contexto == null) {
                System.out.println("Nucleo " + id + " esta ocioso");
                avanzarReloj();              
                continue;  // Salga de esta iteración hasta que exista un contexto
            }  
            System.out.println("Núcleo " + id + " iniciando el quantum");
            procesarQuantum(); // Se procesa un quantum 
            lockOcupado.lock();
            try {
                ocupado = false; 
            } finally {
                lockOcupado.unlock();
            }
            System.out.println("Núcleo " + id + " terminado quantum");  
        }   catch (InterruptedException ex) {
                Logger.getLogger(Nucleo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       
        System.out.println("TERMINO EL NUCLEO " +id);
    }        

    // Recibe la direccion virtual (Por ejemplo 640), el numero de bloque de memoria iniciando en 0 hasta 87, y el string del hilillo
    private boolean ejecutarLW_LL(boolean esLL, int direccion, int numBloqueDatoM, String hilillo) throws InterruptedException {
        boolean lecturaCompleta;
        if(pedirCache()){ // Si pudo tomar la cache, la toma
            System.out.println("Dentro del ciclo ejecutarLW");
            if (cacheHit(numBloqueDatoM) != -1){  // Es un
                Decodificador.ejecutarLectura(cacheDatos, direccion, contexto, hilillo); //Leer la palabra
                avanzarReloj();
                liberarCache();
                lecturaCompleta = true;
            }else {
                if (HiloMaestro.pedirBusDatos()){  // Cambio con respecto al diagrama (di se pide el bus antes de que se pruebe que el tag del bloque es modificado)
                    //PARTE DEL WRITE BACK
                    if(bloqueDatosModificado(numBloqueDatoM%8)){  // Se revisa el bloque que esta en la cache antes de ser reemplazado, si esta modificado se va a memoria a escribirlo
                        HiloMaestro.escribirEnMemoria(cacheDatos[numBloqueDatoM%8], cacheDatos[numBloqueDatoM%8][4]);  //Esto es el write-back
                    }                     
                    if (HiloMaestro.pedirCacheDelOtroNucleo(id)) {
                        int snooping = HiloMaestro.snooping(id, numBloqueDatoM);
                        if ( snooping == 1) { //ESTA EN LA OTRA CACHE Y ESTA MODIFICADO
                            // Se tiene que copiar los datos de la cache vecina y escribir los datos en memoria
                            int [] bloqueMemoria = HiloMaestro.leerDesdeLaOtraCache(id, numBloqueDatoM);
                            System.arraycopy(bloqueMemoria, 0, cacheDatos[numBloqueDatoM%8], 0, PALABRASPORBLOQUE);
                            HiloMaestro.escribirEnMemoria(bloqueMemoria, numBloqueDatoM); // NO LO HE PROBADO
                            cacheDatos[numBloqueDatoM%8][4] =numBloqueDatoM; // Se settea el numero de bloque
                            cacheDatos[numBloqueDatoM%8][5] =0; // Estado compartido
                            HiloMaestro.setEstadoEnOtraCache(id ,numBloqueDatoM%8, 0);  //Cambiar el estado de la otra cache de M a C
                            for (int i = 0;i< (4*b+4*b+4*m);i++){
                                avanzarReloj();
                            }
                            Decodificador.ejecutarLectura(cacheDatos, direccion, contexto, hilillo);
                            liberarCache();
                            HiloMaestro.liberarCacheVecina(id);
                            HiloMaestro.soltarBusDatos();
                            lecturaCompleta = true;
                        } else {
                            //HAY QUE IR A MEMORIA
                            int [] bloqueMemoria = HiloMaestro.leerDesdeMemoria(direccion); 
                            System.arraycopy(bloqueMemoria, 0, cacheDatos[numBloqueDatoM%8], 0, PALABRASPORBLOQUE);
                            cacheDatos[numBloqueDatoM%8][4] = numBloqueDatoM;   // Se settea el numero de bloque    
                            cacheDatos[numBloqueDatoM%8][5] = 0;                // El estado sera COMPÀRTIDO POR DEFAULT
                            Decodificador.ejecutarLectura(cacheDatos, direccion, contexto, hilillo);
                            for (int i = 0;i< (4*b+4*m);i++){
                                avanzarReloj();
                            }
                            Decodificador.ejecutarLectura(cacheDatos, direccion, contexto, hilillo);
                            liberarCache();
                            HiloMaestro.liberarCacheVecina(id);
                            HiloMaestro.soltarBusDatos();
                            /*cacheDatos[numBloqueDatoM%8][0] = cacheDatos[numBloqueDatoM%8][0]*10;
                            cacheDatos[numBloqueDatoM%8][1] = cacheDatos[numBloqueDatoM%8][1]*10;
                            cacheDatos[numBloqueDatoM%8][2] = cacheDatos[numBloqueDatoM%8][2]*10;
                            cacheDatos[numBloqueDatoM%8][3] = cacheDatos[numBloqueDatoM%8][3]*10;
                            HiloMaestro.escribirEnMemoria(cacheDatos[numBloqueDatoM%8], numBloqueDatoM); */
                            lecturaCompleta = true;
                        }
                    } else { // No se obtuvo la Cache Vecina
                        HiloMaestro.soltarBusDatos();
                        liberarCache();
                        avanzarReloj();
                        lecturaCompleta = false;
                    } 
                } else {  // No se obtuvo el BUS
                    liberarCache();
                    lecturaCompleta = false;
                }
            }            
        } else {  // No se obtuvo la Cache
            avanzarReloj();
            lecturaCompleta = false;
        }
        if (lecturaCompleta && esLL){
            contexto.RL = direccion;
        }
        return lecturaCompleta;
    }

    public void traerBloqueMemDatos(int numBloqueDatoM) {
        for (int i = 0; i < 4; i++){
            cacheDatos[numBloqueDatoM%8][i] = HiloMaestro.leerMemoriaDatos(i+(numBloqueDatoM*4));
        }
    }
    
    public void subirBloqueMemDatos(int numBloqueDatoM) {
        for (int i = 0; i < 4; i++){
            int valor = cacheDatos[numBloqueDatoM%8][i];
            HiloMaestro.escribirMemoriaDatos(i+(numBloqueDatoM*4), valor);
        }
    }
    
    public int cacheState(int bloqueMemoria){
        if (cacheDatos[bloqueMemoria%8][5] == 1){
            return 1; // modificado 
        } else {
            return 0; // compartido
        }
    }
    
    public int [] compartirBloque(int numBloqueDatoM, int [] bloque) {
        for (int i = 0; i < 4; i++){
            bloque[i] = cacheDatos[numBloqueDatoM%8][i];
        }
        return bloque; 
    }
    
    public void actualizarBloque(int numBloqueDatoM, int [] bloque) {
        for (int i = 0; i < 4; i++){
            cacheDatos[numBloqueDatoM%8][i] = bloque[i]; 
        }
    }
    
     public void invalidarBloque(int numBloque){
        cacheDatos[numBloque%8][5] = -1;
    }
    
    public void ponermeModifBloque(int numBloque){
        cacheDatos[numBloque%8][5] = 1;
    }
    
    private boolean ejecutarSW_SC(boolean esSC, int direccion, int numBloqueDatoM, String hilillo, int complete) throws InterruptedException {
        
        imprimirCacheDatos(); 
        HiloMaestro.imprimirMemDatos();
        if (contexto.RL == -1 && esSC){
            // el sc falla en su ejecucion y no ejecuta un sw 
            contexto.registros[1] = 0;
            complete = 0; 
            return true;
        } else {
            if (esSC) {
                contexto.registros[1] = 1;
            }            
            // el sw normal 
            int estado = cacheHit(numBloqueDatoM); 
            if(pedirCache()){ // Si pudo tomar la cache, la toma
                 System.out.println("Dentro del ciclo ejecutarSW");
                //System.out.println("Nucleo "+id+" toma su cache");
                if (estado != -1){  // Es un C o un M osea un hit 
                    //System.out.println("Nucleo "+id+" tiene un hit");
                    if (estado == 1)  { // modificado 
                        //no pide bus, solo escribe
                        Decodificador.ejecutarEscritura(cacheDatos, direccion, contexto, hilillo);
                        liberarCache();
                        complete = 1; 
                        return true;
                        
                    } else { // compartido 
                        // si es C, pide bus para avisar a las demas caches q modificara un bloque
                        if (HiloMaestro.pedirBusDatos()){ // si es c pide bus avisar a las demas caches 
                            HiloMaestro.avisarInvalidacion(numBloqueDatoM,id);
                            Decodificador.ejecutarEscritura(cacheDatos, direccion, contexto, hilillo);
                            ponermeModifBloque(numBloqueDatoM);
                            HiloMaestro.soltarBusDatos();
                            liberarCache();
                            complete = 1; 
                            return true; 
                        } else { 
                            liberarCache();
                            complete = 2; 
                            return false;                            
                        }
                    } 
                    
                } else {// miss
                    if (HiloMaestro.pedirBusDatos()){
                        
                        if(cacheState(numBloqueDatoM) == 1){  // si el bloque actual de mi cache esta Modificado, lo escribo en memoria 
                            subirBloqueMemDatos(numBloqueDatoM);
                        } 
                        // ya ahora si puedo caerle encima con cosas al bloque
                        // primero me fijo si puedo pedir la cache vecina y preguntar si el bloque esta ahi
                        if (HiloMaestro.pedirCacheDelOtroNucleo(id)) {
                            //System.out.println("Nucleo "+id+" pidio cache vecina y la tomo");
                            int snooping = HiloMaestro.snooping(id, numBloqueDatoM); // me dice el estado en que esté el bloque en la otra cache
                            System.out.println("Nucleo "+id+" pregunta por estado de bloque: "+numBloqueDatoM+ "y el estado es: "+snooping);
                            if ( snooping == 1) { // esta en M en la otra cache
                                System.out.println("Nucleo "+id+" pide subir bloque de cache vecina a mem ");
                                HiloMaestro.subirBloqueCacheVecinaAMemDatos(id, numBloqueDatoM);
                                System.out.println("Nucleo "+id+" pide a hilo principal q le pase el bloque de cache vecina");
                                HiloMaestro.subirBloqueCacheVecinaACacheNeedly(id, numBloqueDatoM);
                                cacheDatos[numBloqueDatoM%8][4] = numBloqueDatoM;
                                ponermeModifBloque(numBloqueDatoM);// ponerme como modificado
                                HiloMaestro.avisarInvalidacion(numBloqueDatoM, id);
                                //como fui a memoria y todo simulo los ciclos
                                Decodificador.ejecutarEscritura(cacheDatos, direccion, contexto, hilillo);

                                for (int i = 0;i< (4*b+4*m);i++){
                                    avanzarReloj();
                                }
                                //System.out.println("Nucleo "+id+" libera cache vecina");
                                HiloMaestro.liberarCacheVecina(id);
                                HiloMaestro.soltarBusDatos();
                                liberarCache();
                                complete = 1; 
                                return true; 
                            } else { // esta en C o I, se trae de memoria, no ocupamos cache vecina, la liberamos.
                                //System.out.println("Nucleo "+id+" trae bloque de memoria pues no esta en cache vecina");
                                traerBloqueMemDatos(numBloqueDatoM);
                                cacheDatos[numBloqueDatoM%8][4] = numBloqueDatoM;
                                ponermeModifBloque(numBloqueDatoM);
                                for (int i = 0;i< (4*b+4*m);i++){
                                    avanzarReloj();
                                }
                                HiloMaestro.liberarCacheVecina(id);
                                HiloMaestro.soltarBusDatos();
                                liberarCache();
                                complete = 2;
                                return false;
                            }
                            
                        } else {
                            HiloMaestro.soltarBusDatos();
                            liberarCache();
                            complete = 2; 
                            return false;
                        }

                    } else {
                        liberarCache();
                        complete = 2; 
                        return false;
                    }
                       
                }
   
            } else {  // No se pudo pedir la cache
                //System.out.println("Nucleo "+id+" no pudo tomar su cache, solo avanza reloj");
                complete = 2; 
                return false;
            }
        }
        
    }

    public void setEstadoBloque(int bloque, int estado) {
        cacheDatos[bloque][5] = estado;
    }
}
