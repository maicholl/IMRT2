package imrt2;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.*;
import java.util.*;
        
/**
 *
 * @author Maicholl
 */

public class DDM {
    public HashMap<Integer, Double> voxelIndex;
    
    public int  totalVoxels;
    public int  totalBeamlets;
    public int entries;
    
    public DDM(DDM ddm){
        voxelIndex = new HashMap<>();
         for(Integer inten:ddm.voxelIndex.keySet()){
             voxelIndex.put(inten, ddm.voxelIndex.get(inten));
        } 
        totalVoxels = ddm.totalVoxels;
        totalBeamlets = ddm.totalBeamlets;
    }
    
    public DDM(int bl) {
        this.totalBeamlets=bl;
        voxelIndex = new HashMap<>();
    }
    public DDM() {
        voxelIndex = new HashMap<>();
        
    }
    
    
    
    public void writeDDM(int numBlts, ArrayList<double[]> intensities, String nombre, boolean isTarget, String folderDB, int beam) throws IOException{
     //number of Voxels of organ 'y'
     //if (voxelIndex[org.index] == null)
        
     voxelIndex = new HashMap<>();
     
     entries = intensities.size();
     //o[org.index].totalVoxels=voxelIndex[org.index].size();
     //int numVoxels = org.totalVoxels;
     String fileName = "";
     BufferedWriter bwDDM = null ;
     fileName =  "zDDM_" + nombre +"_"+ beam + ".dat";
     String vIndexName = "./"+folderDB+"voxelIndex" + nombre + ".txt";
     
     
     File fileAMPL = new File(fileName);
     if (!fileAMPL.exists()) {
        bwDDM = new BufferedWriter(new FileWriter(fileName));
     }else{
        fileAMPL.delete();
        bwDDM = new BufferedWriter(new FileWriter(fileName));
     }

     
    writeLine("param: voxelbmlt_"+nombre+"_"+beam+": intensities_"+nombre+"_"+beam+" := \n", bwDDM);
    for(int i=0; i< intensities.size();i++){
        writeLine((int)intensities.get(i)[0] + " " + (int)intensities.get(i)[1] + " " + intensities.get(i)[2] + "\n", bwDDM);
    }
    writeLine(";\n", bwDDM);
    //writeLine("param intensityentries_"+nombre+"_"+beam+" := " + entries + ";\n", bwDDM);
    bwDDM.close();
     
  
    //System.out.println("Se ha generado el DDM: "+fileName);
 }
    
 
        public int read(int numBlts, ArrayList<double[]> intensities, String nombre, boolean isTarget, String processName,int beam) throws IOException{
            //number of Voxels of organ 'y'
            ArrayList<double[]> DDM = new ArrayList<>();
            //if (voxelIndex[org.index] == null)

            voxelIndex = new HashMap<>();
            int vIndex, col;
            double voxelNum, intensity;
               for (int i=0;i<intensities.size();i++){
                  voxelNum = intensities.get(i)[0];
                  col = (int) intensities.get(i)[1]-1 ; //beamlet Index; //beams[j].intensity[org.index]
                  intensity =intensities.get(i)[2];
                  //vIndex=voxelIndex[org.index].indexOf(voxelNum);
                  vIndex = (int)voxelNum;
                  if (!voxelIndex.containsKey(voxelNum)){ //voxel is not in the voxelIndex (new voxel)
                      vIndex = voxelIndex.size();
                      voxelIndex.put(vIndex, voxelNum);
                      double[] auxDDM = new double[numBlts+1];
                      DDM.add(auxDDM);
                  }
                  if (DDM.get(vIndex)[col]==0){
                     DDM.get(vIndex)[col] = intensity;
                     DDM.get(vIndex)[numBlts] += intensity; 
                  }else{
                     System.err.println("Error 4. Se sobre-escribio DDM, beamlet" + col + ", voxel " + vIndex);
                  }

            }
            //o[org.index].totalVoxels=voxelIndex[org.index].size();
            //int numVoxels = org.totalVoxels;
            int numVoxels = voxelIndex.size();
            String fileName = "";
            BufferedWriter bwDDM = null ;
            fileName =  "DDM_" + nombre +"_"+ beam + ".dat";
            BufferedWriter bwVoxelIndex = null ;
            String vIndexName = "./"+processName+"voxelIndex" + nombre + ".txt";


            File fileAMPL = new File(fileName);
            if (!fileAMPL.exists()) {
               bwDDM = new BufferedWriter(new FileWriter(fileName));
            }else{
               fileAMPL.delete();
               bwDDM = new BufferedWriter(new FileWriter(fileName));
            }

            File fileVoxelIndex = new File(vIndexName);
            if (!fileVoxelIndex.exists()) {
               bwVoxelIndex = new BufferedWriter(new FileWriter(vIndexName));
            }else{
               fileVoxelIndex.delete();
               bwVoxelIndex = new BufferedWriter(new FileWriter(vIndexName));
            }

            writeLine("param ddm" + nombre + beam +":\t", bwDDM);
            for (int auxIndex=1;auxIndex<numBlts;auxIndex++){
               writeLine(auxIndex + "\t", bwDDM);
            }
            writeLine(numBlts + " :=\n", bwDDM);

            ArrayList<Long> deletedVoxels = new ArrayList <>();
            int deletedCounter = 0;
            int row = 1;
            /*for(int a : voxelIndex.keySet()){
                System.out.println(a);
                System.out.println("====");
            }*/
            for (int v=0;v<numVoxels;v++){
                if (DDM.get(v)[numBlts] > 0 || isTarget){
                    writeLine(row + "\t" + v + "\t" + voxelIndex.get(v) + "\n", bwVoxelIndex);
                    writeLine(row + "\t", bwDDM);
                    for (int j=0;j<numBlts;j++){
                       writeLine(DDM.get(v)[j] + "\t", bwDDM); 
                    }
                    writeLine("\n", bwDDM);
                    row++;
                }else{
                    deletedVoxels.add((long)v);
                    deletedCounter++;
                }
           }
           writeLine(";\n", bwDDM);
           bwDDM.close();
           bwVoxelIndex.close();
           System.out.println(deletedCounter + " voxels have been removed from " + nombre);
           for (int v=0;v<deletedVoxels.size();v++){
               for (int i=0;i<voxelIndex.size();i++){
                   if (voxelIndex.get(i).equals(deletedVoxels.get(v))){
                       voxelIndex.remove(i);
                       break;
                   }
               }
           }
           //o[org.index].totalVoxels=voxelIndex[org.index].size();
           //System.out.println("Voxels : " + voxelIndex[org.index].size() + " -- Removed: " + deletedVoxels.size());    
           totalVoxels = voxelIndex.size();
          return totalVoxels;  
        }
    
 
    public static void writeLine(String l, BufferedWriter bw) throws IOException{
	
	String row = "";
	row = l ; 
	bw.write(row );
	

    }
    
    
    
   
    
}
