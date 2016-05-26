package swarmBots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.Coord;
import common.MapTile;
import common.ScanMap;
import enums.Terrain;

/**
 * The seed that this program is built on is a chat program example found here:
 * http://cs.lmu.edu/~ray/notes/javanetexamples/ Many thanks to the authors for
 * publishing their code examples
 */

public class ROVER_14 {
	
	public static final int DIAGONAL_COST = 14;
    public static final int V_H_COST = 10;
	
    static class Cell{  
        int heuristicCost = 0; //Heuristic cost
        int finalCost = 0; //G+H
        int i, j;
        Cell parent; 
        
        Cell(int i, int j){
            this.i = i;
            this.j = j; 
        }
        
        @Override
        public String toString(){
            return "["+this.i+", "+this.j+"]";
        }
    }
    
    
  //Blocked cells are just null Cell values in grid
    static Cell [][] grid = new Cell[60][100];
    
    static PriorityQueue<Cell> open;
     
    static boolean closed[][];
    static int startI, startJ;
    static int endI, endJ;
    
    
    
            
    public static void setBlocked(int i, int j){
        grid[i][j] = null;
    }
    
    public static void setStartCell(int i, int j){
        startI = i;
        startJ = j;
    }
    
    public static void setEndCell(int i, int j){
        endI = i;
        endJ = j; 
    }
    
    static void checkAndUpdateCost(Cell current, Cell t, int cost){
        if(t == null || closed[t.i][t.j])return;
        int t_final_cost = t.heuristicCost+cost;
        
        boolean inOpen = open.contains(t);
        if(!inOpen || t_final_cost<t.finalCost){
            t.finalCost = t_final_cost;
            t.parent = current;
            if(!inOpen)open.add(t);
        }
    }
    

	BufferedReader in;
	PrintWriter out;
	String rovername;
	ScanMap scanMap;
	int sleepTime;
	String SERVER_ADDRESS = "localhost";
	static final int PORT_ADDRESS = 9537;
	
	// Arraylist for chemical locations
			ArrayList<String> chemicalsFetch = new ArrayList<String>();
			ArrayList<String> chemicalLocations = new ArrayList<String>();

	public ROVER_14() {
		// constructor
		System.out.println("ROVER_14 rover object constructed");
		rovername = "ROVER_14";
		SERVER_ADDRESS = "localhost";
		// this should be a safe but slow timer value
		sleepTime = 300; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
	}
	
	public ROVER_14(String serverAddress) {
		// constructor
		System.out.println("ROVER_14 rover object constructed");
		rovername = "ROVER_14";
		SERVER_ADDRESS = serverAddress;
		sleepTime = 200; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
	}

	
	// Rover declarations
	
	boolean goingSouth = false;
	boolean goingEast = false;
	boolean goingWest = false;
	boolean goingNorth = false;
	boolean eastBlocked = false;
	boolean westBlocked = false;
	boolean northBlocked = false;
	boolean southBlocked = false;
	
	
	
	
	/**
	 * Connects to the server then enters the processing loop.
	 */
	private void run() throws IOException, InterruptedException {

		// Make connection and initialize streams
		//TODO - need to close this socket
		Socket socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS); // set port here
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);

		//Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// Process all messages from server, wait until server requests Rover ID
		// name
		while (true) {
			String line = in.readLine();
			if (line.startsWith("SUBMITNAME")) {
				out.println(rovername); // This sets the name of this instance
										// of a swarmBot for identifying the
										// thread to the server
				break;
			}
		}

		// ******** Rover logic *********
		// int cnt=0;
		String line = "";

		int counter = 0;
		int verticalrounds = 0;
		
		// Current and target coordinates in int
		int current_x, current_y;
		int target_x, target_y;
		
		
		boolean forward = false;
		boolean reverse = false;
		
		boolean stuck = false; // just means it did not change locations between requests,
								// could be velocity limit or obstruction etc.
		boolean blocked = false;

		String[] cardinals = new String[4];
		String currentCoord=null;
		cardinals[0] = "N";
		cardinals[1] = "E";
		cardinals[2] = "S";
		cardinals[3] = "W";

		String currentDir = null;
		Coord currentLoc = null;
		Coord previousLoc = null;
		Coord targetLoc = null;
		
		// Initial direction
		goingWest=true;
		forward=true;
		
		
		/// Astar
       
		 //Reset Grid
        grid = new Cell[60][100];
        closed = new boolean[60][100];
        open = new PriorityQueue<>((Object o1, Object o2) -> {
             Cell c1 = (Cell)o1;
             Cell c2 = (Cell)o2;

             return c1.finalCost<c2.finalCost?-1:
                     c1.finalCost>c2.finalCost?1:0;
         });
		
        for(int i=0;i<60;++i){
            for(int j=0;j<100;++j){
                grid[i][j] = new Cell(i, j);
                grid[i][j].heuristicCost = Math.abs(i-endI)+Math.abs(j-endJ);
//                System.out.print(grid[i][j].heuristicCost+" ");
            }
//            System.out.println();
         }
		
		
		
		
		
		
		

		// start Rover controller process
		while (true) {

			// currently the requirements allow sensor calls to be made with no
			// simulated resource cost
			
			
			// **** location call ****
			out.println("LOC");
			line = in.readLine();
            if (line == null) {
            	System.out.println("ROVER_14 check connection to server");
            	line = "";
            }
			if (line.startsWith("LOC")) {
				// loc = line.substring(4);
				currentLoc = extractLOC(line);
			}
			System.out.println("ROVER_14 currentLoc at start: " + currentLoc);
			
			// after getting location set previous equal current to be able to check for stuckness and blocked later
			previousLoc = currentLoc;
			
			// getting current location in a string
			currentCoord=currentLoc.currentCoord();
			
			// getting current location in a integer
			//String [] coordinates = currentCoord.split(" ");
			current_x=currentLoc.xpos;
			current_y=currentLoc.ypos;
			
			// printing current coordinates
			
			System.out.println(currentCoord);
			
			
			// **** Request TARGET_LOC Location from SwarmServer ****
	        out.println("TARGET_LOC");
	        line = in.readLine();
	        if (line == null) {
	            System.out.println(rovername + " check connection to server");
	            line = "";
	        }
	        if (line.startsWith("TARGET_LOC")) {
	            targetLoc = extractLOC(line);
	        }
	        System.out.println(rovername + " TARGET_LOC " + targetLoc);
			
			// getting target location in a integer
			target_x=targetLoc.xpos;
			target_y=targetLoc.ypos;
			
			
			// Create array list for blocked cells
			List<Coord> blockedcells = new ArrayList<Coord>();
			
			
					
			
			
			// **** get equipment listing ****			
			ArrayList<String> equipment = new ArrayList<String>();
			equipment = getEquipment();
			//System.out.println("ROVER_14 equipment list results drive " + equipment.get(0));
			System.out.println("ROVER_14 equipment list results " + equipment + "\n");
			
	

			// ***** do a SCAN *****
			//System.out.println("ROVER_14 sending SCAN request");
			this.doScan();
			scanMap.debugPrintMap();
			
			// Catching the chemical locations arraylist
			chemicalsFetch = scanMap.chemicalLocations();
			
			// Calculating coordinates and adding the chemical locations to an new arraylist using a function
			AddChemicalLocations(currentCoord, chemicalsFetch);
			
			for(String s:chemicalLocations){
				System.out.println(s);
			}
			
						
			// MOVING
				
				// pull the MapTile array out of the ScanMap object
				MapTile[][] scanMapTiles = scanMap.getScanMap();
				int centerIndex = (scanMap.getEdgeSize() - 1)/2;
				// tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
				
				
				
				// printing center index
				System.out.println("****************** Center Index**************");
				System.out.println(centerIndex);
				
				// Check East
				if(		scanMapTiles[centerIndex + 1][centerIndex].getHasRover() 
						|| scanMapTiles[centerIndex +1][centerIndex].getTerrain() == Terrain.ROCK
						|| scanMapTiles[centerIndex +1][centerIndex].getTerrain() == Terrain.SAND
						|| scanMapTiles[centerIndex +1][centerIndex].getTerrain() == Terrain.NONE){
					eastBlocked = true;
				}else {
					eastBlocked=false;
				}
				
				// Printing the East Block Status
				System.out.println("East Block Status : "+eastBlocked);
				
				// Check West
				if(		scanMapTiles[centerIndex-1][centerIndex].getHasRover() 
						|| scanMapTiles[centerIndex -1][centerIndex].getTerrain() == Terrain.ROCK
						|| scanMapTiles[centerIndex -1][centerIndex].getTerrain() == Terrain.SAND
						|| scanMapTiles[centerIndex -1][centerIndex].getTerrain() == Terrain.NONE){
					westBlocked = true;
				}else {
					westBlocked=false;
				}
				
				// Printing the West Block Status
				System.out.println("West Block Status : "+westBlocked);
				
				// Check North
				if(		scanMapTiles[centerIndex][centerIndex -1].getHasRover() 
						|| scanMapTiles[centerIndex][centerIndex -1].getTerrain() == Terrain.ROCK
						|| scanMapTiles[centerIndex][centerIndex -1].getTerrain() == Terrain.SAND
						|| scanMapTiles[centerIndex][centerIndex -1].getTerrain() == Terrain.NONE){
					northBlocked = true;
				}else {
					northBlocked=false;
				}
				
				// Printing the North Block Status
				System.out.println("North Block Status : "+northBlocked);
				
				// Check South
				if(		scanMapTiles[centerIndex][centerIndex +1].getHasRover() 
						|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.ROCK
						|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.SAND
						|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.NONE){
					southBlocked = true;
				}else {
					southBlocked=false;
				}
				
				// Printing the South Block Status
				System.out.println("South Block Status : "+southBlocked);


				
				if (goingEast) {
					// check scanMap to see if path is blocked to the south
					// (scanMap may be old data by now)
					if (eastBlocked) {
						blocked = true;
					} else {
						// request to server to move
						out.println("MOVE E");						
						System.out.println("ROVER_14 request move E");
						
						// Setting other directions false
						goingWest=false;
						goingNorth=false;
						goingSouth=false;
						
						// Setting current direction
						
						currentDir=cardinals[1];
					}
					
				} else if (goingWest) {
					// check scanMap to see if path is blocked to the south
					// (scanMap may be old data by now)
					if (westBlocked) {
						blocked = true;
					} else {
						// request to server to move
						out.println("MOVE W");
						System.out.println("ROVER_14 request move W");
						
						// Setting other directions false
						goingEast=false;
						goingNorth=false;
						goingSouth=false;
						
						// Setting current direction
						
						currentDir=cardinals[3];
					}
					
				} else if (goingNorth) {
					// check scanMap to see if path is blocked to the south
					// (scanMap may be old data by now)
					if (northBlocked) {
						blocked = true;
					} else {
						// request to server to move
						out.println("MOVE N");
						System.out.println("ROVER_14 request move N");
						
						// Setting other directions false
						goingWest=false;
						goingEast=false;
						goingSouth=false;
						
						// Setting current direction
						
						currentDir=cardinals[0];
					}
					
				} else {
					// check scanMap to see if path is blocked to the south
					// (scanMap may be old data by now)
					if (southBlocked) {
						blocked = true;
					} else {
						// request to server to move
						out.println("MOVE S");
						System.out.println("ROVER_14 request move S");
						
						// Setting other directions false
						goingWest=false;
						goingNorth=false;
						goingEast=false;
						
						// Setting current direction
						
						currentDir=cardinals[2];
					}
					
				}


			// another call for current location
			out.println("LOC");
			line = in.readLine();
			if (line.startsWith("LOC")) {
				currentLoc = extractLOC(line);
			}
			
			

			System.out.println("ROVER_14 currentLoc after recheck: " + currentLoc);
			System.out.println("ROVER_14 previousLoc: " + previousLoc);

			// test for stuckness
			stuck = currentLoc.equals(previousLoc);

			System.out.println("ROVER_14 stuck test " + stuck);
			System.out.println("ROVER_14 blocked test " + blocked);

			
			Thread.sleep(sleepTime);
			
			System.out.println("ROVER_14 ------------ bottom process control --------------"); 

		}

	}

	// ################ Support Methods ###########################
	
	
	//  Astar
	
	public static void AStar(){ 
        
        //add the start location to open list.
        open.add(grid[startI][startJ]);
        
        Cell current;
        
        while(true){ 
            current = open.poll();
            
            
            if(current==null)break;
            closed[current.j][current.i]=true; 

            if(current.equals(grid[endI][endJ])){
                return; 
            } 

            Cell t;  
            if(current.i-1>=0){
                t = grid[current.i-1][current.j];
                checkAndUpdateCost(current, t, current.finalCost+V_H_COST); 

                if(current.j-1>=0){                      
                    t = grid[current.i-1][current.j-1];
                    checkAndUpdateCost(current, t, current.finalCost+DIAGONAL_COST); 
                }

                if(current.j+1<grid[0].length){
                    t = grid[current.i-1][current.j+1];
                    checkAndUpdateCost(current, t, current.finalCost+DIAGONAL_COST); 
                }
            } 

            if(current.j-1>=0){
                t = grid[current.i][current.j-1];
                checkAndUpdateCost(current, t, current.finalCost+V_H_COST); 
            }

            if(current.j+1<grid[0].length){
                t = grid[current.i][current.j+1];
                checkAndUpdateCost(current, t, current.finalCost+V_H_COST); 
            }

            if(current.i+1<grid.length){
                t = grid[current.i+1][current.j];
                checkAndUpdateCost(current, t, current.finalCost+V_H_COST); 

                if(current.j-1>=0){
                    t = grid[current.i+1][current.j-1];
                    checkAndUpdateCost(current, t, current.finalCost+DIAGONAL_COST); 
                }
                
                if(current.j+1<grid[0].length){
                   t = grid[current.i+1][current.j+1];
                    checkAndUpdateCost(current, t, current.finalCost+DIAGONAL_COST); 
                }  
            }
        } 
    }
    
    /*
    Params :
    tCase = test case No.
    x, y = Board's dimensions
    si, sj = start location's x and y coordinates
    ei, ej = end location's x and y coordinates
    int[][] blocked = array containing inaccessible cell coordinates
    */
    public static List<Coord> test(int si, int sj, int ei, int ej, ArrayList<Coord> blockedcells){
    	
    	List<Coord> path = new ArrayList<Coord>();
    	
           /*System.out.println("\n\nTest Case #"+tCase);
            //Reset
           grid = new Cell[x][y];
           closed = new boolean[x][y];
           open = new PriorityQueue<>((Object o1, Object o2) -> {
                Cell c1 = (Cell)o1;
                Cell c2 = (Cell)o2;

                return c1.finalCost<c2.finalCost?-1:
                        c1.finalCost>c2.finalCost?1:0;
            });*/
           //Set start position
           setStartCell(si, sj);  //Setting to 0,0 by default. Will be useful for the UI part
           
           //Set End Location
           setEndCell(ei, ej); 
           
/*           for(int i=0;i<x;++i){
              for(int j=0;j<y;++j){
                  grid[i][j] = new Cell(i, j);
                  grid[i][j].heuristicCost = Math.abs(i-endI)+Math.abs(j-endJ);
//                  System.out.print(grid[i][j].heuristicCost+" ");
              }
//              System.out.println();
           }*/
           grid[si][sj].finalCost = 0;
           
           /*
             Set blocked cells. Simply set the cell values to null
             for blocked cells.
           */
           for(Coord temp : blockedcells){
        	   setBlocked(temp.ypos, temp.xpos);
           }
           
          /* for(int i=0;i<blocked.length;++i){
               setBlocked(blocked[i][0], blocked[i][1]);
           }*/
           
          /* //Display initial map
           System.out.println("Grid: ");
            for(int i=0;i<x;++i){
                for(int j=0;j<y;++j){
                   if(i==si&&j==sj)System.out.print("SO  "); //Source
                   else if(i==ei && j==ej)System.out.print("DE  ");  //Destination
                   else if(grid[i][j]!=null)System.out.printf("%-3d ", 0);
                   else System.out.print("BL  "); 
                }
                System.out.println();
            } 
            System.out.println();
           */
           AStar(); 
           
           /*System.out.println("\nScores for cells: ");
           for(int i=0;i<x;++i){
               for(int j=0;j<x;++j){
                   if(grid[i][j]!=null)System.out.printf("%-3d ", grid[i][j].finalCost);
                   else System.out.print("BL  ");
               }
               System.out.println();
           }
           System.out.println();*/
            
           if(closed[endI][endJ]){
               //Trace back the path 
                //System.out.println("Path: ");
                Cell current = grid[endI][endJ];
                System.out.print(current);
                while(current.parent!=null){
                	
                	path.add(new Coord(current.j,current.i));
                	
                    //System.out.print(" -> "+current.parent);
                    current = current.parent;
                }
                Collections.reverse(path);
                System.out.println();
           }else System.out.println("No possible path");
           
           System.out.println(path);
           
           /*for(Coord next : path){
        	   
           }*/
           
           return path;
    }
    
    private void MoveOnPath(List<Coord> path){
    	
    	// scan the path list and move the rover along the path
    	
    	//for(Coord next)
    }
	
	
	private List<Coord> UpdateBlockCells(int x, int y){
		// current x, y
		int curr_x = x;
		int curr_y = y;
		
		// Create array list for blocked cells
		List<Coord> blockedcells = new ArrayList<Coord>();
		
		// pull the MapTile array out of the ScanMap object
		MapTile[][] scanMapTiles = scanMap.getScanMap();
		int centerIndex = (scanMap.getEdgeSize() - 1)/2;
		// tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
		
		// Check East
		if(		scanMapTiles[centerIndex + 1][centerIndex].getHasRover() 
				|| scanMapTiles[centerIndex +1][centerIndex].getTerrain() == Terrain.ROCK
				|| scanMapTiles[centerIndex +1][centerIndex].getTerrain() == Terrain.SAND
				|| scanMapTiles[centerIndex +1][centerIndex].getTerrain() == Terrain.NONE){
			eastBlocked = true;
			blockedcells.add(new Coord(curr_x+1, curr_y));
		}else {
			eastBlocked=false;
		}
		
		// Printing the East Block Status
		System.out.println("East Block Status : "+eastBlocked);
		
		// Check West
		if(		scanMapTiles[centerIndex-1][centerIndex].getHasRover() 
				|| scanMapTiles[centerIndex -1][centerIndex].getTerrain() == Terrain.ROCK
				|| scanMapTiles[centerIndex -1][centerIndex].getTerrain() == Terrain.SAND
				|| scanMapTiles[centerIndex -1][centerIndex].getTerrain() == Terrain.NONE){
			westBlocked = true;
			blockedcells.add(new Coord(curr_x-1, curr_y));
		}else {
			westBlocked=false;
		}
		
		// Printing the West Block Status
		System.out.println("West Block Status : "+westBlocked);
		
		// Check North
		if(		scanMapTiles[centerIndex][centerIndex -1].getHasRover() 
				|| scanMapTiles[centerIndex][centerIndex -1].getTerrain() == Terrain.ROCK
				|| scanMapTiles[centerIndex][centerIndex -1].getTerrain() == Terrain.SAND
				|| scanMapTiles[centerIndex][centerIndex -1].getTerrain() == Terrain.NONE){
			northBlocked = true;
			blockedcells.add(new Coord(curr_x, curr_y-1));
		}else {
			northBlocked=false;
		}
		
		// Printing the North Block Status
		System.out.println("North Block Status : "+northBlocked);
		
		// Check South
		if(		scanMapTiles[centerIndex][centerIndex +1].getHasRover() 
				|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.ROCK
				|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.SAND
				|| scanMapTiles[centerIndex][centerIndex +1].getTerrain() == Terrain.NONE){
			southBlocked = true;
			blockedcells.add(new Coord(curr_x, curr_y+1));
		}else {
			southBlocked=false;
		}
		
		// Printing the South Block Status
		System.out.println("South Block Status : "+southBlocked);
		
		return blockedcells;
	}
	
	private void AddChemicalLocations(String currentCoord,
			ArrayList<String> chemicalsFetch) {
		// TODO Auto-generated method stub
		
		//declaring variables for current x & y , chemical x & y
		int x_Current=0, y_Current=0, x_Chemical=0, y_Chemical=0;
		
		boolean duplicate=false;
		
		String chemicalLocation=null;
		
		//extracting the current coordinates and putting into integer variables
		String[] currentCoordinates = currentCoord.split(" ");
		x_Current = Integer.parseInt(currentCoordinates[0]);
		y_Current = Integer.parseInt(currentCoordinates[1]);
		
		// iterating the chemicalsfetch array list for all the chemical locations
		for(String s:chemicalsFetch){
			//extracting the chemical coordinates and putting into integer variables
			String[] chemicalCoordinates = s.split(" ");
			x_Chemical = Integer.parseInt(chemicalCoordinates[0]);
			y_Chemical = Integer.parseInt(chemicalCoordinates[1]);
			
			// checking the x value of chemical coordinate in the scan map
			// least will be 0 and max will 10 while 5 will be median
			switch(x_Chemical){
			case 0:
				x_Chemical=x_Current-5;
				break;
			case 1:
				x_Chemical=x_Current-4;
				break;
			case 2:
				x_Chemical=x_Current-3;
				break;
			case 3:
				x_Chemical=x_Current-2;
				break;
			case 4:
				x_Chemical=x_Current-1;
				break;
			case 5:
				x_Chemical=x_Current;
				break;
			case 6:
				x_Chemical=x_Current+1;
				break;
			case 7:
				x_Chemical=x_Current+2;
				break;
			case 8:
				x_Chemical=x_Current+3;
				break;
			case 9:
				x_Chemical=x_Current+4;
				break;
			case 10:
				x_Chemical=x_Current+5;
				break;
			}
			
			// checking the y value of chemical coordinate in the scan map
			// least will be 0 and max will 10 while 5 will be median
			switch(y_Chemical){
			case 0:
				y_Chemical=y_Current-5;
				break;
			case 1:
				y_Chemical=y_Current-4;
				break;
			case 2:
				y_Chemical=y_Current-3;
				break;
			case 3:
				y_Chemical=y_Current-2;
				break;
			case 4:
				y_Chemical=y_Current-1;
				break;
			case 5:
				y_Chemical=y_Current;
				break;
			case 6:
				y_Chemical=y_Current+1;
				break;
			case 7:
				y_Chemical=y_Current+2;
				break;
			case 8:
				y_Chemical=y_Current+3;
				break;
			case 9:
				y_Chemical=y_Current+4;
				break;
			case 10:
				y_Chemical=y_Current+5;
				break;
			}
			
			// checking whether coordinates are not negative
			if(x_Chemical>=0 && y_Chemical>=0){
				//creating a string form of coordinates to store in arraylist
				chemicalLocation=x_Chemical+","+y_Chemical;
				// iterating through existing coordinates arraylist for duplicates
				for(String loc:this.chemicalLocations){
					if(loc.equals(chemicalLocation)){
						duplicate=true;
						break;
					}
						
				}
				// adding to arraylist if no duplicates found above
				if(!duplicate)
					this.chemicalLocations.add(chemicalLocation);
					duplicate=false;
			}			
			
		}
		
	}

	private void clearReadLineBuffer() throws IOException{
		while(in.ready()){
			//System.out.println("ROVER_14 clearing readLine()");
			String garbage = in.readLine();	
		}
	}
	

	// method to retrieve a list of the rover's equipment from the server
	private ArrayList<String> getEquipment() throws IOException {
		//System.out.println("ROVER_14 method getEquipment()");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		out.println("EQUIPMENT");
		
		String jsonEqListIn = in.readLine(); //grabs the string that was returned first
		if(jsonEqListIn == null){
			jsonEqListIn = "";
		}
		StringBuilder jsonEqList = new StringBuilder();
		//System.out.println("ROVER_14 incomming EQUIPMENT result - first readline: " + jsonEqListIn);
		
		if(jsonEqListIn.startsWith("EQUIPMENT")){
			while (!(jsonEqListIn = in.readLine()).equals("EQUIPMENT_END")) {
				if(jsonEqListIn == null){
					break;
				}
				//System.out.println("ROVER_14 incomming EQUIPMENT result: " + jsonEqListIn);
				jsonEqList.append(jsonEqListIn);
				jsonEqList.append("\n");
				//System.out.println("ROVER_14 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return null; // server response did not start with "EQUIPMENT"
		}
		
		String jsonEqListString = jsonEqList.toString();		
		ArrayList<String> returnList;		
		returnList = gson.fromJson(jsonEqListString, new TypeToken<ArrayList<String>>(){}.getType());		
		//System.out.println("ROVER_14 returnList " + returnList);
		
		return returnList;
	}
	

	// sends a SCAN request to the server and puts the result in the scanMap array
	public void doScan() throws IOException {
		//System.out.println("ROVER_14 method doScan()");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		out.println("SCAN");

		String jsonScanMapIn = in.readLine(); //grabs the string that was returned first
		if(jsonScanMapIn == null){
			System.out.println("ROVER_14 check connection to server");
			jsonScanMapIn = "";
		}
		StringBuilder jsonScanMap = new StringBuilder();
		System.out.println("ROVER_14 incomming SCAN result - first readline: " + jsonScanMapIn);
		
		if(jsonScanMapIn.startsWith("SCAN")){	
			while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
				//System.out.println("ROVER_14 incomming SCAN result: " + jsonScanMapIn);
				jsonScanMap.append(jsonScanMapIn);
				jsonScanMap.append("\n");
				//System.out.println("ROVER_14 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return; // server response did not start with "SCAN"
		}
		//System.out.println("ROVER_14 finished scan while");

		String jsonScanMapString = jsonScanMap.toString();
		// debug print json object to a file
		//new MyWriter( jsonScanMapString, 0);  //gives a strange result - prints the \n instead of newline character in the file

		//System.out.println("ROVER_14 convert from json back to ScanMap class");
		// convert from the json string back to a ScanMap object
		scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);		
	}
	

	// this takes the LOC response string, parses out the x and x values and
	// returns a Coord object
	public static Coord extractLOC(String sStr) {
		int indexOf;
        indexOf = sStr.indexOf(" ");
        sStr = sStr.substring(indexOf + 1);
        if (sStr.lastIndexOf(" ") != -1) {
            String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
            //System.out.println("extracted xStr " + xStr);

            String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
            //System.out.println("extracted yStr " + yStr);
            return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
        }
        return null;
	}
	
	

	/**
	 * Runs the client
	 */
	public static void main(String[] args) throws Exception {
		ROVER_14 client = new ROVER_14();
		client.run();
	}
}