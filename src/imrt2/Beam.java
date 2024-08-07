/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imrt2;

/**
 *
 * @author Maicholl
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gcab623
 */
public class Beam {
    private int index;
    private int beamlets;
    private double[][] beamletsCoord;
    private ArrayList<double[]> intensity; 
    private DDM ddm;
    private boolean generatedDDM;
    private boolean resolvedBeam;
    private double[] x;
    
    public Beam(Beam rayo){
        index = rayo.getIndex();
        beamlets = rayo.getBeamlets();
        if(rayo.getBeamletsCoord() != null)
            if(rayo.getBeamletsCoord().length> 0){
                beamletsCoord = new double[rayo.getBeamletsCoord().length][rayo.getBeamletsCoord()[0].length];
                for(int i=0;i<rayo.getBeamletsCoord().length;i++){
                    System.arraycopy(rayo.getBeamletsCoord()[i], 0, beamletsCoord[i], 0, rayo.getBeamletsCoord()[0].length);
                }
            }
            
        intensity = new ArrayList<>();
        if(rayo.getIntensity() != null){
            for(double[] inten:rayo.getIntensity()){
               double[] aux = new double[inten.length];
               System.arraycopy(inten, 0, aux, 0, inten.length);
               intensity.add(aux);
           } 
        }
       
        if(rayo.getDdm() != null)
            ddm = new DDM(rayo.getDdm());
        generatedDDM = rayo.isGeneratedDDM();
        resolvedBeam = rayo.isResolvedBeam();
        if(rayo.getX() != null){
            x = new double[rayo.getX().length];
            
            System.arraycopy(rayo.getX(), 0, x, 0, rayo.getX().length);
        }
    }
    
    public Beam(int bl, int ang, String path) throws IOException{
        this.beamlets=bl;
        this.index=ang;
        this.beamletsCoord = new double[bl][5];
        readCoordinates(path);
        generatedDDM = false;
        resolvedBeam = false;
        ddm = new DDM(beamlets);
    }
    
    public Beam(int bl, int ang) throws IOException{
        this.beamlets=bl;
        this.index=ang;
        generatedDDM = false;
        resolvedBeam = false;
    }
    
    public Beam(int ang){
        this.beamlets=0;
        this.index=ang;
        generatedDDM = false;
        resolvedBeam = false;
    }
    
    public Beam(int bl, int ang, boolean gene, String path, String nameOrgan) throws IOException{
        this.beamlets=bl;
        this.index=ang;
        generatedDDM = gene;
        resolvedBeam = false;
        readIntensities(path, nameOrgan);
    }
    
    

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
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
     * @return the beamletsCoord
     */
    public double[][] getBeamletsCoord() {
        return beamletsCoord;
    }

    /**
     * @param beamletsCoord the beamletsCoord to set
     */
    public void setBeamletsCoord(double[][] beamletsCoord) {
        this.beamletsCoord = beamletsCoord;
    }

    /**
     * @return the intensity
     */
    public ArrayList<double[]> getIntensity() {
        return intensity;
    }

    /**
     * @param intensity the intensity to set
     */
    public void setIntensity(ArrayList<double[]> intensity) {
        this.intensity = intensity;
    }

    /**
     * @return the ddm
     */
    public DDM getDdm() {
        return ddm;
    }

    /**
     * @param ddm the ddm to set
     */
    public void setDdm(DDM ddm) {
        this.ddm = ddm;
    }

    /**
     * @return the generatedDDM
     */
    public boolean isGeneratedDDM() {
        return generatedDDM;
    }

    /**
     * @param generatedDDM the generatedDDM to set
     */
    public void setGeneratedDDM(boolean generatedDDM) {
        this.generatedDDM = generatedDDM;
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
     * @return the resolvedBeam
     */
    public boolean isResolvedBeam() {
        return resolvedBeam;
    }

    /**
     * @param resolvedBeam the resolvedBeam to set
     */
    public void setResolvedBeam(boolean resolvedBeam) {
        this.resolvedBeam = resolvedBeam;
    }
    
    
    public void readCoordinates(String s) throws FileNotFoundException, IOException{
        
        beamletsCoord= new double[beamlets][5];
        
        String[] auxReader = new String[3];
        String sp="\t";
        String dir = s + "beamletCoord" +"_"+index+".txt";
        File f = new File(dir);
        BufferedReader fileIn = new BufferedReader(new FileReader(f));
        String line = "";
        line=fileIn.readLine();
        int i=0;
        do{
            auxReader = line.split(sp);
            beamletsCoord[i][0] = i; //beamletIndex
            beamletsCoord[i][1] = Double.parseDouble(auxReader[6]); //Relative xCoord
            beamletsCoord[i][2] = Double.parseDouble(auxReader[7]); //Relative yCoord
            beamletsCoord[i][3] = Double.parseDouble(auxReader[8]); //xCoord
            beamletsCoord[i][4] = Double.parseDouble(auxReader[9]); //yCoord
            i++;
            line=fileIn.readLine();
        }while(line != null);  
    }
    public void readIntensities(String s, String nombre) throws FileNotFoundException, IOException{
            
        this.intensity = new ArrayList<>();
        String[] auxReader = new String[3];
        String sp="\t";
        String dir = s + nombre +"_"+this.index+".txt";
        File f = new File(dir);
        BufferedReader fileIn = new BufferedReader(new FileReader(f));
        String line = "";
        line=fileIn.readLine();
        while(line != null){
            double[] auxIntensity = new double[3];
            auxReader = line.split(sp);
            auxIntensity[0] = Double.parseDouble(auxReader[0]); //voxelIndex
            auxIntensity[1] = Double.parseDouble(auxReader[1]); //beamletIndex
            auxIntensity[2] = Double.parseDouble(auxReader[2]); //intensity
            this.intensity.add(auxIntensity); 
            line=fileIn.readLine();
        }
        fileIn.close();
    }
    
    public void readIntensities(String[] s, int indice, String nombre, ArrayList<Long> voxelIndex) throws FileNotFoundException, IOException{
        
        
        String[] auxReader = new String[3]; //VoxelIndex - BeamletIndex - Intensity
        String sp="\t";
        String dir = new String();
        dir = s[1] + nombre +"_"+this.index+".txt";
        File f = new File(dir);
        BufferedReader fileIn = new BufferedReader(new FileReader(f));
        this.intensity = new ArrayList<>();
        dir = s[1] + nombre +"_"+this.index+".txt";
        String line = "";
        line=fileIn.readLine();
        //this.intensity[y] = new ArrayList<>();
        while(line != null){
            double[] auxIntensity = new double[3];
            auxReader = line.split(sp);
            auxIntensity[0] = Double.parseDouble(auxReader[0]); //voxelIndex
            auxIntensity[1] = Double.parseDouble(auxReader[1]); //beamletIndex
            auxIntensity[2] = Double.parseDouble(auxReader[2]); //intensity
            this.intensity.add(auxIntensity);
            //Generate Voxel Index of organ 'y' using beam angle 'j'
            /*if (!voxelIndex.contains((long)auxIntensity[0])){
                    voxelIndex.add((long)auxIntensity[0]);
            }*/
            line=fileIn.readLine();
        }
        Collections.sort(voxelIndex);
        fileIn.close();
    }
    
    
    public void createDDM(boolean isTarget, String  name, String folderDB) throws IOException {
        readIntensities(name, folderDB);
        ddm = new DDM(beamlets);
        ddm.writeDDM(beamlets, intensity, folderDB, isTarget, name,index);
        generatedDDM = true;
    }

        public void liberateMemory(){
        intensity = null;
        beamletsCoord = null;
        ddm = null;
    }

   

    
    
    
}
