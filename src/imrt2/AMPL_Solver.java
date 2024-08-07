package imrt2;

/* The function solves the problem using AMPL. To do that, the function
 * first create a script to be run from the AMPL terminal. It also creates a
 * .dat file with some specific parameter values (extraXXXXX.dat). 
 * Once the problem is solved, the function return solution  * vector "x". 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import com.ampl.AMPL;
import com.ampl.DataFrame;
import com.ampl.Objective;
import com.ampl.Parameter;
import com.ampl.Tuple;
import com.ampl.Variable;
import java.util.Comparator;
/**
 *
 * @author guille
 */
public class AMPL_Solver {
    private int organs;
    private int beams;
    private int[] bmlts;
    private int totalBmlts;
    private int[] angles;
    private int[] aPar;
    private int[] vPar;
    private double[] wPar; //weights
    private double[] EUD0Par;
    private double[] LB;
    private double[] UB;
    private boolean[] isTarget;
    private double epsilon;
    //public double t;
    private double[] x;
    private String jobThreadID;
    private String solver;
    private double maxIntensity;
    //public double PTV_UB;
    
     public AMPL_Solver(AMPL_Solver ampls){
        organs = ampls.getOrgans();
        beams = ampls.getBeams();
        bmlts = new int[ampls.getBmlts().length];
        if(ampls.getBmlts().length>0)
            System.arraycopy(ampls.getBmlts(), 0, bmlts, 0, ampls.getBmlts().length);
        totalBmlts = ampls.getTotalBmlts();
        angles = new int[ampls.getAngles().length];
        if(ampls.getAngles().length>0)
            System.arraycopy(ampls.getAngles(), 0, angles, 0, ampls.getAngles().length);
        aPar = new int[ampls.getaPar().length];
        if(ampls.getaPar().length>0)
            System.arraycopy(ampls.getaPar(), 0, aPar, 0, ampls.getaPar().length);
        vPar = new int[ampls.getvPar().length];
        if(ampls.getvPar().length>0)
            System.arraycopy(ampls.getvPar(), 0, vPar, 0, ampls.getvPar().length);
        wPar = new double[ampls.getwPar().length];
        if(ampls.getwPar().length>0)
            System.arraycopy(ampls.getwPar(), 0, wPar, 0, ampls.getwPar().length); //weights
        EUD0Par = new double[ampls.getEUD0Par().length];
        if(ampls.getEUD0Par().length>0)
            System.arraycopy(ampls.getEUD0Par(), 0, EUD0Par, 0, ampls.getEUD0Par().length);
        LB = new double[ampls.getLB().length];
        if(ampls.getLB().length>0)
            System.arraycopy(ampls.getLB(), 0, LB, 0, ampls.getLB().length);
        UB = new double[ampls.getUB().length];
        if(ampls.getUB().length>0)
            System.arraycopy(ampls.getUB(), 0, UB, 0, ampls.getUB().length);
        isTarget = new boolean[ampls.getIsTarget().length];
        if(ampls.getIsTarget().length>0)
            System.arraycopy(ampls.getIsTarget(), 0, isTarget, 0, ampls.getIsTarget().length);
        epsilon = ampls.getEpsilon();
        //public double t;
        x = new double[ampls.getX().length];
        if(ampls.getX().length>0)
            System.arraycopy(ampls.getX(), 0, x, 0, ampls.getX().length);
        jobThreadID = ampls.getJobThreadID();
        solver = ampls.getSolver();
        maxIntensity = ampls.getMaxIntensity();
    }
    
    
    /**
     * @param args the command line arguments
     */
    //public AMPL_Solver (Organs_bkp[] o,TreatmentPlan sol, double e) throws IOException {
    public AMPL_Solver (TreatmentPlan sol, String solver,double maxIntensity, String jobThreadID) 
            throws IOException {
        
        HashMap<Integer,Organs> o = sol.getOrganos();
        this.organs = o.size();
        this.beams = sol.beams;
        this.bmlts=new int[sol.selAngles.length];
        this.totalBmlts = sol.getBeamlets();
        this.angles= new int[this.beams];
        for(int i=0; i< this.beams; i++){
            this.angles[i] = sol.selAngles[i];
            Organs auxOrgans = sol.getOrganos().get(0);
            Beam auxBeam = auxOrgans.getBeams().get(sol.selAngles[i]);
            this.bmlts[i] = sol.getBeamletsMap()[sol.selAngles[i]][1];
        }
        this.aPar =new int[this.organs];
        this.vPar =new int[this.organs];
        this.EUD0Par =new double[this.organs];
        this.UB =new double[this.organs];
        this.LB =new double[this.organs];
        this.wPar =new double[this.organs];
        this.isTarget = new boolean[this.organs];
        for(int i=0; i< o.size(); i++){
            this.aPar[i] =  o.get(i).getA();
            this.vPar[i] =  o.get(i).getV();
            this.EUD0Par[i] =  o.get(i).getDesiredDose();
            this.UB[i] = o.get(i).getDoseUB();
            this.LB[i] = o.get(i).getDoseLB();
            this.isTarget[i]=o.get(i).isIsTarget();
            this.wPar[i] = o.get(i).getWeight();
            //if (o[i].isTarget){
            //    this.PTV_UB =  o[i].doseUB;
            //}
        }
        //this.t= o[0].doseLB;
        this.x = new double[this.totalBmlts]; 
        this.jobThreadID = jobThreadID;
        this.solver=solver;
        if(maxIntensity == -1){
            //default MaxIntensity 400
            this.maxIntensity=400;
        }else{
            this.maxIntensity=maxIntensity;
        }
        
    }
    
    
    
    public AMPL_Solver (TreatmentPlan sol, String solver,double maxIntensity, String jobThreadID, int[] angulos_sel) 
            throws IOException {
        
        HashMap<Integer,Organs> o = sol.getOrganos();
        this.organs = o.size();
        this.beams = angulos_sel.length;
        this.bmlts=new int[angulos_sel.length];
        this.totalBmlts = sol.getBeamlets();
        this.angles= new int[this.beams];
        for(int i=0; i< this.beams; i++){
            this.angles[i] = angulos_sel[i];
            Organs auxOrgans = sol.getOrganos().get(0);
            Beam auxBeam = auxOrgans.getBeams().get(angulos_sel[i]);
            this.bmlts[i] = sol.getBeamletsMap()[angulos_sel[i]][1];
        }
        this.aPar =new int[this.organs];
        this.vPar =new int[this.organs];
        this.EUD0Par =new double[this.organs];
        this.UB =new double[this.organs];
        this.LB =new double[this.organs];
        this.wPar =new double[this.organs];
        this.isTarget = new boolean[this.organs];
        for(int i=0; i< o.size(); i++){
            this.aPar[i] =  o.get(i).getA();
            this.vPar[i] =  o.get(i).getV();
            this.EUD0Par[i] =  o.get(i).getDesiredDose();
            this.UB[i] = o.get(i).getDoseUB();
            this.LB[i] = o.get(i).getDoseLB();
            this.isTarget[i]=o.get(i).isIsTarget();
            this.wPar[i] = o.get(i).getWeight();
            //if (o[i].isTarget){
            //    this.PTV_UB =  o[i].doseUB;
            //}
        }
        //this.t= o[0].doseLB;
        this.x = new double[this.totalBmlts]; 
        this.jobThreadID = jobThreadID;
        this.solver=solver;
        if(maxIntensity == -1){
            //default MaxIntensity 400
            this.maxIntensity=400;
        }else{
            this.maxIntensity=maxIntensity;
        }
        
    }
    
    
    
    public void generateParametersFile(TreatmentPlan tp) throws IOException{
        String parameterFile = "./"+this.jobThreadID + "extra.dat";
        //Deleting parameter file extra.txt
        
        try{
            File file = new File(parameterFile);
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(1)");
    		}
            }
    	}catch(Exception e){
            e.printStackTrace();
        }
        Random r = new Random();
        //creating the new file
        
        BufferedWriter bwParametersFile=null;
        bwParametersFile =createBufferedWriter(parameterFile);
        
        /*Organs auxOrgans = tp.getOrganos().get(0);
        for(int angulo : tp.selAngles){
            Beam auxBeam = auxOrgans.getBeams().get(angulo);
            if(auxBeam.isResolvedBeam()){
                writeLine("var x"+angulo+" := ", bwParametersFile);
                for (int k=0;k<tp.getBeamletsMap()[angulo][1];k++){
                    int j = k+1;
                    writeLine(j + " " + auxBeam.getX()[k] + "\t", bwParametersFile);
                }
                writeLine(";\n", bwParametersFile);
            }
        }*/
        
        
        
        writeLine("param a := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.aPar[i] + "\t", bwParametersFile);
        }
        //OAR-Target
        writeLine((this.organs+1) + " 10\t", bwParametersFile);
        writeLine(";\n", bwParametersFile);
        
        for (int i=0;i<this.organs;i++){
            int j=i+1;
            if (this.isTarget[i]){
                writeLine("param t := " + EUD0Par[i] + ";\n", bwParametersFile);
                writeLine("param OAR_targetUB := " + this.UB[i] + ";\n", bwParametersFile);
            }else{
                writeLine("param UB" + j + " := " + this.UB[i] + ";\n", bwParametersFile);
                //writeLine("param LB" + j + " := " + this.LB[i] + ";\n", bwParametersFile);
            }
        }
        
        //writeLine("param R2 := " + this.voxels[1] + ";\n", bwParametersFile);
        //writeLine("param R3 := " + this.voxels[2] + ";\n", bwParametersFile);
        totalBmlts = 0;
        for(int i = 0; i<bmlts.length; i++){
            writeLine("param bmlt"+angles[i]+" := " + bmlts[i] + ";\n", bwParametersFile);
            totalBmlts += bmlts[i];
        }
        
        writeLine("param totalbmlt := " + this.totalBmlts + ";\n", bwParametersFile);
        
        writeLine("param epsilon := " + this.epsilon + ";\n", bwParametersFile);
        
        
        
        
        bwParametersFile.close();
        
    }
    public void generateParametersFile(TreatmentPlan tp, double[] x) throws IOException{
        //Deleting parameter file extra.txt
        String parameterFile = "./"+this.jobThreadID + "extra.dat";
        try{
            File file = new File(parameterFile);
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(2)");
    		}
            }
    	}catch(Exception e){
    	}
        Random r = new Random();
        //creating the new file
        
        BufferedWriter bwParametersFile=null;
        bwParametersFile =createBufferedWriter(parameterFile);
        
        for(int i=0; i<bmlts.length;i++){
            writeLine("var x"+i+" := ", bwParametersFile);
            for (int k=0;k<bmlts[i];k++){
                int j = k+1;
                writeLine(j + " " + r.nextDouble()*50 + "\t", bwParametersFile);
            }
        }
        writeLine(";\n", bwParametersFile);
        
        writeLine("param a := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.aPar[i] + "\t", bwParametersFile);
        }
        //OAR-Target
        writeLine((this.organs+1) + " 10\t", bwParametersFile);
        writeLine(";\n", bwParametersFile);
        
        for (int i=0;i<this.organs;i++){
            int j=i+1;
            if (this.isTarget[i]){
                writeLine("param t := " + EUD0Par[i] + ";\n", bwParametersFile);
                writeLine("param OAR_targetUB := " + this.UB[i] + ";\n", bwParametersFile);
            }else{
                writeLine("param UB" + j + " := " + this.UB[i] + ";\n", bwParametersFile);
                //writeLine("param LB" + j + " := " + this.LB[i] + ";\n", bwParametersFile);
            }
        }
        //writeLine("param R1 := " + this.voxels[0] + ";\n", bwParametersFile);
        //writeLine("param R2 := " + this.voxels[1] + ";\n", bwParametersFile);
        //writeLine("param R3 := " + this.voxels[2] + ";\n", bwParametersFile);
        totalBmlts = 0;
        for(int i = 0; i<bmlts.length; i++){
            writeLine("param bmlt"+angles[i]+" := " + bmlts[i] + ";\n", bwParametersFile);
            totalBmlts += bmlts[i];
        }
        writeLine("param totalbmlt := " + this.totalBmlts + ";\n", bwParametersFile);
        
        writeLine("param epsilon := " + this.epsilon + ";\n", bwParametersFile);
        
        bwParametersFile.close();
    }
    
    public void generateParametersFile_logFunction(TreatmentPlan tp) throws IOException{
        String parameterFile = "./"+this.jobThreadID + "extraLogFunction.dat";
        //Deleting parameter file extra.txt
        
        System.out.println(parameterFile);  
        try{
            File file = new File(parameterFile);
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(3)");
    		}
            }
    	}catch(Exception e){
            e.printStackTrace();
    	}
        Random r = new Random();
        //creating the new file
        
        BufferedWriter bwParametersFile=null;
        bwParametersFile =createBufferedWriter(parameterFile);
        
        /*Organs auxOrgans = tp.getOrganos().get(0);
        for(int angulo : tp.selAngles){
            Beam auxBeam = auxOrgans.getBeams().get(angulo);
            if(auxBeam.isResolvedBeam()){
                writeLine("var x"+angulo+" := ", bwParametersFile);
                for (int k=0;k<tp.getBeamletsMap()[angulo][1];k++){
                    int j = k+1;
                    writeLine(j + " " + auxBeam.getX()[k] + "\t", bwParametersFile);
                }
                writeLine(";\n", bwParametersFile);
            }
        }*/
        
        writeLine("param a := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.aPar[i] + "\t", bwParametersFile);
        }
        //OAR-Target
        writeLine((this.organs+1) + " 10\t", bwParametersFile);//katty se cambio 10 por 50
        writeLine(";\n", bwParametersFile);
        
        writeLine(";\n", bwParametersFile);
        
        writeLine("param v := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.vPar[i] + "\t", bwParametersFile);
        }
        //OAR-Target
        writeLine((this.organs+1) +" 8\t", bwParametersFile);
        writeLine(";\n", bwParametersFile);
        
        writeLine("param EUD0 := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.EUD0Par[i] + "\t", bwParametersFile);
        }
        writeLine(";\n", bwParametersFile);
        
        for (int i=0;i<this.organs;i++){
            int j=i+1;
            if (this.isTarget[i]){
                writeLine("param t := " + EUD0Par[i] + ";\n", bwParametersFile);
                writeLine("param OAR_targetUB := " + this.UB[i] + ";\n", bwParametersFile);
            }else{
                writeLine("param UB" + j + " := " + this.UB[i] + ";\n", bwParametersFile);
                //writeLine("param LB" + j + " := " + this.LB[i] + ";\n", bwParametersFile);
            }
        }
        totalBmlts = 0;
        for(int i = 0; i<bmlts.length; i++){
            writeLine("param bmlt"+angles[i]+" := " + bmlts[i] + ";\n", bwParametersFile);
            totalBmlts += bmlts[i];
        }
        writeLine("param totalbmlt := " + this.totalBmlts + ";\n", bwParametersFile);
        
        writeLine("param epsilon := " + this.epsilon + ";\n", bwParametersFile);
        
        bwParametersFile.close();
        
    }
    
    public void generateParametersFile_logFunction(TreatmentPlan tp, double[] x) throws IOException{
        //Deleting parameter file extra.txt
        
        String parameterFile = "./" + this.jobThreadID + "extraLogFunction.dat";
        System.out.println(parameterFile);  
        try{
            File file = new File(parameterFile);
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(4)");
    		}
            }
    	}catch(Exception e){
    	}
        Random r = new Random();
        //creating the new file
        
        
        BufferedWriter bwParametersFile=null;
        bwParametersFile =createBufferedWriter(parameterFile);
        
        for(int i=0; i<bmlts.length;i++){
            writeLine("var x"+i+" := ", bwParametersFile);
            for (int k=0;k<bmlts[i];k++){
                int j = k+1;
                writeLine(j + " " + r.nextDouble()*50 + "\t", bwParametersFile);
            }
        }
        writeLine(";\n", bwParametersFile);
        
        writeLine("param a := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.aPar[i] + "\t", bwParametersFile);
        }
        //OAR-Target
        writeLine((this.organs+1) + " 10\t", bwParametersFile);
        writeLine(";\n", bwParametersFile);
        
        writeLine("param v := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.vPar[i] + "\t", bwParametersFile);
        }
        //OAR-Target
        writeLine((this.organs+1) + " 8\t", bwParametersFile);
        writeLine(";\n", bwParametersFile);
        
        writeLine("param EUD0 := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.EUD0Par[i] + "\t", bwParametersFile);
        }
        writeLine(";\n", bwParametersFile);
        
        for (int i=0;i<this.organs;i++){
            int j=i+1;
            if (this.isTarget[i]){
                writeLine("param t := " + EUD0Par[i] + ";\n", bwParametersFile);
                writeLine("param OAR_targetUB := " + this.UB[i] + ";\n", bwParametersFile);
            }else{
                writeLine("param UB" + j + " := " + this.UB[i] + ";\n", bwParametersFile);
                //writeLine("param LB" + j + " := " + this.LB[i] + ";\n", bwParametersFile);
            }
        }
        
        //writeLine("param R1 := " + this.voxels[0] + ";\n", bwParametersFile);
        //writeLine("param R2 := " + this.voxels[1] + ";\n", bwParametersFile);
        //writeLine("param R3 := " + this.voxels[2] + ";\n", bwParametersFile);
        totalBmlts = 0;
        for(int i = 0; i<bmlts.length; i++){
            writeLine("param bmlt"+angles[i]+" := " + bmlts[i] + ";\n", bwParametersFile);
            totalBmlts += bmlts[i];
        }
        
        writeLine("param totalbmlt := " + this.totalBmlts + ";\n", bwParametersFile);
        
        writeLine("param epsilon := " + this.epsilon + ";\n", bwParametersFile);
        //writeLine("param t := " + this.t + ";\n", bwParametersFile);
        
        bwParametersFile.close();
        
    }

    public void getSolution(TreatmentPlan tp) throws FileNotFoundException, IOException{
        HashMap<Integer,Double[]> auxX = new HashMap<>();
        for(int angulo:angles){
            double[] aux_solution = new double[tp.getBeamletsMap()[angulo][1]*2];
            double[] aux_X_sol = new double[tp.getBeamletsMap()[angulo][1]];
            String dir = this.jobThreadID + "currentSol_"+angulo+".txt";
            String[] auxReader=null;
            File f = new File(dir);
            if (f.exists()) {
                try(BufferedReader fileIn = new BufferedReader(new FileReader(f))){
                    String line = "";
                    line=fileIn.readLine(); //avoid first line;
                    line=fileIn.readLine();
                    auxReader = line.split(" ");
                    int j=0;
                    while (!";".equals(auxReader[0])){

                        for (String auxReader1 : auxReader) {
                            if (!"".equals(auxReader1)) {
                                aux_solution[j] = Double.parseDouble(auxReader1);
                                j++;
                            }
                        }
                        line=fileIn.readLine();
                        //System.out.println(line);
                        auxReader = line.split(" ");
                    }
                    j=0;
                    for (int i=0; i<aux_solution.length;i++){
                        if (i%2 == 0){
                            j=(int)aux_solution[i];
                        }else{
                            if (j>0){
                                aux_X_sol[j-1] = aux_solution[i];
                            }else{
                                System.err.print("Archivo Existe, pero hubo un error al leer " + jobThreadID + "currentSol.txt");
                                System.err.print("auxXaux_souliton = ");
                                for (int k=0; k<aux_solution.length;k++){
                                    System.err.print(aux_solution[k]+ " - ");
                                }
                                System.err.println();
                                System.err.print("x = ");
                                for (int k=0; k<this.x.length;k++){
                                    System.err.print(this.x[k]+ " - ");
                                }
                                aux_X_sol[j-1] = aux_solution[i];
                            }
                        }       
                    }
                    fileIn.close();
                    for(Organs o1 : tp.getOrganos().values()){
                        Beam beamAux = o1.getBeams().get(angulo);
                        beamAux.setX(aux_X_sol);
                    }
                 
                }
            }
        }
        
        /*else{
            renombrar_LogFunction_DDM(o); \\Maicholl cambia  los resultados malos
            System.out.println("ERROR: ./" + dir + "/ file wasn't generated");
            for(int i=0; i < this.x.length;i++){
                this.x[i]=0;
            }
        }*/
    }
    
    public void getSolution() throws FileNotFoundException, IOException{
        String dir = "./"+this.jobThreadID + "currentSol.txt";
        String[] auxReader=null;
        File f = new File(dir);
        if (f.exists()) {
            BufferedReader fileIn = new BufferedReader(new FileReader(f));
            String line = "";
            line=fileIn.readLine(); //avoid first line;
            line=fileIn.readLine();
            auxReader = line.split(" ");
            int j=0;
            double[] auxX = new double[this.x.length * 2];
            while (!";".equals(auxReader[0])){

                for (String auxReader1 : auxReader) {
                    if (!"".equals(auxReader1)) {
                        auxX[j] = Double.parseDouble(auxReader1);
                        j++;
                    }
                }
                line=fileIn.readLine();
                //System.out.println(line);
                auxReader = line.split(" ");
            }
            j=0;
            for (int i=0; i<auxX.length;i++){
                if (i%2 == 0){
                    j=(int)auxX[i];
                }else{
                    if (j>0){
                        x[j-1] = auxX[i];
                    }else{
                        System.err.print("ERROR al leer " + jobThreadID + "currentSol.txt");
                        System.err.print("auxX = ");
                        for (int k=0; k<auxX.length;k++){
                            System.err.print(auxX[k]+ " - ");
                        }
                        System.err.println();
                        System.err.print("x = ");
                        for (int k=0; k<this.x.length;k++){
                            System.err.print(this.x[k]+ " - ");
                        }
                        x[j-1] = auxX[i];
                    }
                }       
            }
        }else{
            System.out.println("ERROR: CurrentSol file wasn't generated");
        }
    }
    
    public void useThisSolution() throws FileNotFoundException, IOException{
        for(int angulo:angles){
            String dir = this.jobThreadID + "currentSol_"+angulo+".txt";
            String new_dir = "x"+angulo+".dat";
            File f = new File(dir);
            if (f.exists()) {
                try(BufferedReader fileIn = new BufferedReader(new FileReader(f))){
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new_dir, false))){
                        
                        String line = "";
                        line=fileIn.readLine(); //avoid first line;
                        if(line != null)
                            line = "var "+ line;
                        else{
                            System.out.print("Error leyendo x");
                        }
                        writer.write(line);
                        writer.newLine();
                        while((line=fileIn.readLine()) != null){
                            writer.write(line);
                            writer.newLine();
                            // copiamos al nuevo archivo de warm start  

                        }   
                        fileIn.close();
                        writer.close();
                    }
                }
            }
        }
    }

     private BufferedWriter createBufferedWriter(String route) throws IOException {
        BufferedWriter bw;
        File auxFile = new File(route);
                if (!auxFile.exists()) {
                    bw = new BufferedWriter(new FileWriter(route));
                }else{
                    bw = new BufferedWriter(new FileWriter(route,true));
                }
        return bw;     
    }
     
     
    public void runLogFunction_Solver ( TreatmentPlan tp) throws IOException{
        //Deleting Solution File currentSol.txt
        /*try{
            File file = new File("./"+this.jobThreadID + "currentSol.txt");
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(5)");
    		}
            }
    	}catch(Exception e){
    	}*/
        System.out.println("Creating " + this.jobThreadID + "scriptLogFunction.sh");  
        //Creating the script file
        String scriptFile = "./"+this.jobThreadID + "scriptLogFunction.sh";
        try{
            File file = new File(scriptFile);
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(6)");
    		}
            }
    	}catch(Exception e){
    	}
        
        //creating the new file
        
        BufferedWriter bwParametersFile=null;
        bwParametersFile =createBufferedWriter(scriptFile);
        
        
        writeLine("model " + this.jobThreadID +"logisticModel.mod;\n", bwParametersFile);
        for (Organs o1 : tp.getOrganos().values()) {
            for(int i = 0; i < tp.selAngles.length; i++){
                Beam auxBeam = o1.getBeams().get(tp.selAngles[i]);
                writeLine("data "+ "zDDM_" + o1.getName()+"_"+ auxBeam.getIndex() + ".dat;\n", bwParametersFile);
            }
            writeLine("data "+ jobThreadID+"_MapVoxel_" + o1.getName()+".dat;\n", bwParametersFile);
        }
        writeLine("data "+ this.jobThreadID +"extraLogFunction.dat;\n", bwParametersFile);
        if(tp.warm_start){
            boolean isPrev = false;
            for(int i=0; i<tp.prevAngles.length;i++){
                if(tp.prevAngles[i]!=0){
                    isPrev = true;
                    break;
                }
            }
            if(isPrev){
                ArrayList<Integer> list = new ArrayList<>();
                for(int a:tp.selAngles){
                    list.add(a);
                }
                for(int i=0; i<tp.prevAngles.length;i++){
                    if(list.contains(tp.prevAngles[i])){
                        writeLine("data "+ "x"+tp.prevAngles[i]+".dat;\n", bwParametersFile);
                    }
                }
            }
        }
        
        
        System.out.println("PASO 1");
        //writeLine("data "+ this.jobID +"DDM_RECTUM.dat;\n", bwParametersFile);
        //writeLine("data "+ this.jobID +"DDM_BLADDER.dat;\n", bwParametersFile);
        
        writeLine("solve;\n", bwParametersFile);
        
        for(int i=0; i<angles.length;i++){
            writeLine("display x"+ angles[i] +" > "+ this.jobThreadID +"currentSol_"+angles[i]+".txt;\n", bwParametersFile);
        }
        int orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            String st1 = "";
            String st2 = "";
            for(int cont=0; cont<tp.selAngles.length; cont++){
                /*else{
                        
                    writeLine("#OAR_UB"+(orgindex +1)+": 	((1/R"+(orgindex +1)+")*(sum {i in 1..R"+(orgindex +1)+"} (sum {j in 1..bmlt} "
                            + "x[j]*ddm"+o1.getName()+"[i,j])^a["+(orgindex +1)+"]))^(1/a["+(orgindex +1)+"]) <= UB"+(orgindex +1)+"; \n", bwParametersFile);
                }*/
                st1 = st1+"(sum {j in bmltsIndex_"+o1.getName()+"_"+tp.selAngles[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+tp.selAngles[cont]+"} "
                            + "x"+tp.selAngles[cont]+"[j]*intensities_"+o1.getName()+"_"+tp.selAngles[cont]+"[i,j]) ";
                
                st2 = st2+"(sum {j in bmltsIndex_"+o1.getName()+"_"+tp.selAngles[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+tp.selAngles[cont]+"} "
                            + "x"+tp.selAngles[cont]+"[j]*intensities_"+o1.getName()+"_"+tp.selAngles[cont]+"[i,j])"+" ";
                 if(cont != tp.selAngles.length-1){
                        st1 = st1 + "+";
                        st2 = st2 + "+";
                    }
            }
            String sumatoria = "";
            for(int cont=0; cont<tp.selAngles.length; cont++){
                Beam auxBeam = o1.getBeams().get(tp.selAngles[cont]);
                sumatoria = sumatoria+"(sum{j in bmltsIndex_"+o1.getName()+"_"+tp.selAngles[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+tp.selAngles[cont]+"} "
                    + "x"+tp.selAngles[cont]+"[j]*intensities_"+o1.getName()+"_"+tp.selAngles[cont]+"[i,j])"+" ";
                if(cont != tp.selAngles.length-1){
                    sumatoria = sumatoria + "+";
                }
            }
            writeLine("display ( "+ "((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"}"+"( "+st1+")^a["+(orgindex +1)+"])))^(1/a["+(orgindex +1)+"])" + " > " + this.jobThreadID + "gEUD_" + o1.getName() + ".txt;\n", bwParametersFile); 
            writeLine(" display {i in voxelIndex_"+o1.getName()+"} "+st1+" > " + this.jobThreadID + "DVH_" + o1.getName() + ".txt;\n", bwParametersFile); 
            if (o1.isIsTarget()){
                writeLine("display (" + "((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"} "
                        + "("+sumatoria+")^a["+(tp.getOrganos().values().size() +1)+"])))^(1/a["+(tp.getOrganos().values().size() +1)+"]) > " + this.jobThreadID + "gEUD_" + o1.getName() + "_UB.txt;\n", bwParametersFile); 
            }
            orgindex++;
        }
        
        
        /*for (Organs o1 : o) {
            writeLine("display gEUD_" + o1.name + " > " + this.jobThreadID + "gEUD_" + o1.name + ".txt;\n", bwParametersFile);
            writeLine("display d_" + o1.name + " > " + this.jobThreadID + "dvh_" + o1.name + ".txt;\n", bwParametersFile); 
            if (o1.isTarget){
                writeLine("display gEUD_" + o1.name + "_UB > "+"gEUD_" + o1.name + "_UB.txt;\n", bwParametersFile);
            }
        }*/
        
         bwParametersFile.close();
        
        /*CREATING THE LOGISTIC MODEL FILE FOR AMPL*/
        
        System.out.println("Creating " + this.jobThreadID + "logisticModel.mod");  
        //Creating the script file
        scriptFile = this.jobThreadID + "logisticModel.mod";
        try{
            File file = new File(scriptFile);
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(6)");
    		}
            }
    	}catch(Exception e){
    	}
        //creating the new file
        bwParametersFile=null;
        bwParametersFile =createBufferedWriter(scriptFile);
        
        
        writeLine("option solver "+this.solver+"; \n", bwParametersFile);
        //writeLine("options ipopt_options \"linear_solver=ma57 linear_system_scaling=mc19 wantsol=8 print_level=4 tol=0.0001\"; \n", bwParametersFile);
        if(tp.warm_start)
            writeLine("options "+this.solver+"_options \"wantsol=8 \t outlev=1 \t hessopt=6 \t strat_warm_start=1\"; \n", bwParametersFile);
        else
            writeLine("options "+this.solver+"_options \"wantsol=8 \t hessopt=6 \t outlev=1\"; \n", bwParametersFile);
        
        /*writeLine("option solver ipopt; \n", bwParametersFile);
        //writeLine("options ipopt_options \"linear_solver=ma57 linear_system_scaling=mc19 wantsol=8 print_level=4 tol=0.0001\"; \n", bwParametersFile);
        writeLine("options ipopt_options \"wantsol=8 print_level=4 tol=0.0001\"; \n", bwParametersFile);*/
        orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            
            writeLine("param R_"+tp.getOrganos().get(orgindex).getName()+ ";\n", bwParametersFile);
            writeLine("set voxelIndex_"+tp.getOrganos().get(orgindex).getName()+ ";\n", bwParametersFile);
            for(int cont=0; cont<angles.length; cont++){
                Beam beam_aux = o1.getBeams().get(tp.selAngles[cont]);
                //writeLine("param R" + (orgindex +1)+"_"+ angles[cont] + "; #number of voxels of " + o1.getName() + "\n", bwParametersFile);
                writeLine("set bmltsIndex_"+tp.getOrganos().get(orgindex).getName()+"_"+ angles[cont] +" ;\n", bwParametersFile);
                
                writeLine("set voxelbmlt_"+tp.getOrganos().get(orgindex).getName()+"_"+ angles[cont]+" within {voxelIndex_"+tp.getOrganos().get(orgindex).getName()+ ",bmltsIndex_"+tp.getOrganos().get(orgindex).getName()+"_"+ angles[cont] +"};\n", bwParametersFile);
                writeLine("param intensities_"+tp.getOrganos().get(orgindex).getName()+"_"+ angles[cont]+" {voxelbmlt_"+tp.getOrganos().get(orgindex).getName()+"_"+ angles[cont]+"} >=0;\n", bwParametersFile);
            }
            orgindex++;
        }
        
        for(int i = 0; i<bmlts.length; i++){
            writeLine("param bmlt"+ angles[i] +";\n", bwParametersFile);
        }
        
        writeLine("param totalbmlt; 	#number of beamlets \n", bwParametersFile);
        
       
        orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            writeLine("param UB" + (orgindex +1) + ";\n", bwParametersFile);
            orgindex++;
        }
        writeLine("param t;\n" + "param epsilon;\n" + "param OAR_targetUB;\n", bwParametersFile);
        for(int num=0;num<tp.selAngles.length;num++){
            writeLine("var x"+tp.selAngles[num]+" {1 .. "+bmlts[num]+"} >= 0, <="+maxIntensity+", default 1; \n", bwParametersFile);
        }
        writeLine("param a{1 .. "+ (tp.getOrganos().values().size() + 1) + "}; \n", bwParametersFile);
        writeLine("param v{1 .. "+ (tp.getOrganos().values().size() + 1) + "}; \n", bwParametersFile);
        writeLine("param EUD0{1 .. "+ (tp.getOrganos().values().size() + 1) + "}; \n", bwParametersFile);
        for (Organs o1 : tp.getOrganos().values()) {
            writeLine("var intensityVoxel_"+o1.getName()+"{1 .. "+o1.getMapVoxel().size()+ "}; \n", bwParametersFile);
        }
        
        
        
        /*for (Organs o1 : o) {
            writeLine("par d_" + o1.name + " {i in 1 .. R"+(o1.index +1)+"} = (sum {j in 1..bmlt} x[j]*ddm"+o1.name+"[i,j]); \n", bwParametersFile); 
        }*/
        
        writeLine("minimize Total_Cost: ", bwParametersFile);
        orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            if (!o1.isIsTarget()){
                String sumatoria = "";
                for(int cont=0; cont<tp.selAngles.length; cont++){
                    Beam auxBeam = o1.getBeams().get(tp.selAngles[cont]);
                    sumatoria = sumatoria+"(sum{j in bmltsIndex_"+o1.getName()+"_"+tp.selAngles[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+tp.selAngles[cont]+"} "
                        + "x"+tp.selAngles[cont]+"[j]*intensities_"+o1.getName()+"_"+tp.selAngles[cont]+"[i,j])"+" \n";
                    if(cont != tp.selAngles.length-1){
                        sumatoria = sumatoria + "+";
                    }
                }
                String cadenaString = "- log((1+(( "+ "((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"} ("+"\n"+sumatoria+")^a["+(orgindex +1)+"]))"+" )^(1/a["+(orgindex +1)+"])/EUD0["+(orgindex +1)+"])^v["+(orgindex +1)+"])^-1)";
                    writeLine(cadenaString, bwParametersFile);
                
            }
            orgindex++;
        }
        writeLine(";\n s.t. \n", bwParametersFile);
        orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            String st1 = "";
            String st2 = "";
            for(int cont=0; cont<tp.selAngles.length; cont++){
                /*else{
                        
                    writeLine("#OAR_UB"+(orgindex +1)+": 	((1/R"+(orgindex +1)+")*(sum {i in 1..R"+(orgindex +1)+"} (sum {j in 1..bmlt} "
                            + "x[j]*ddm"+o1.getName()+"[i,j])^a["+(orgindex +1)+"]))^(1/a["+(orgindex +1)+"]) <= UB"+(orgindex +1)+"; \n", bwParametersFile);
                }*/
                st1 = st1+"(sum {j in bmltsIndex_"+o1.getName()+"_"+tp.selAngles[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+tp.selAngles[cont]+"} "
                            + "x"+tp.selAngles[cont]+"[j]*intensities_"+o1.getName()+"_"+tp.selAngles[cont]+"[i,j]) \n";
                
                st2 = st2+"(sum {j in bmltsIndex_"+o1.getName()+"_"+tp.selAngles[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+tp.selAngles[cont]+"} "
                            + "x"+tp.selAngles[cont]+"[j]*intensities_"+o1.getName()+"_"+tp.selAngles[cont]+"[i,j])"+" \n";
                 if(cont !=  tp.selAngles.length-1){
                        st1 = st1 + "+";
                        st2 = st2 + "+";
                    }
            }
            if (o1.isIsTarget()){
                    writeLine("equalityTarget: 	( "+ "((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"}"+"( \n"+st1+")^a["+(orgindex +1)+"])))^(1/a["+(orgindex +1)+"]) >= t; \n", bwParametersFile);
                    writeLine("constraintOAR_Target: 	("+"((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"} ("+" \n "+st2+")^a["+(tp.getOrganos().size() +1)+"]))"+")^(1/a["+(tp.getOrganos().size() +1)+"]) <=OAR_targetUB;\n", bwParametersFile);
                }
            orgindex++;
        }
        
        bwParametersFile.close();
        String bacmsg = "";
        for (int anguloAux :  tp.selAngles){
            bacmsg += " "+ anguloAux + " -"; 
        }
        System.out.println("PASO 2 : Solving BAC "+ bacmsg);
        //Running Process
        scriptFile = this.jobThreadID + "scriptLogFunction.sh";
        String dir = jobThreadID + "currentSol.txt";
        File direc= new File(dir);
        
        try{
            int exitAMPL=-1;
            Process p=null;
            int wait=-1;
            /*do{
                if(exitAMPL!=0){
                    p= new ProcessBuilder("ampl", scriptFile).start();
                    p.waitFor();
                }
                exitAMPL= p.exitValue();
            }while(exitAMPL!=0);
            */
            do{
                p= new ProcessBuilder("ampl", scriptFile).start();
               
                while(!p.waitFor(10, TimeUnit.MINUTES)){

                }
                exitAMPL= p.exitValue();
            }while(exitAMPL!=0);
            
            System.out.print("\n"+p.exitValue()+"\n");
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            System.out.print("Solving Log Function (Wu et al., 2002): ");
            //System.out.print("Angles: ");
            for(int i=0; i< this.beams; i++){
                System.out.print(" " +this.angles[i]+ " -- ");
            }
            System.out.print(" // ");
            for(int i=0; i< this.EUD0Par.length; i++){
                System.out.print(this.EUD0Par[i]+ " - ");
            }
            System.out.println();

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }catch(IOException e){
            e.printStackTrace();
        }catch(Exception e){
            System.out.println("\033[0;31m \n"+e.toString()+"\n");
        }
        //Process p = new ProcessBuilder("ampl", scriptFile).start();
        /*try {
            p.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(AMPL_Solver.class.getName()).log(Level.SEVERE, null, ex);
        }
        File f = new File(dir);
        while(!f.exists()){
            p = new ProcessBuilder("ampl", scriptFile).start();
            try {
                p.waitFor();
            } catch (InterruptedException ex) {
                //Logger.getLogger(AMPL_Solver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }*/
        
    }
    
    
    public void writeLine(String l, BufferedWriter bw) throws IOException{
	
	String row;
	row = l; 
	bw.write(row);
	

    }
    
    private void renombrar_LogFunction_DDM(Organs[] o,String BACname) {
        for(Organs o1 : o){
                File file = new File(this.jobThreadID+ "zDDM_"+o1.getName()+".dat");
                File file2 = new File(this.jobThreadID+ "zDDM_"+o1.getName()+"_ERROR.dat");
                if(file.renameTo(file2)){
                    System.out.println("Se renombró por error: "+ this.jobThreadID + "DDM_"+o1.getName()+".dat");
                }
            }
            File file = new File(this.jobThreadID+ "extraLogFunction.dat");
            File file2 = new File(this.jobThreadID+ "extraLogFunction_ERROR.dat");
            if(file.renameTo(file2)){
                System.out.println("Se renombró por error: "+ this.jobThreadID + "extraLogFunction.dat");
            }
            file = new File(this.jobThreadID+ "scriptLogFunction.sh");
            file2 = new File(this.jobThreadID+ "scriptLogFunction_ERROR.sh");
            if(file.renameTo(file2)){
                System.out.println("Se renombró por error: "+ this.jobThreadID + "scriptLogFunction.sh");
            }
            file = new File(this.jobThreadID+ "logisticModel.mod");
            file2 = new File(this.jobThreadID+ "logisticModel_ERROR.mod");
            if(file.renameTo(file2)){
                System.out.println("Se renombró por error: "+ this.jobThreadID + "logisticModel.mod");
            }
    }
    
    public void generateParametersFile_rMIPlogFunction(TreatmentPlan tp, int[] fixed, int[] all_ang) throws IOException{
        String parameterFile = "./"+this.jobThreadID + "extraLogFunction.dat";
        //Deleting parameter file extra.txt
        HashMap<Integer,Integer> map_angles = new HashMap<>();
        for(int k = 0; k < all_ang.length; k++){
            map_angles.put(all_ang[k], k+1);
        }
        System.out.println(parameterFile);  
        try{
            File file = new File(parameterFile);
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(3)");
    		}
            }
    	}catch(Exception e){
            e.printStackTrace();
    	}
        Random r = new Random();
        //creating the new file
        
        BufferedWriter bwParametersFile=null;
        bwParametersFile =createBufferedWriter(parameterFile);
        
        /*Organs auxOrgans = tp.getOrganos().get(0);
        for(int angulo : tp.selAngles){
            Beam auxBeam = auxOrgans.getBeams().get(angulo);
            if(auxBeam.isResolvedBeam()){
                writeLine("var x"+angulo+" := ", bwParametersFile);
                for (int k=0;k<tp.getBeamletsMap()[angulo][1];k++){
                    int j = k+1;
                    writeLine(j + " " + auxBeam.getX()[k] + "\t", bwParametersFile);
                }
                writeLine(";\n", bwParametersFile);
            }
        }*/
        
        writeLine("param a := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.aPar[i] + "\t", bwParametersFile);
        }
        //OAR-Target
        writeLine((this.organs+1) + " 10\t", bwParametersFile);//katty se cambio 10 por 50
        writeLine(";\n", bwParametersFile);
        
        writeLine(";\n", bwParametersFile);
        
        writeLine("param v := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.vPar[i] + "\t", bwParametersFile);
        }
        //OAR-Target
        writeLine((this.organs+1) +" 8\t", bwParametersFile);
        writeLine(";\n", bwParametersFile);
        
        writeLine("param EUD0 := ", bwParametersFile);
        for (int i=0;i<this.organs;i++){
            int j = i+1;
            writeLine(j + " " + this.EUD0Par[i] + "\t", bwParametersFile);
        }
        writeLine(";\n", bwParametersFile);
        
        for (int i=0;i<this.organs;i++){
            int j=i+1;
            if (this.isTarget[i]){
                writeLine("param t := " + EUD0Par[i] + ";\n", bwParametersFile);
                writeLine("param OAR_targetUB := " + this.UB[i] + ";\n", bwParametersFile);
            }else{
                writeLine("param UB" + j + " := " + this.UB[i] + ";\n", bwParametersFile);
                //writeLine("param LB" + j + " := " + this.LB[i] + ";\n", bwParametersFile);
            }
        }
        totalBmlts = 0;
        for(int i = 0; i<bmlts.length; i++){
            writeLine("param bmlt"+angles[i]+" := " + bmlts[i] + ";\n", bwParametersFile);
            totalBmlts += bmlts[i];
        }
        writeLine("param totalbmlt := " + this.totalBmlts + ";\n", bwParametersFile);
        
        writeLine("param epsilon := " + this.epsilon + ";\n", bwParametersFile);
        
        writeLine("param numBeams := "+all_ang.length+";\n", bwParametersFile);
        writeLine("param n := 5;\n", bwParametersFile);
        
        for(int i = 0; i < fixed.length; i++){
            writeLine("param fix_b"+i+" := "+map_angles.get(fixed[i])+";\n", bwParametersFile);
        }
        
        
        bwParametersFile.close();
        
    }
    
    public AMPL runrMIP_LogFunction_Solver ( TreatmentPlan tp, int[] fixed, AMPL ampl, int[] angulos_sel, String timerFile) throws IOException{
        long prevTimer = System.currentTimeMillis(); 
        //Deleting Solution File currentSol.txt
        /*try{
            File file = new File("./"+this.jobThreadID + "currentSol.txt");
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(5)");
    		}
            }
    	}catch(Exception e){
    	}*/
        
        System.out.println("Creating " + this.jobThreadID + "scriptLogFunction.sh");  
        //Creating the script file
        String scriptFile = "./"+this.jobThreadID + "scriptLogFunction.sh";
        try{
            File file = new File(scriptFile);
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(6)");
    		}
            }
    	}catch(Exception e){
    	}
        
        //creating the new file
        
        BufferedWriter bwParametersFile=null;
        bwParametersFile =createBufferedWriter(scriptFile);
        
        
        writeLine("model " + this.jobThreadID +"log_rMipModel.mod;\n", bwParametersFile);
        for (Organs o1 : tp.getOrganos().values()) {
            for(int i = 0; i < angulos_sel.length; i++){
                Beam auxBeam = o1.getBeams().get(angulos_sel[i]);
                writeLine("data "+ "zDDM_" + o1.getName()+"_"+ auxBeam.getIndex() + ".dat;\n", bwParametersFile);
            }
            writeLine("data "+ jobThreadID+"_MapVoxel_" + o1.getName()+".dat;\n", bwParametersFile);
        }
        writeLine("data "+ this.jobThreadID +"extraLogFunction.dat;\n", bwParametersFile);
        if(tp.warm_start){
            for(int i=0; i<fixed.length;i++){
                writeLine("data "+ "x"+fixed[i]+".dat;\n", bwParametersFile);
                
            }
           
        }
        
        
        System.out.println("PASO 1");
        //writeLine("data "+ this.jobID +"DDM_RECTUM.dat;\n", bwParametersFile);
        //writeLine("data "+ this.jobID +"DDM_BLADDER.dat;\n", bwParametersFile);
        
        writeLine("solve;\n", bwParametersFile);
        
        for(int i=0; i<angulos_sel.length;i++){
            writeLine("display x"+ angulos_sel[i] +" > "+ this.jobThreadID +"currentSol_"+angulos_sel[i]+".txt;\n", bwParametersFile);
        }
        int orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            String st1 = "";
            String st2 = "";
            for(int cont=0; cont<angulos_sel.length; cont++){
                /*else{
                        
                    writeLine("#OAR_UB"+(orgindex +1)+": 	((1/R"+(orgindex +1)+")*(sum {i in 1..R"+(orgindex +1)+"} (sum {j in 1..bmlt} "
                            + "x[j]*ddm"+o1.getName()+"[i,j])^a["+(orgindex +1)+"]))^(1/a["+(orgindex +1)+"]) <= UB"+(orgindex +1)+"; \n", bwParametersFile);
                }*/
                st1 = st1+"(sum {j in bmltsIndex_"+o1.getName()+"_"+angulos_sel[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+angulos_sel[cont]+"} "
                            + "x"+angulos_sel[cont]+"[j]*intensities_"+o1.getName()+"_"+angulos_sel[cont]+"[i,j]) ";
                
                st2 = st2+"(sum {j in bmltsIndex_"+o1.getName()+"_"+angulos_sel[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+angulos_sel[cont]+"} "
                            + "x"+angulos_sel[cont]+"[j]*intensities_"+o1.getName()+"_"+angulos_sel[cont]+"[i,j])"+" ";
                 if(cont != angulos_sel.length-1){
                        st1 = st1 + "+";
                        st2 = st2 + "+";
                    }
            }
            String sumatoria = "";
            for(int cont=0; cont<angulos_sel.length; cont++){
                Beam auxBeam = o1.getBeams().get(angulos_sel[cont]);
                sumatoria = sumatoria+"(sum{j in bmltsIndex_"+o1.getName()+"_"+angulos_sel[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+angulos_sel[cont]+"} "
                    + "x"+angulos_sel[cont]+"[j]*intensities_"+o1.getName()+"_"+angulos_sel[cont]+"[i,j])"+" ";
                if(cont != angulos_sel.length-1){
                    sumatoria = sumatoria + "+";
                }
            }
            writeLine("display ( "+ "((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"}"+"( "+st1+")^a["+(orgindex +1)+"])))^(1/a["+(orgindex +1)+"])" + " > " + this.jobThreadID + "gEUD_" + o1.getName() + ".txt;\n", bwParametersFile); 
            writeLine(" display {i in voxelIndex_"+o1.getName()+"} "+st1+" > " + this.jobThreadID + "DVH_" + o1.getName() + ".txt;\n", bwParametersFile); 
            if (o1.isIsTarget()){
                writeLine("display (" + "((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"} "
                        + "("+sumatoria+")^a["+(tp.getOrganos().values().size() +1)+"])))^(1/a["+(tp.getOrganos().values().size() +1)+"]) > " + this.jobThreadID + "gEUD_" + o1.getName() + "_UB.txt;\n", bwParametersFile); 
            }
            orgindex++;
        }
        
        
        /*for (Organs o1 : o) {
            writeLine("display gEUD_" + o1.name + " > " + this.jobThreadID + "gEUD_" + o1.name + ".txt;\n", bwParametersFile);
            writeLine("display d_" + o1.name + " > " + this.jobThreadID + "dvh_" + o1.name + ".txt;\n", bwParametersFile); 
            if (o1.isTarget){
                writeLine("display gEUD_" + o1.name + "_UB > "+"gEUD_" + o1.name + "_UB.txt;\n", bwParametersFile);
            }
        }*/
        
         bwParametersFile.close();
        
        /*CREATING THE LOGISTIC MODEL FILE FOR AMPL*/
        
        System.out.println("Creating " + this.jobThreadID + "log_rMipModel.mod");  
        //Creating the script file
        scriptFile = this.jobThreadID + "log_rMipModel.mod";
        try{
            File file = new File(scriptFile);
            if (file.exists()) {
    		if(!file.delete()){
                    System.out.println("Delete operation failed.(6)");
    		}
            }
    	}catch(Exception e){
    	}
        //creating the new file
        bwParametersFile=null;
        bwParametersFile =createBufferedWriter(scriptFile);
        
        
        writeLine("option solver "+this.solver+"; \n", bwParametersFile);
        //writeLine("options ipopt_options \"linear_solver=ma57 linear_system_scaling=mc19 wantsol=8 print_level=4 tol=0.0001\"; \n", bwParametersFile);
        if(tp.warm_start)
            writeLine("options "+this.solver+"_options \"wantsol=8 \t mip_selectdir=1 \t outlev=1 \t hessopt=6 \t strat_warm_start=1 relax=1\"; \n", bwParametersFile);
        else
            writeLine("options "+this.solver+"_options \"wantsol=8 \t mip_selectdir=1 \t hessopt=6 \t outlev=1 relax=1\"; \n", bwParametersFile);
        
        /*writeLine("option solver ipopt; \n", bwParametersFile);
        //writeLine("options ipopt_options \"linear_solver=ma57 linear_system_scaling=mc19 wantsol=8 print_level=4 tol=0.0001\"; \n", bwParametersFile);
        writeLine("options ipopt_options \"wantsol=8 print_level=4 tol=0.0001\"; \n", bwParametersFile);*/
        orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            
            writeLine("param R_"+tp.getOrganos().get(orgindex).getName()+ ";\n", bwParametersFile);
            writeLine("set voxelIndex_"+tp.getOrganos().get(orgindex).getName()+ ";\n", bwParametersFile);
            for(int cont=0; cont<angulos_sel.length; cont++){
                Beam beam_aux = o1.getBeams().get(angulos_sel[cont]);
                //writeLine("param R" + (orgindex +1)+"_"+ angles[cont] + "; #number of voxels of " + o1.getName() + "\n", bwParametersFile);
                writeLine("set bmltsIndex_"+tp.getOrganos().get(orgindex).getName()+"_"+ angulos_sel[cont] +" ;\n", bwParametersFile);
                
                writeLine("set voxelbmlt_"+tp.getOrganos().get(orgindex).getName()+"_"+ angulos_sel[cont]+" within {voxelIndex_"+tp.getOrganos().get(orgindex).getName()+ ",bmltsIndex_"+tp.getOrganos().get(orgindex).getName()+"_"+ angles[cont] +"};\n", bwParametersFile);
                writeLine("param intensities_"+tp.getOrganos().get(orgindex).getName()+"_"+ angulos_sel[cont]+" {voxelbmlt_"+tp.getOrganos().get(orgindex).getName()+"_"+ angles[cont]+"} >=0;\n", bwParametersFile);
            }
            orgindex++;
        }
        
        for(int i = 0; i<bmlts.length; i++){
            writeLine("param bmlt"+ angulos_sel[i] +";\n", bwParametersFile);
        }
        
        writeLine("param totalbmlt; 	#number of beamlets \n", bwParametersFile);
        
       
        orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            writeLine("param UB" + (orgindex +1) + ";\n", bwParametersFile);
            orgindex++;
        }
        writeLine("param t;\n" + "param epsilon;\n" + "param OAR_targetUB;\n", bwParametersFile);
        for(int num=0;num<angulos_sel.length;num++){
            writeLine("var x"+angulos_sel[num]+" {1 .. "+bmlts[num]+"} >= 0, <="+maxIntensity+", default 1; \n", bwParametersFile);
        }
        writeLine("param a{1 .. "+ (tp.getOrganos().values().size() + 1) + "}; \n", bwParametersFile);
        writeLine("param v{1 .. "+ (tp.getOrganos().values().size() + 1) + "}; \n", bwParametersFile);
        writeLine("param EUD0{1 .. "+ (tp.getOrganos().values().size() + 1) + "}; \n", bwParametersFile);
        for (Organs o1 : tp.getOrganos().values()) {
            writeLine("var intensityVoxel_"+o1.getName()+"{1 .. "+o1.getMapVoxel().size()+ "}; \n", bwParametersFile);
        }
        
        writeLine("\n param numBeams; #Total Number of Beams\n" + "param n; #Number of Selected Beams\n", bwParametersFile);
        writeLine("\n var beams{1 .. numBeams} binary;\n", bwParametersFile);
        for(int i = 0; i < fixed.length; i++){
            writeLine("param fix_b"+i+";\n", bwParametersFile);
        }
        
        /*for (Organs o1 : o) {
            writeLine("par d_" + o1.name + " {i in 1 .. R"+(o1.index +1)+"} = (sum {j in 1..bmlt} x[j]*ddm"+o1.name+"[i,j]); \n", bwParametersFile); 
        }*/
        
        writeLine("minimize Total_Cost: ", bwParametersFile);
        orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            if (!o1.isIsTarget()){
                String sumatoria = "";
                for(int cont=0; cont<angulos_sel.length; cont++){
                    Beam auxBeam = o1.getBeams().get(angulos_sel[cont]);
                    sumatoria = sumatoria+"(sum{j in bmltsIndex_"+o1.getName()+"_"+angulos_sel[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+angulos_sel[cont]+"} "
                        + "x"+angulos_sel[cont]+"[j]*intensities_"+o1.getName()+"_"+angulos_sel[cont]+"[i,j])"+" \n";
                    if(cont != angulos_sel.length-1){
                        sumatoria = sumatoria + "+";
                    }
                }
                String cadenaString = "- log((1+(( "+ "((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"} ("+"\n"+sumatoria+")^a["+(orgindex +1)+"]))"+" )^(1/a["+(orgindex +1)+"])/EUD0["+(orgindex +1)+"])^v["+(orgindex +1)+"])^-1)";
                    writeLine(cadenaString, bwParametersFile);
                
            }
            orgindex++;
        }
        writeLine(";\n s.t. \n", bwParametersFile);
        orgindex = 0;
        for (Organs o1 : tp.getOrganos().values()) {
            String st1 = "";
            String st2 = "";
            for(int cont=0; cont<angulos_sel.length; cont++){
                /*else{
                        
                    writeLine("#OAR_UB"+(orgindex +1)+": 	((1/R"+(orgindex +1)+")*(sum {i in 1..R"+(orgindex +1)+"} (sum {j in 1..bmlt} "
                            + "x[j]*ddm"+o1.getName()+"[i,j])^a["+(orgindex +1)+"]))^(1/a["+(orgindex +1)+"]) <= UB"+(orgindex +1)+"; \n", bwParametersFile);
                }*/
                st1 = st1+"(sum {j in bmltsIndex_"+o1.getName()+"_"+angulos_sel[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+angulos_sel[cont]+"} "
                            + "x"+angulos_sel[cont]+"[j]*intensities_"+o1.getName()+"_"+angulos_sel[cont]+"[i,j]) \n";
                
                st2 = st2+"(sum {j in bmltsIndex_"+o1.getName()+"_"+angulos_sel[cont]+": (i,j) in voxelbmlt_"+o1.getName()+"_"+angulos_sel[cont]+"} "
                            + "x"+angulos_sel[cont]+"[j]*intensities_"+o1.getName()+"_"+angulos_sel[cont]+"[i,j])"+" \n";
                 if(cont !=  angulos_sel.length-1){
                        st1 = st1 + "+";
                        st2 = st2 + "+";
                    }
            }
                   
            
            if (o1.isIsTarget()){
                
                // BEAMS VECTOR
                    writeLine("maxNumBeams 				: 	sum {k in 1 .. numBeams} beams[k] 								= 	n; \n", bwParametersFile);
                    for(int cont=0; cont<angulos_sel.length; cont++){
                       writeLine("beamAvailable_x"+angulos_sel[cont]+"{j in 1 .. "+bmlts[cont] +"}: "
                               +"x"+angulos_sel[cont]+"[j] <= 	30*beams["+(cont+1)+"] ;\n",bwParametersFile);
                    }
                    for(int i = 0; i < fixed.length; i++){
                        writeLine("beam"+i+": beams[fix_b"+i+"] = 1;\n", bwParametersFile);
                    }    
                    writeLine("equalityTarget: 	( "+ "((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"}"+"( \n"+st1+")^a["+(orgindex +1)+"])))^(1/a["+(orgindex +1)+"]) >= t; \n", bwParametersFile);
                    writeLine("constraintOAR_Target: 	("+"((1/R_"+o1.getName()+")*(sum {i in voxelIndex_"+o1.getName()+"} ("+" \n "+st2+")^a["+(tp.getOrganos().size() +1)+"]))"+")^(1/a["+(tp.getOrganos().size() +1)+"]) <=OAR_targetUB;\n", bwParametersFile);
                }
            orgindex++;
        }
        
        bwParametersFile.close();
        String bacmsg = "";
        for (int anguloAux :  angulos_sel){
            bacmsg += " "+ anguloAux + " -"; 
        }
        System.out.println("PASO 2 : Solving BAC "+ bacmsg);
        //Running Process
        scriptFile = this.jobThreadID + "scriptLogFunction.sh";
        String dir = jobThreadID + "currentSol.txt";
        File direc= new File(dir);
        
        try{
            int exitAMPL=-1;
            Process p=null;
            int wait=-1;
            /*
            do{
                p= new ProcessBuilder("ampl", scriptFile).start();
               
                while(!p.waitFor(10, TimeUnit.MINUTES)){

                }
                exitAMPL= p.exitValue();
            }while(exitAMPL!=0);
            */
            System.out.print("Solving Log Function (Wu et al., 2002): ");
            //System.out.print("Angles: ");
            for(int i=0; i< this.beams; i++){
                System.out.print(" " +this.angles[i]+ " -- ");
            }
            System.out.print(" // ");
            for(int i=0; i< this.EUD0Par.length; i++){
                System.out.print(this.EUD0Par[i]+ " - ");
            }
            System.out.println();
            tp.printSolTime(timerFile,0,prevTimer,"solverMIPLogFunction: AMPL Files created",false);
            prevTimer = System.currentTimeMillis(); 
            ampl = prepareampl(tp,ampl,fixed, angulos_sel,timerFile);
            System.out.println();
            
            
            /*System.out.print("\n"+p.exitValue()+"\n");
            InputStream is = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            System.out.print("Solving Log Function (Wu et al., 2002): ");
            //System.out.print("Angles: ");
            for(int i=0; i< this.beams; i++){
                System.out.print(" " +this.angles[i]+ " -- ");
            }
            System.out.print(" // ");
            for(int i=0; i< this.EUD0Par.length; i++){
                System.out.print(this.EUD0Par[i]+ " - ");
            }
            System.out.println();

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
           */
        }catch(Exception e){
            System.out.println("\033[0;31m \n"+e.toString()+"\n");
        }
        //Process p = new ProcessBuilder("ampl", scriptFile).start();
        /*try {
            p.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(AMPL_Solver.class.getName()).log(Level.SEVERE, null, ex);
        }
        File f = new File(dir);
        while(!f.exists()){
            p = new ProcessBuilder("ampl", scriptFile).start();
            try {
                p.waitFor();
            } catch (InterruptedException ex) {
                //Logger.getLogger(AMPL_Solver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }*/
        return ampl;
    }
    
    public AMPL prepareampl(TreatmentPlan tp, AMPL ampl, int[] fixed , int[] angulos_sel, String timerFile){
        long prevTimer = System.currentTimeMillis(); 
        // Create an AMPL instance
        boolean new_ampl = false;
        if(ampl == null){
            ampl = new AMPL();
            new_ampl = true;
        }
        HashMap<Integer,Integer> map_angles_index = new HashMap<>();
        HashMap<Integer,Integer> map_index_angles = new HashMap<>();
        for(int k = 0; k < angulos_sel.length; k++){
            map_angles_index.put(angulos_sel[k], k+1);
            map_index_angles.put(k+1, angulos_sel[k]);
        }
        try {
             if(!new_ampl){
                // Create second dataframe (for data indexed over FOOD)
                // Add column by column
                tp.printSolTime(timerFile,0,prevTimer,"prepareampl: AMPL Files loading, Changing fixed angles",false);
                prevTimer = System.currentTimeMillis(); 
                ArrayList<Parameter> fix_beams = new ArrayList<>();
                for(int i = 0; i < fixed.length; i++){
                    fix_beams.add(ampl.getParameter("fix_b"+i));
                    fix_beams.getLast().setValues(map_angles_index.get(fixed[i]));
                }
                //ampl.setBoolOption("presolve", false);
                //ampl.setBoolOption("omit_zero_rows", true);
                tp.printSolTime(timerFile,0,prevTimer,"prepareampl: AMPL Files OK, rMIP fixed solving;",false);
                prevTimer = System.currentTimeMillis(); 
                ampl.eval("solve Total_Cost;");
                tp.printSolTime(timerFile,0,prevTimer,"prepareampl: rMIP fixed solved",false);
                prevTimer = System.currentTimeMillis();
                // Assign data to NUTR, n_min and n_max
            }else{
                tp.printSolTime(timerFile,0,prevTimer,"prepareampl: Starting AMPL Files loading",false);
                prevTimer = System.currentTimeMillis(); 
                ampl.read(this.jobThreadID +"log_rMipModel.mod");
                for (Organs o1 : tp.getOrganos().values()) {
                    for(int i = 0; i < angulos_sel.length; i++){
                        Beam auxBeam = o1.getBeams().get(angulos_sel[i]);
                        ampl.readData("zDDM_" + o1.getName()+"_"+ auxBeam.getIndex() + ".dat");
                    }
                    ampl.readData(jobThreadID+"_MapVoxel_" + o1.getName()+".dat");
                }
                ampl.readData(this.jobThreadID +"extraLogFunction.dat");
                if(tp.warm_start){
                    boolean isPrev = false;
                    for(int i=0; i<tp.prevAngles.length;i++){
                        if(tp.prevAngles[i]!=0){
                            isPrev = true;
                            break;
                        }
                    }
                    if(isPrev){
                        ArrayList<Integer> list = new ArrayList<>();
                        for(int a:angulos_sel){
                            list.add(a);
                        }
                        for(int i=0; i<tp.prevAngles.length;i++){
                            if(list.contains(tp.prevAngles[i])){
                                ampl.readData("x"+tp.prevAngles[i]+".dat");
                            }
                        }
                    }
                }
                
                //ampl.setBoolOption("presolve", false);
                //ampl.setBoolOption("omit_zero_rows", true);
                tp.printSolTime(timerFile,0,prevTimer,"prepareampl: AMPL Files OK, rMIP fixed solving;",false);
                prevTimer = System.currentTimeMillis(); 
                ampl.solve();
                tp.printSolTime(timerFile,0,prevTimer,"prepareampl: rMIP fixed solved",false);
                prevTimer = System.currentTimeMillis(); 
             }
            //ampl.solve();

            // Get objective entity by AMPL name
            //Objective totalcost = ampl.getObjective("Total_Cost");
            // Print it
            //System.out.format("Objective is: %f%n", totalcost.value());
            
           
            
            
            
            DataFrame beams_a = ampl.getVariable("beams").getValues();
            tp.beams_vector = new double[angulos_sel.length][2];
            int cont = 0;
            for (Double n : beams_a.getColumnAsDoubles("beams.val")) {
                tp.beams_vector[cont][0] = cont;
                tp.beams_vector[cont][1] = n;
                cont++;
            }
            Arrays.sort(tp.beams_vector, (a, b) -> Double.compare(b[1],a[1]));
            //System.out.println(tp.beams_vector.toString());
            /*for(int i=0; i<angles.length;i++){
                ampl.display("x"+ angles[i] +" > "+ this.jobThreadID +"currentSol_"+angles[i]+".txt");
            }*/
            
        } catch (IOException e) {
            //ampl.close();
            
            System.out.println(e.getMessage());
        } finally {
            //ampl.close();
        }
        return ampl;
    }
    

    /**
     * @return the organs
     */
    public int getOrgans() {
        return organs;
    }

    /**
     * @param organs the organs to set
     */
    public void setOrgans(int organs) {
        this.organs = organs;
    }

    /**
     * @return the beams
     */
    public int getBeams() {
        return beams;
    }

    /**
     * @param beams the beams to set
     */
    public void setBeams(int beams) {
        this.beams = beams;
    }

    /**
     * @return the bmlts
     */
    public int[] getBmlts() {
        return bmlts;
    }

    /**
     * @param bmlts the bmlts to set
     */
    public void setBmlts(int[] bmlts) {
        this.bmlts = bmlts;
    }

    /**
     * @return the totalBmlts
     */
    public int getTotalBmlts() {
        return totalBmlts;
    }

    /**
     * @param totalBmlts the totalBmlts to set
     */
    public void setTotalBmlts(int totalBmlts) {
        this.totalBmlts = totalBmlts;
    }

    /**
     * @return the angles
     */
    public int[] getAngles() {
        return angles;
    }

    /**
     * @param angles the angles to set
     */
    public void setAngles(int[] angles) {
        this.angles = angles;
    }

 
    /**
     * @return the aPar
     */
    public int[] getaPar() {
        return aPar;
    }

    /**
     * @param aPar the aPar to set
     */
    public void setaPar(int[] aPar) {
        this.aPar = aPar;
    }

    /**
     * @return the vPar
     */
    public int[] getvPar() {
        return vPar;
    }

    /**
     * @param vPar the vPar to set
     */
    public void setvPar(int[] vPar) {
        this.vPar = vPar;
    }

    /**
     * @return the wPar
     */
    public double[] getwPar() {
        return wPar;
    }

    /**
     * @param wPar the wPar to set
     */
    public void setwPar(double[] wPar) {
        this.wPar = wPar;
    }

    /**
     * @return the EUD0Par
     */
    public double[] getEUD0Par() {
        return EUD0Par;
    }

    /**
     * @param EUD0Par the EUD0Par to set
     */
    public void setEUD0Par(double[] EUD0Par) {
        this.EUD0Par = EUD0Par;
    }

    /**
     * @return the LB
     */
    public double[] getLB() {
        return LB;
    }

    /**
     * @param LB the LB to set
     */
    public void setLB(double[] LB) {
        this.LB = LB;
    }

    /**
     * @return the UB
     */
    public double[] getUB() {
        return UB;
    }

    /**
     * @param UB the UB to set
     */
    public void setUB(double[] UB) {
        this.UB = UB;
    }

    /**
     * @return the isTarget
     */
    public boolean[] getIsTarget() {
        return isTarget;
    }

    /**
     * @param isTarget the isTarget to set
     */
    public void setIsTarget(boolean[] isTarget) {
        this.isTarget = isTarget;
    }

    /**
     * @return the epsilon
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * @param epsilon the epsilon to set
     */
    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    /**
     * @return the x
     */
    public double[] getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(double[] x) {
        this.x = x;
    }

    /**
     * @return the jobThreadID
     */
    public String getJobThreadID() {
        return jobThreadID;
    }

    /**
     * @param jobThreadID the jobThreadID to set
     */
    public void setJobThreadID(String jobThreadID) {
        this.jobThreadID = jobThreadID;
    }

    /**
     * @return the solver
     */
    public String getSolver() {
        return solver;
    }

    /**
     * @param solver the solver to set
     */
    public void setSolver(String solver) {
        this.solver = solver;
    }

    /**
     * @return the maxIntensity
     */
    public double getMaxIntensity() {
        return maxIntensity;
    }

    /**
     * @param maxIntensity the maxIntensity to set
     */
    public void setMaxIntensity(double maxIntensity) {
        this.maxIntensity = maxIntensity;
    }
    
    public void new_params(int[] params){
        for(int i=0;i<3;i++)
            this.aPar[i] = params[i+1];
        this.UB[0] = params[0];
    }
    
}
