package imrt2;

/* LogFunction
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.ampl.AMPL;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.FileNameMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gcab623
 */
public class TreatmentPlan implements Comparable<TreatmentPlan> {
    
    public double[][] beams_vector;
    public boolean warm_start;
    public int beams;
    public int[] prevAngles;
    public int[] selAngles;
    private int beamlets;
    private double slope;
    private double[] intensity;
    private double[] weights;
    private double[] gEUD;
    private double singleObjectiveValue;
    private boolean visited;
    private double Vx[][];
    private double Dx[][];
    private String pathfile;
    
    private HashMap<Integer, Organs> organos;
    private AMPL_Solver ampl;
    private int[][] beamletsMap;
    
        public TreatmentPlan(int b, int bl, int a, int o) {
        this.beams = b;
        this.prevAngles = new int[b];
        this.beamlets = bl;
        this.intensity = new double[bl];
        this.gEUD = new double[o + 1]; // # of Organs + PTV UB
        this.weights = new double[o]; // # of Organs + PTV UB
        this.Vx = new double[o][]; // # of Organs
        this.Dx = new double[o][]; // # of Organs
        this.singleObjectiveValue = 0;
        this.visited = false;
        this.slope = 0;
        organos = new HashMap<>();
        beamletsMap = new int[360][4];
        
    }
    
    public TreatmentPlan(TreatmentPlan tratamiento){
        
        warm_start = tratamiento.warm_start;
        
        beams = tratamiento.beams;
        
        prevAngles = new int[tratamiento.prevAngles.length];
        if(tratamiento.prevAngles.length>0)
            System.arraycopy(tratamiento.prevAngles, 0, prevAngles, 0, tratamiento.prevAngles.length);
        
        selAngles = new int[tratamiento.selAngles.length];
        if(tratamiento.selAngles.length>0)
            System.arraycopy(tratamiento.selAngles, 0, selAngles, 0, tratamiento.selAngles.length);
        
        beamlets = tratamiento.getBeamlets();
        
        slope = tratamiento.getSlope();
        if(tratamiento.getIntensity() != null){
            intensity = new double[tratamiento.getIntensity().length];
            if(tratamiento.getIntensity().length>0)
                System.arraycopy(tratamiento.getIntensity(), 0, intensity, 0, tratamiento.getIntensity().length);

        }
        
        weights = new double[tratamiento.getWeights().length];
        if(tratamiento.getWeights().length>0)
            System.arraycopy(tratamiento.getWeights(), 0, weights, 0, tratamiento.getWeights().length);
        
        gEUD = new double[tratamiento.getgEUD().length];
        if(tratamiento.getgEUD().length>0)
            System.arraycopy(tratamiento.getgEUD(), 0, gEUD, 0, tratamiento.getgEUD().length);
        
        singleObjectiveValue = tratamiento.getSingleObjectiveValue();
        
        visited = tratamiento.isVisited();
        
        if(tratamiento.getVx() != null)
            if(tratamiento.getVx().length> 0 && tratamiento.getVx()[0] != null){
                Vx = new double[tratamiento.getVx().length][tratamiento.getVx()[0].length];
                for(int i=0;i<tratamiento.getVx().length;i++){
                    System.arraycopy(tratamiento.getVx()[i], 0, Vx[i], 0, tratamiento.getVx()[0].length);
                }
            }
        if(tratamiento.getDx() != null)
            if(tratamiento.getDx().length> 0 && tratamiento.getDx()[0] != null){
                Dx = new double[tratamiento.getDx().length][tratamiento.getDx()[0].length];
                for(int i=0;i<tratamiento.getDx().length;i++){
                    System.arraycopy(tratamiento.getDx()[i], 0, Dx[i], 0, tratamiento.getDx()[0].length);
                }
            }
        
        pathfile = tratamiento.getPathfile();

        organos = new HashMap<>();
        for(int organo:tratamiento.getOrganos().keySet()){
            organos.put(organo, new Organs(tratamiento.getOrganos().get(organo)));
        }
        if(tratamiento.getAmpl() != null)
            ampl = new AMPL_Solver(tratamiento.getAmpl());
        
        beamletsMap = tratamiento.getBeamletsMap().clone();
        if(tratamiento.getBeamletsMap() != null){
            beamletsMap = new int[tratamiento.getBeamletsMap().length][tratamiento.getBeamletsMap()[0].length];
            for(int i=0; i<tratamiento.getBeamletsMap().length;i++){
                System.arraycopy(tratamiento.getBeamletsMap()[i], 0, beamletsMap[i], 0, tratamiento.getBeamletsMap()[i].length); 
            }
        }
        
    }
    
    
    
    @Override
    public int compareTo(TreatmentPlan o) {
        return new Double(this.gEUD[1]).compareTo(new Double(o.gEUD[1]));
    }
    public void loadNumBixels(String beamsInfoDir) throws IOException{
        //int[][] beamlets = new int[numBeams][4];
        int auxBeamletIndex=0;
        System.out.println(beamsInfoDir);
        File beamInfo = new File(beamsInfoDir);
        if (! beamInfo.exists()) {
            System.err.println("Couldn't find 'beamsInfo.txt' file");
        }else{
            String line ="";
            String[] auxReader=null;   
            File f= new File(beamsInfoDir);
            BufferedReader fileIn= new BufferedReader(new FileReader(f));
            for (int i=0;i<360;i = i++){
                line=fileIn.readLine();
                auxReader = line.split("\t");
                beamletsMap[i][0]=(int) Double.parseDouble(auxReader[0]); //beamIndex
                beamletsMap[i][1]=(int) Double.parseDouble(auxReader[1]); //numBeamlets
                beamletsMap[i][2]= auxBeamletIndex;                       //firstBeamletIndex
                beamletsMap[i][3]=(int) Double.parseDouble(auxReader[2]) - 1; //lastBeamletIndex
                auxBeamletIndex = auxBeamletIndex + (int) Double.parseDouble(auxReader[1]);
            }
            fileIn.close();
        }
    }
    
    public void loadNumBixels(String beamsInfoDir, int step) throws IOException{
        //int[][] beamlets = new int[numBeams][4];
        int auxBeamletIndex=0;
        System.out.println(beamsInfoDir);
        File beamInfo = new File(beamsInfoDir);
        if (! beamInfo.exists()) {
            System.err.println("Couldn't find 'beamsInfo.txt' file");
        }else{
            String line ="";
            String[] auxReader=null;   
            File f= new File(beamsInfoDir);
            BufferedReader fileIn= new BufferedReader(new FileReader(f));
            for (int i=0;i<360;i++){
                line=fileIn.readLine();
                auxReader = line.split("\t");
                beamletsMap[i][0]=(int) Double.parseDouble(auxReader[0]); //beamIndex
                beamletsMap[i][1]=(int) Double.parseDouble(auxReader[1]); //numBeamlets
                beamletsMap[i][2]= auxBeamletIndex;                       //firstBeamletIndex
                beamletsMap[i][3]=(int) Double.parseDouble(auxReader[2]) - 1; //lastBeamletIndex
                auxBeamletIndex = auxBeamletIndex + (int) Double.parseDouble(auxReader[1]);
            }
            fileIn.close();
        }
    }
    
   

    public void generateReferencePoint(HashMap<Integer,Organs> o, int weighted, String solver, double maxIntensity, int opt,  String jobThreadID) throws IOException {

        switch (opt) {
            case 1:
                this.solveLogFunction(maxIntensity,o,solver, jobThreadID);
                break;
           
           
        }

    }
    
    public AMPL generateReferencePoint(HashMap<Integer,Organs> o, int weighted, String solver, double maxIntensity, int opt,  String jobThreadID, int[] all_ang, int[] fixed, AMPL ampl, String timerFile) throws IOException {

        switch (opt) {
            case 1:
                ampl = this.solverMIPLogFunction(ampl,maxIntensity,o,solver, jobThreadID, all_ang, fixed,timerFile);
                break;
           
           
        }
        return ampl;
    }
   
    
    public static double getDxx_ge(ArrayList<Double> dose,ArrayList<Double> volume,double percentage){
       double Dxx = (double)-1;
       int i=0;
       for(i=0;i<volume.size();i++){
       //    System.out.println("dose: "+dose.get(i)+"\t volume: "+volume.get(i)+">="+percentage);
               if(volume.get(i)<percentage){
                       return dose.get(i-1);
               }
       }
       if(percentage==0){
               return dose.get(i-1);
       }
       return Dxx;
    }
    public static double getDxx_le(ArrayList<Double> dose,ArrayList<Double> volume,double percentage){
        double Dxx = (double)-1;

        int i=0;
        for(i=0;i<volume.size();i++){
        //    System.out.println("dose: "+dose.get(i)+"\t volume: "+volume.get(i)+"<="+percentage);
                if(volume.get(i)<=percentage){
                        return dose.get(i);
                }
        }
        if(percentage==0){
                return dose.get(i-1);
        }
        return Dxx;
    }
   
    public void solveTestFunction(Organs[] o, String jobThreadID) throws IOException {
        Random r = new Random();
        double aux;
        aux = 0;
        this.intensity = new double[this.beamlets];
        for (int i = 0; i < this.beamlets; i++) {
            this.intensity[i] = r.nextDouble();
        }
        this.gEUD[0] = o[0].getDesiredDose();
        aux = (1 + r.nextDouble());
        this.gEUD[1] = o[1].getDesiredDose() * aux;
        aux = (1 + r.nextDouble());
        this.gEUD[2] = o[2].getDesiredDose() * aux;
        aux = r.nextDouble();
        this.gEUD[3] = o[0].getDoseUB() * aux;
        aux = 1 + r.nextDouble();
        this.singleObjectiveValue = aux * 5;
    }
    //public void getLogFunctionValue(Organs_bkp[] o) throws IOException{
    public void getLogFunctionValue(HashMap<Integer,Organs> o) throws IOException {
        double objectiveValue = 0;
        double EUD0, aux_gEUD, v;
        for (int i = 1; i < o.size(); i++) {
            EUD0 = o.get(i).getDesiredDose();
            aux_gEUD = this.gEUD[i];
            v = o.get(i).getV();
            objectiveValue = objectiveValue - Math.log(Math.pow(1 + Math.pow(aux_gEUD / EUD0, v), -1));
        }
        this.singleObjectiveValue = objectiveValue;
        System.out.println("Log function Value = " + this.singleObjectiveValue);
    }

 

    public double[] getEUD(HashMap<Integer,Organs> o, String jobThreadID) throws FileNotFoundException, IOException {
        File[] f = new File[o.size()];
        BufferedReader[] fileIn = new BufferedReader[o.size()];
        double[] aux_gEUD = new double[this.gEUD.length];
        for (int y = 0; y < o.size(); y++) {
            f[y] = new File("./" + jobThreadID + "gEUD_" + o.get(y).getName() + ".txt");
            fileIn[y] = new BufferedReader(new FileReader(f[y]));
            String line = fileIn[y].readLine();
            
            String auxReader = null;
            while (!line.equals("")) {
                
                auxReader = line;
                //System.out.println(o[y].name +" : " +  aux_gEUD[y] + " [Gy] ");
                line = fileIn[y].readLine();
            } 
            if(auxReader != line){
                String[] auxReader1 = auxReader.split(" = ");
                aux_gEUD[y] = Double.parseDouble(auxReader1[auxReader1.length-1]);
            }else {
                System.out.println("error: No gEUD value for: " + o.get(y).getName() + "(./" + jobThreadID + "gEUD_" + o.get(y).getName() + ".txt)");
            }
            fileIn[y].close();
            if (o.get(y).isIsTarget()) {
                // get gEUD for OAR-PTV
                File g = new File("./" + jobThreadID + "gEUD_" + o.get(y).getName() + "_UB.txt");
                BufferedReader gIn = new BufferedReader(new FileReader(g));
                line = gIn.readLine();
                 while (!line.equals("")) {
                    
                    auxReader = line;
                    line = gIn.readLine();
                    //System.out.println(o[y].name +" : " +  aux_gEUD[y] + " [Gy] ");
                } 
                
                if (line != null) {
                    String[] auxReader2 = auxReader.split(" = ");
                    aux_gEUD[this.gEUD.length - 1] = Double.parseDouble(auxReader2[auxReader2.length-1]);
                    //System.out.println(o[y].name +" : " +  aux_gEUD[y] + " [Gy] ");
                } else {
                    System.out.println("error: No gEUD value for OAR-PTV (./" + jobThreadID + "gEUD_" + o.get(y).getName() + "_UB.txt)");
                }
            }
        }
        return aux_gEUD;
    }

 
    public void printSol(String dirFile) throws IOException {
        BufferedWriter bwFile = null;
        File solFile = new File(dirFile);
        if (solFile.exists()) {
            bwFile = new BufferedWriter(new FileWriter(solFile, true));
        } else {
            
            bwFile = new BufferedWriter(new FileWriter(solFile));
            /**************IMPRIME PRIMERA LINEA DEL ARCHIVO*****************/
            for (int i = 0; i < this.beams; i++) {
                writeLine( "Angles["+i+"] \t", bwFile);
            }
            for (int i = 0; i < this.gEUD.length; i++) {
                writeLine( "gEUD["+i+"] \t", bwFile);
            }
            writeLine("globalScore \t", bwFile);//
            for (int j = 0; j < this.weights.length; j++) {
                writeLine("weights["+j+"] \t", bwFile);
            }
            /*for (double[] Vx1 : this.Vx) {
                if (Vx1 != null) {
                    for (int j = 0; j < Vx1.length; j++) {
                        writeLine("Vx1["+j+"] \t", bwFile);
                    }
                }
            }
            for (double[] Dx1 : this.Dx) {
                if (Dx1 != null) {
                    for (int j = 0; j < Dx1.length; j++) {
                        writeLine("Dx1["+j+"] \t", bwFile);
                    }
                }
            }*/
            writeLine( "\n", bwFile);
        } 
        for (int i = 0; i < this.beams; i++) {
            writeLine(this.selAngles[i] + "\t", bwFile);
        }
        beams = selAngles.length;
        for (int j = 0; j < this.gEUD.length; j++) {
            writeLine(this.gEUD[j] + "\t", bwFile);
        }
        writeLine(this.singleObjectiveValue + "\t", bwFile);
        
        for (int j = 0; j < this.weights.length; j++) {
            writeLine(this.weights[j] + "\t", bwFile);
        }

        /*for (double[] Vx1 : this.Vx) {
            if (Vx1 != null) {
                for (int j = 0; j < Vx1.length; j++) {
                    writeLine(Vx1[j] + "\t", bwFile);
                }
            }
        }
        for (double[] Dx1 : this.Dx) {
            if (Dx1 != null) {
                for (int j = 0; j < Dx1.length; j++) {
                    writeLine(Dx1[j] + "\t", bwFile);
                }
            }
        }*/
       for(int k=0;k<this.selAngles.length;k++){
        writeLine("x"+this.selAngles[k]+":\t", bwFile);
        Beam beamAux = this.organos.get(0).getBeams().get(this.selAngles[k]);
         for (int j = 0; j < beamAux.getX().length; j++) {
             writeLine(beamAux.getX()[j] + "\t", bwFile);
         }
         
       }
        writeLine("\n", bwFile);
        bwFile.close();
    
    }
        
    public void printSol(String dirFile, int option) throws IOException {
        BufferedWriter bwFile = null;
        File solFile = new File(dirFile);
        if (solFile.exists()) {
            bwFile = new BufferedWriter(new FileWriter(solFile, true));
        } else {
            bwFile = new BufferedWriter(new FileWriter(solFile));
            /**************IMPRIME PRIMERA LINEA DEL ARCHIVO*****************/
            writeLine("TipoMov \t", bwFile);//
            for (int i = 0; i < this.beams; i++) {
                writeLine( "Angles["+i+"] \t", bwFile);
            }
            for (int i = 0; i < this.gEUD.length; i++) {
                writeLine( "gEUD["+i+"] \t", bwFile);
            }
           
            writeLine("globalScore \t", bwFile);//
            for (int j = 0; j < this.weights.length; j++) {
                writeLine("weights["+j+"] \t", bwFile);
            }
            
          
            writeLine( "\n", bwFile);
            /**************IMPRIME PRIMERA LINEA DEL ARCHIVO*****************/
        }
        writeLine(option + "\t", bwFile);
        for (int i = 0; i < this.beams; i++) {
            writeLine(this.selAngles[i] + "\t", bwFile);
        }
        for (int j = 0; j < this.gEUD.length; j++) {
            writeLine(this.gEUD[j] + "\t", bwFile);
        }
        writeLine(this.singleObjectiveValue + "\t", bwFile);
        
        for (int j = 0; j < this.weights.length; j++) {
            writeLine(this.weights[j] + "\t", bwFile);
        }

       for(int k=0;k<this.selAngles.length;k++){
        writeLine("x"+this.selAngles[k]+":\t", bwFile);
        Beam beamAux = this.organos.get(0).getBeams().get(this.selAngles[k]);
         for (int j = 0; j < beamAux.getX().length; j++) {
             writeLine(beamAux.getX()[j] + "\t", bwFile);
         }
         
       }
       
        writeLine("\n", bwFile);
        bwFile.close();
    }
    
    public void printSol(int iter, int feval, int tipoMov, String dirFile) throws IOException { //MAICHOLL
        BufferedWriter bwFile = null;
        File solFile = new File(dirFile);
        if (solFile.exists()) {
            bwFile = new BufferedWriter(new FileWriter(solFile, true));
        } else {
            
            bwFile = new BufferedWriter(new FileWriter(solFile));
            /**************IMPRIME PRIMERA LINEA DEL ARCHIVO*****************/
            writeLine( "Iter \t", bwFile);
            writeLine( "F.Eval \t", bwFile);
            writeLine( "TipoVecindario \t", bwFile);
            for (int i = 0; i < this.beams; i++) {
                writeLine( "Angles["+i+"] \t", bwFile);
            }
            for (int i = 0; i < this.gEUD.length; i++) {
                writeLine( "gEUD["+i+"] \t", bwFile);
            }
          
            /*for (int j = 0; j < this.weights.length; j++) {
                writeLine("weights["+j+"] \t", bwFile);
            }
            for (double[] Vx1 : this.Vx) {
                if (Vx1 != null) {
                    for (int j = 0; j < Vx1.length; j++) {
                        writeLine("Vx1["+j+"] \t", bwFile);
                    }
                }
            }
            for (double[] Dx1 : this.Dx) {
                if (Dx1 != null) {
                    for (int j = 0; j < Dx1.length; j++) {
                        writeLine("Dx1["+j+"] \t", bwFile);
                    }
                }
            }*/
           
            writeLine( "\n", bwFile);
            /**************IMPRIME PRIMERA LINEA DEL ARCHIVO*****************/
        }
        writeLine(iter + "\t", bwFile);
        writeLine(feval + "\t", bwFile);
        writeLine(tipoMov + "\t", bwFile);
        for (int i = 0; i < this.beams; i++) {
            writeLine(this.selAngles[i] + "\t", bwFile);
        }
        for (int j = 0; j < this.gEUD.length; j++) {
            writeLine(this.gEUD[j] + "\t", bwFile);
        }
        writeLine(this.singleObjectiveValue + "\t", bwFile);
        
        for (int j = 0; j < this.weights.length; j++) {
            writeLine(this.weights[j] + "\t", bwFile);
        }

        /*for (double[] Vx1 : this.Vx) {
            if (Vx1 != null) {
                for (int j = 0; j < Vx1.length; j++) {
                    writeLine(Vx1[j] + "\t", bwFile);
                }
            }
        }
        for (double[] Dx1 : this.Dx) {
            if (Dx1 != null) {
                for (int j = 0; j < Dx1.length; j++) {
                    writeLine(Dx1[j] + "\t", bwFile);
                }
            }
        }*/
       
        for(int k=0;k<this.selAngles.length;k++){
            writeLine("x"+this.selAngles[k]+":\t", bwFile);
            Beam beamAux = this.organos.get(0).getBeams().get(this.selAngles[k]);
            for (int j = 0; j < beamAux.getX().length; j++) {
                writeLine(beamAux.getX()[j] + "\t", bwFile);
            }

       }
        writeLine("\n", bwFile);
        bwFile.close();
    }

    public void printSol(String dirFile, long time) throws IOException {
        BufferedWriter bwFile = null;
        File solFile = new File(dirFile);
        if (solFile.exists()) {
            bwFile = new BufferedWriter(new FileWriter(solFile, true));
        } else {
            bwFile = new BufferedWriter(new FileWriter(solFile));
            /**************IMPRIME PRIMERA LINEA DEL ARCHIVO*****************/
            for (int i = 0; i < this.beams; i++) {
                writeLine( "Angles["+i+"] \t", bwFile);
            }
            for (int i = 0; i < this.gEUD.length; i++) {
                writeLine( "gEUD["+i+"] \t", bwFile);
            }
            writeLine( "singleObjectiveValue \t", bwFile);
            writeLine( "time \t", bwFile);
           
            writeLine("globalScore \t", bwFile);//
            for (int j = 0; j < this.weights.length; j++) {
                writeLine("weights["+j+"] \t", bwFile);
            }
            
            writeLine( "\n", bwFile);
            /**************IMPRIME PRIMERA LINEA DEL ARCHIVO*****************/
        }
        for (int i = 0; i < this.beams; i++) {
            writeLine(this.selAngles[i] + "\t", bwFile);
        }
        for (int j = 0; j < this.gEUD.length; j++) {
            writeLine(this.gEUD[j] + "\t", bwFile);
        }
        writeLine(this.singleObjectiveValue + "\t", bwFile);
        writeLine(time + "\t", bwFile);
        //writeLine("x[]:\t", bwFile);
       
        for(int k=0;k<this.selAngles.length;k++){
        writeLine("x"+this.selAngles[k]+":\t", bwFile);
        Beam beamAux = this.organos.get(0).getBeams().get(this.selAngles[k]);
         for (int j = 0; j < beamAux.getX().length; j++) {
             writeLine(beamAux.getX()[j] + "\t", bwFile);
         }
         
       }
        writeLine("\n", bwFile);
        bwFile.close();
    }

   
    public static void writeLine(String l, BufferedWriter bw) throws IOException {

        bw.write(l);
    }
    
    public void printSolTime(String dirFile, int option, long localTimer, String line, boolean of) throws IOException {
        BufferedWriter bwFile = null;
        File solFile = new File(dirFile);
        if (solFile.exists()) {
            bwFile = new BufferedWriter(new FileWriter(solFile, true));
        } else {
            bwFile = new BufferedWriter(new FileWriter(solFile));
            /**************IMPRIME PRIMERA LINEA DEL ARCHIVO*****************/
            writeLine("TipoMov \t", bwFile);//
            for (int i = 0; i < this.beams; i++) {
                writeLine( "Angles["+i+"] \t", bwFile);
            }
            for (int i = 0; i < this.gEUD.length; i++) {
                writeLine( "gEUD["+i+"] \t", bwFile);
            }
           
            writeLine("globalScore \t", bwFile);//
            for (int j = 0; j < this.weights.length; j++) {
                writeLine("weights["+j+"] \t", bwFile);
            }
            
          
            writeLine( "\n", bwFile);
            /**************IMPRIME PRIMERA LINEA DEL ARCHIVO*****************/
        }
        
        if(of){
            writeLine(option + "\t", bwFile);
            for (int i = 0; i < this.beams; i++) {
                writeLine(this.selAngles[i] + "\t", bwFile);
            }
            for (int j = 0; j < this.gEUD.length; j++) {
                writeLine(this.gEUD[j] + "\t", bwFile);
            }
            writeLine(this.singleObjectiveValue + "\t", bwFile);

            for (int j = 0; j < this.weights.length; j++) {
                writeLine(this.weights[j] + "\t", bwFile);
            }

           for(int k=0;k<this.selAngles.length;k++){
            writeLine("x"+this.selAngles[k]+":\t", bwFile);
            Beam beamAux = this.organos.get(0).getBeams().get(this.selAngles[k]);
             for (int j = 0; j < beamAux.getX().length; j++) {
                 writeLine(beamAux.getX()[j] + "\t", bwFile);
             }

           }
           writeLine("\n", bwFile);
        }
        writeLine(((System.currentTimeMillis()- localTimer)/1000)+ " Seconds \t" +line , bwFile);
        writeLine("\n", bwFile);
        bwFile.close();
    }

    public static Comparator<TreatmentPlan> gEUDRectum_Comparator = new Comparator<TreatmentPlan>() {
        public int compare(TreatmentPlan tp1, TreatmentPlan tp2) {
            if (tp1.gEUD[1] < tp2.gEUD[1]) {
                return -1;
            } else {
                return 1;
            }
        }
    };

    public static Comparator<TreatmentPlan> judgementFunct_Comparator = new Comparator<TreatmentPlan>() {
        public int compare(TreatmentPlan tp1, TreatmentPlan tp2) {
            if (tp1.singleObjectiveValue < tp2.singleObjectiveValue) {
                return -1;
            } else {
                return 1;
            }
        }
    };

    private void solveLogFunction(double maxIntensity, HashMap<Integer,Organs> o, String solver, String jobThreadID) throws IOException {
        double[] EUD0 = new double[o.size()];
        for (int i = 0; i < o.size(); i++) {
            if (o.get(i).isIsTarget()) {
                EUD0[i] = o.get(i).getDoseLB(); //Target
            } else {
                EUD0[i] = o.get(i).getDoseUB(); //OAR
            }
        }

        ampl = new AMPL_Solver( this, solver, maxIntensity,jobThreadID); //-1 maxIntensity default
        /*##################################################
       # "generateParametersFile" method  per each option #
       # "generateParametersFile" method  per each option #
       #################################################*/
     
      
       
        System.out.println("Generating parameters File (1)");
        ampl.generateParametersFile_logFunction(this);
        
         
        ampl.runLogFunction_Solver(this);
        
        ampl.getSolution(this);
        //this.intensity = new double[this.beamlets];
        //System.arraycopy(ampl.getX(), 0, this.intensity, 0, this.beamlets);
        //calculate gEUDs
        System.arraycopy(getEUD(o, jobThreadID), 0, this.gEUD, 0, this.gEUD.length);
        this.getLogFunctionValue(o);
        markResolved();
    }
    
    private AMPL solverMIPLogFunction(AMPL ampl_solver, double maxIntensity, HashMap<Integer,Organs> o, String solver, String jobThreadID, int[] all_ang, int[] fixed, String timerFile) throws IOException {
        long prevTimer = System.currentTimeMillis();
        double[] EUD0 = new double[o.size()];
        for (int i = 0; i < o.size(); i++) {
            if (o.get(i).isIsTarget()) {
                EUD0[i] = o.get(i).getDoseLB(); //Target
            } else {
                EUD0[i] = o.get(i).getDoseUB(); //OAR
            }
        }

        ampl = new AMPL_Solver( this, solver, maxIntensity,jobThreadID, all_ang); //-1 maxIntensity default
        /*##################################################
       # "generateParametersFile" method  per each option #
       # "generateParametersFile" method  per each option #
       #################################################*/
     
      
       
        System.out.println("Generating parameters File (1)");
        
        ampl.generateParametersFile_rMIPlogFunction(this,fixed, all_ang);
        
        this.printSolTime(timerFile,0,prevTimer,"solverMIPLogFunction: Starting AMPL",false);
        prevTimer = System.currentTimeMillis(); 
        ampl_solver = ampl.runrMIP_LogFunction_Solver(this,fixed, ampl_solver, all_ang, timerFile);
        
        ampl.getSolution(this);
        //this.intensity = new double[this.beamlets];
        //System.arraycopy(ampl.getX(), 0, this.intensity, 0, this.beamlets);
        //calculate gEUDs
        //System.arraycopy(getEUD(o, jobThreadID), 0, this.gEUD, 0, this.gEUD.length);
        //this.getLogFunctionValue(o);
        //markResolved();
        return ampl_solver;
    }
    
    public void useThisSolution() throws IOException{
    
        ampl.useThisSolution();
    }
     
    /**
     * @return the beamlets
     */
    public int getBeamlets() {
        return beamlets;
    }

    /**
     * @param beamlets the beamlets to set
     */
    public void setBeamlets(int beamlets) {
        this.beamlets = beamlets;
    }

    /**
     * @return the slope
     */
    public double getSlope() {
        return slope;
    }

    /**
     * @param slope the slope to set
     */
    public void setSlope(double slope) {
        this.slope = slope;
    }

    /**
     * @return the intensity
     */
    public double[] getIntensity() {
        return intensity;
    }

    /**
     * @param intensity the intensity to set
     */
    public void setIntensity(double[] intensity) {
        this.intensity = intensity;
    }

    /**
     * @return the weights
     */
    public double[] getWeights() {
        return weights;
    }

    /**
     * @param weights the weights to set
     */
    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    /**
     * @return the gEUD
     */
    public double[] getgEUD() {
        return gEUD;
    }

    /**
     * @param gEUD the gEUD to set
     */
    public void setgEUD(double[] gEUD) {
        this.gEUD = gEUD;
    }

    /**
     * @return the singleObjectiveValue
     */
    public double getSingleObjectiveValue() {
        return singleObjectiveValue;
    }

    /**
     * @param singleObjectiveValue the singleObjectiveValue to set
     */
    public void setSingleObjectiveValue(double singleObjectiveValue) {
        this.singleObjectiveValue = singleObjectiveValue;
    }

    /**
     * @return the visited
     */
    public boolean isVisited() {
        return visited;
    }

    /**
     * @param visited the visited to set
     */
    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public void visited() {
        this.visited = true;
    }

    /**
     * @return the Vx
     */
    public double[][] getVx() {
        return Vx;
    }

    /**
     * @param Vx the Vx to set
     */
    public void setVx(double[][] Vx) {
        this.Vx = Vx;
    }

    /**
     * @return the Dx
     */
    public double[][] getDx() {
        return Dx;
    }

    /**
     * @param Dx the Dx to set
     */
    public void setDx(double[][] Dx) {
        this.Dx = Dx;
    }

    /**
     * @return the pathfile
     */
    public String getPathfile() {
        return pathfile;
    }

    /**
     * @param pathfile the pathfile to set
     */
    public void setPathfile(String pathfile) {
        this.pathfile = pathfile;
    }

    /**
     * @return the organos
     */
    public HashMap<Integer, Organs> getOrganos() {
        return organos;
    }

    /**
     * @param organos the organos to set
     */
    public void setOrganos(HashMap<Integer, Organs> organos) {
        this.organos = organos;
    }

    /**
     * @return the beamletsMap
     */
    public int[][] getBeamletsMap() {
        return beamletsMap;
    }

    /**
     * @param beamletsMap the beamletsMap to set
     */
    public void setBeamletsMap(int[][] beamletsMap) {
        this.beamletsMap = beamletsMap;
    }
    
    
     /**
     * @return the ampl
     */
    public AMPL_Solver getAmpl() {
        return ampl;
    }

    /**
     * @param ampl the ampl to set
     */
    public void setAmpl(AMPL_Solver ampl) {
        this.ampl = ampl;
    }
    
    public boolean generateDDM(String folderDB){
        boolean isOk = true;
        int auxTotalGenerated = 0;
        for(Organs organo:organos.values()){
            
            for(int i:selAngles){
                int respuesta = 0;
                respuesta = organo.createDDM(i, folderDB,beamletsMap[i][1]);
                if(respuesta == -1){
                    isOk = false;
                }
                auxTotalGenerated += respuesta;
            }
            
        }
        if(isOk)
            System.out.println("Se han generado "+auxTotalGenerated+ " DDMs");
        return isOk;
    }
    
    public boolean generateDDM(String folderDB, int[] all_beams){
        boolean isOk = true;
        int auxTotalGenerated = 0;
        for(Organs organo:organos.values()){
            
            for(int i:all_beams){
                int respuesta = 0;
                respuesta = organo.createDDM(i, folderDB,beamletsMap[i][1]);
                if(respuesta == -1){
                    isOk = false;
                }
                auxTotalGenerated += respuesta;
            }
            
        }
        if(isOk)
            System.out.println("Se han generado "+auxTotalGenerated+ " DDMs");
        return isOk;
    }

   
    public void iniorgansbeams(int [] selAngles, String path, String proccessName) throws IOException{
        this.selAngles = new int[selAngles.length];
        System.arraycopy(selAngles, 0, this.selAngles, 0, selAngles.length);
        //cargarBeams(path);
        for(int ind: organos.keySet()){
            organos.get(ind).iniorgansbeams(beamletsMap, selAngles, path);
            organos.get(ind).generateMapVoxel();
            organos.get(ind).writeMapVoxel(proccessName,selAngles);
        }
    }

    public void setAngles(int[] initialBAC) {
        selAngles = new int[initialBAC.length];
        if(initialBAC.length>0)
            System.arraycopy(initialBAC, 0, selAngles, 0, initialBAC.length);
    }

    public void setPrevAngles() {
        prevAngles = new int[selAngles.length];
        if(selAngles.length>0)
            System.arraycopy(selAngles, 0, prevAngles, 0, selAngles.length);
    }
    
    public void liberateMemory(){
        Vx = null;
        Dx = null;
        intensity = null;
        for(int organindex:organos.keySet()){
            organos.get(organindex).liberateMemory();
        }
        
    }   
    
    public void copyAngle(TreatmentPlan plan, int[] angulos){ // COPIA SIN REFERENCIA
        for(int o1: plan.organos.keySet()){
            Organs auxOrgans = plan.getOrganos().get(o1);
            for(int angulo:angulos){
                if(!auxOrgans.getBeams().containsKey(angulo))
                    organos.get(o1).newBeam(angulo, new Beam(auxOrgans.getBeams().get(angulo)));
            }
            
        }
    }

    private void markResolved() {
       for(int o1: organos.keySet()){
            Organs auxOrgans = getOrganos().get(o1);
            for(int angulo:selAngles){
                Beam auxBeam = auxOrgans.getBeams().get(angulo);
                auxBeam.setResolvedBeam(true);
            }
            
        }
    }
    
    public void cargarBeams(String path){
        System.out.println("Inicio de carga de BEAMS");
        File folder = new File("./");
        File[] listOfFiles = folder.listFiles();
        ArrayList<Integer> angulos = new ArrayList<>();
        for (int i = 0; i < listOfFiles.length; i++) {
          if (listOfFiles[i].isFile()) {
            String nameFile = listOfFiles[i].getName();
            String[] auxString = nameFile.split("\\.");
            if( auxString.length > 1)
                if(auxString[1].equals("dat")){
                    String[] name = auxString[0].split("_");
                    if(name.length>2){
                        if(name[0].equals("zDDM")){
                            if(!angulos.contains(Integer.valueOf(name[name.length-1]))){
                                angulos.add(Integer.valueOf(name[name.length-1]));
                            }
                        }
                    }
                }
          } 
          /*else if (listOfFiles[i].isDirectory()) {
            System.out.println("Directory " + listOfFiles[i].getName());
          }*/
        }
        for(Organs o1: organos.values()){
            for(int angulo: angulos){
                try {
                    o1.newBeam(angulo, new Beam(beamletsMap[angulo][1],angulo,true,path,o1.getName()));
                } catch (IOException ex) {
                    System.err.println("No pudo ser cargado el BEAM "+angulo+" "+ ex.getMessage());
                }
            }
           
        }
        System.out.println("Se han cargado "+angulos.size()+" ángulos");
    }
        
    public void createDVH(String jobThreadID,String input)
    throws IOException{
        for(int ang : selAngles){
            for(Organs organ : organos.values()){
                Beam rayo = organ.getBeams().get(ang);
                rayo.readIntensities(jobThreadID, input);
            }
        }
    }
    
    // MO IS DOMINATED FUNCTION
    public boolean isDominated(TreatmentPlan s) {
        boolean isDominated = true; //means, s dominates 'this'
        //We assume that first index in gEDU  corresponds to the target
        //which is equal for all the solutions
        for (int i = 1; i < this.gEUD.length - 1; i++) {
            if (this.gEUD[i] < s.gEUD[i]) {
                isDominated = false;
                break;
            }
        }
        return (isDominated);
    }
    
    public void generateReferencePoint_ajust(HashMap<Integer,Organs> o, int weighted, String solver, double maxIntensity, int opt,  String jobThreadID, int[] params) throws IOException {

        switch (opt) {
            case 1:
                this.solveLogFunction(maxIntensity,o,solver, jobThreadID,params);
                break;
           
           
        }

    }
    private void solveLogFunction(double maxIntensity, HashMap<Integer,Organs> o, String solver, String jobThreadID, int[] params) throws IOException {
        double[] EUD0 = new double[o.size()];
        for (int i = 0; i < o.size(); i++) {
            if (o.get(i).isIsTarget()) {
                EUD0[i] = o.get(i).getDoseLB(); //Target
            } else {
                EUD0[i] = o.get(i).getDoseUB(); //OAR
            }
        }

        ampl = new AMPL_Solver( this, solver, maxIntensity,jobThreadID); //-1 maxIntensity default
        ampl.new_params(params);
        /*##################################################
       # "generateParametersFile" method  per each option #
       # "generateParametersFile" method  per each option #
       #################################################*/
     
      
       
        System.out.println("Generating parameters File (1)");
        ampl.generateParametersFile_logFunction(this);
        
         
        ampl.runLogFunction_Solver(this);
        
        ampl.getSolution(this);
        //this.intensity = new double[this.beamlets];
        //System.arraycopy(ampl.getX(), 0, this.intensity, 0, this.beamlets);
        //calculate gEUDs
        System.arraycopy(getEUD(o, jobThreadID), 0, this.gEUD, 0, this.gEUD.length);
        this.getLogFunctionValue(o);
        markResolved();
    }
    
}
