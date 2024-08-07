package imrt2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.FileNameMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author gcab623
 */
public class Organs {
    private String name;
    private double weight; //In case weighted sum method is considered 0<weight<1
    private double voxelEnd;
    private double totalDose;
    private double actualMinDose;
    private double actualMaxDose;
    private double doseUB;
    private double doseLB;
    private double desiredDose; //Maximum dose for OARs and Minimum Dose for Target
    private int a;
    private int v;
    private boolean isTarget;//PTV
    private HashMap<Integer,Beam> beams; 
    private HashMap<Double, Integer> mapVoxel;
    

    public  Organs(String n, double vI, double vE, double TD, double mD, double MD, double UB, double LB, double dD, int aPar, int vPar, int tV, boolean isTar){
        this.name=n;  this.weight = vI; this.voxelEnd=vE;
        this.totalDose=TD; this.actualMinDose=mD; this.actualMaxDose=MD;
        this.doseUB = UB; this.doseLB=LB; this.desiredDose=dD; this.a=aPar; this.v = vPar;
        this.isTarget = isTar;  
        this.beams = new HashMap<>();
        mapVoxel = new HashMap<>();
        
    }
    public Organs(Organs o){
        name = o.getName();
        weight = o.getWeight();
        voxelEnd = o.getVoxelEnd();
        totalDose = o.getTotalDose();
        actualMinDose = o.getActualMinDose();
        actualMaxDose = o.getActualMaxDose();
        doseUB = o.getDoseUB();
        doseLB = o.getDoseLB();
        desiredDose = o.getDesiredDose();
        a = o.getA();
        v = o.getV();
        isTarget = o.isIsTarget();
        beams = new HashMap<>();
        for(int angle:o.getBeams().keySet()){
            beams.put(angle, new Beam(o.getBeams().get(angle)));
        }
        
        mapVoxel = new HashMap<>();
        if(o.getMapVoxel() != null){
            for(Double indice:o.getMapVoxel().keySet()){
                mapVoxel.put(indice, o.getMapVoxel().get(indice));
            }
        }
        
    }
    
    
    
      public void iniorgansbeams(int [][] beamletmap, int [] selAngles, String pathfile) throws IOException{
        for(int i=0; i<selAngles.length; i++){
            if(!beams.containsKey(selAngles[i])){
                beams.put(selAngles[i], new Beam(beamletmap[selAngles[i]][1], selAngles[i],pathfile));
                Beam aux = beams.get(selAngles[i]);
                aux.readCoordinates(pathfile);
                aux.readIntensities(pathfile,name);
                beams.put(selAngles[i],aux);
            }            
        }   
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * @return the voxelEnd
     */
    public double getVoxelEnd() {
        return voxelEnd;
    }

    /**
     * @param voxelEnd the voxelEnd to set
     */
    public void setVoxelEnd(double voxelEnd) {
        this.voxelEnd = voxelEnd;
    }

    /**
     * @return the totalDose
     */
    public double getTotalDose() {
        return totalDose;
    }

    /**
     * @param totalDose the totalDose to set
     */
    public void setTotalDose(double totalDose) {
        this.totalDose = totalDose;
    }

    /**
     * @return the actualMinDose
     */
    public double getActualMinDose() {
        return actualMinDose;
    }

    /**
     * @param actualMinDose the actualMinDose to set
     */
    public void setActualMinDose(double actualMinDose) {
        this.actualMinDose = actualMinDose;
    }

    /**
     * @return the actualMaxDose
     */
    public double getActualMaxDose() {
        return actualMaxDose;
    }

    /**
     * @param actualMaxDose the actualMaxDose to set
     */
    public void setActualMaxDose(double actualMaxDose) {
        this.actualMaxDose = actualMaxDose;
    }

    /**
     * @return the doseUB
     */
    public double getDoseUB() {
        return doseUB;
    }

    /**
     * @param doseUB the doseUB to set
     */
    public void setDoseUB(double doseUB) {
        this.doseUB = doseUB;
    }

    /**
     * @return the doseLB
     */
    public double getDoseLB() {
        return doseLB;
    }

    /**
     * @param doseLB the doseLB to set
     */
    public void setDoseLB(double doseLB) {
        this.doseLB = doseLB;
    }

    /**
     * @return the desiredDose
     */
    public double getDesiredDose() {
        return desiredDose;
    }

    /**
     * @param desiredDose the desiredDose to set
     */
    public void setDesiredDose(double desiredDose) {
        this.desiredDose = desiredDose;
    }

    /**
     * @return the a
     */
    public int getA() {
        return a;
    }

    /**
     * @param a the a to set
     */
    public void setA(int a) {
        this.a = a;
    }

    /**
     * @return the v
     */
    public int getV() {
        return v;
    }

    /**
     * @param v the v to set
     */
    public void setV(int v) {
        this.v = v;
    }

 
    /**
     * @return the isTarget
     */
    public boolean isIsTarget() {
        return isTarget;
    }

    /**
     * @param isTarget the isTarget to set
     */
    public void setIsTarget(boolean isTarget) {
        this.isTarget = isTarget;
    }

    /**
     * @return the beams
     */
    public HashMap<Integer,Beam> getBeams() {
        return beams;
    }

    /**
     * @param beams the beams to set
     */
    public void setBeams(HashMap<Integer,Beam> beams) {
        this.beams = beams;
    }
    
        /**
     * @return the mapVoxel
     */
    public HashMap<Double, Integer> getMapVoxel() {
        return mapVoxel;
    }

    /**
     * @param mapVoxel the mapVoxel to set
     */
    public void setMapVoxel(HashMap<Double, Integer> mapVoxel) {
        this.mapVoxel = mapVoxel;
    }

    public int createDDM(int beamindex, String folderDB, int bmlts) {
        if(beams.containsKey(beamindex)){
            if(beams.get(beamindex).isGeneratedDDM()){
                return 0;
            }else{
                try {
                    beams.get(beamindex).createDDM(isTarget,  folderDB, name);
                } catch (IOException ex) {
                    return -1;
                }
               
                return 1;
            }
        }else{
            try {
                beams.put(beamindex, new Beam(bmlts,beamindex));
                //beams.put(beamindex, new Beam(beamindex));
                beams.get(beamindex).createDDM(isTarget, folderDB,name);
            } catch (IOException ex) {
                return -1;
            }
            return 1;                     
        }
    }
    
    public void generateMapVoxel(){
        for(int beam_ind:beams.keySet()){
            Beam beam = beams.get(beam_ind);
            ArrayList<double[]> intensities = beam.getIntensity();
            for(double[] int_vox: intensities){
                if(!mapVoxel.containsKey(int_vox[0])){
                    mapVoxel.put(int_vox[0], mapVoxel.size());
                }
            }
        }
    }
    
    public void writeMapVoxel(String processName, int [] selangles) throws IOException{
        generateMapVoxel();
        String fileName = "";
        BufferedWriter bwDDM = null ;
        fileName =  "./"+processName+"_MapVoxel_" + name + ".dat";

        File fileAMPL = new File(fileName);
        if (!fileAMPL.exists()) {
           bwDDM = new BufferedWriter(new FileWriter(fileName));
        }else{
           fileAMPL.delete();
           bwDDM = new BufferedWriter(new FileWriter(fileName));
        }

       writeLine("set voxelIndex_"+name+" := \n", bwDDM);
       for(double keymap : mapVoxel.keySet()){
           writeLine((int)keymap + "\t", bwDDM);
       }
       writeLine(";\n", bwDDM);
       writeLine("param R_"+name+" := " + mapVoxel.size() + ";\n", bwDDM);
       for(int i = 0; i<selangles.length; i++){
            writeLine("set bmltsIndex_"+name+"_"+selangles[i]+" := \n", bwDDM);
            for(int j=1; j<=beams.get(selangles[i]).getBeamlets(); j++){
                writeLine((int)j + "\t", bwDDM);
            }
            writeLine(";\n", bwDDM);
       }
       
       writeLine(";\n", bwDDM);
       bwDDM.close();
    }
    
    public void liberateMemory(){
        //mapVoxel = null;
        for(int index:beams.keySet()){
            beams.get(index).liberateMemory();
        }
    }

    public static void writeLine(String l, BufferedWriter bw) throws IOException{
	
	String row = "";
	row = l ; 
	bw.write(row );
	

    }
    
    public void newBeam(int index,Beam angulo){
        beams.put(index, angulo);
    }
}

