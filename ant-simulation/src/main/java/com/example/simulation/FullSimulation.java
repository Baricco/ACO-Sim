package com.example.simulation;

public class FullSimulation extends Simulation {
    
    private static final int NEST_NUMBER = 1;
    private static final int ANT_NUMBER = 500;
    private static final int CLUMP_SIZE = 100;                       // Numero di pezzi di cibo in un ammasso
    private static final int CLUMP_NUMBER = 15;                      // Numero di ammassi di cibo        

    public FullSimulation(double mapWidth, double mapHeight) {
        super(NEST_NUMBER, ANT_NUMBER, CLUMP_SIZE, CLUMP_NUMBER, mapWidth, mapHeight);
        initDensityManager();
    }

    @Override
    public void start() throws InterruptedException {

        // Inizializzazione dell'arraylist di formiche e di cibo
        startupSimulation();
        
        // selectedItemIndex = 0;
        // zoom = false;

        // simulationGraphicsSetup rimossa - sarà gestita dal controller JavaFX

        while (!exit) {
            try { Thread.sleep(REFRESH_RATE); } catch (InterruptedException e) { } 
            /*
                if(zoom) {
                    if(selectedItemObj!=null && selectedItemObj.checkAlive()){
                        centerOnPoint(selectedItemObj.getCircle().getCenter());
                        lbl_stats_title.setText(selectedItemObj.getObjType().toUpperCase());
                        lbl_stats_text.setText(selectedItemObj.toHTML());
                    }
                }
            */
        }   
    }
}