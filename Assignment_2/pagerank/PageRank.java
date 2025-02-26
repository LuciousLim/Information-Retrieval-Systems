import java.util.*;
import java.io.*;

public class PageRank {

    /**
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

	Random rand = new Random();
	double[] exactPageRank;
	int[] visits;



	class DocRank implements Comparable<DocRank> {
		int index;
		double rank;

		public DocRank(int index, double rank) {
			this.index = index;
			this.rank = rank;
		}

		@Override
		public int compareTo(DocRank other) {
			return Double.compare(other.rank, this.rank);
		}
	}


    /* --------------------------------------------- */


    public PageRank( String filename ) {
		int noOfDocs = readDocs( filename );
		iterate( noOfDocs, 1000 );
		monteCarlo( noOfDocs, 1000 );
		Svwiki(3);
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures.
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
		int fileIndex = 0;
		try {
		    System.err.print( "Reading file... " );
		    BufferedReader in = new BufferedReader( new FileReader( filename ));
		    String line;
		    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
				int index = line.indexOf( ";" );
				String title = line.substring( 0, index );
				Integer fromdoc = docNumber.get( title );
				//  Have we seen this document before?
				if ( fromdoc == null ) {
				    // This is a previously unseen doc, so add it to the table.
				    fromdoc = fileIndex++;
				    docNumber.put( title, fromdoc );
				    docName[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
				while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
				    String otherTitle = tok.nextToken();
				    Integer otherDoc = docNumber.get( otherTitle );
				    if ( otherDoc == null ) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = fileIndex++;
						docNumber.put( otherTitle, otherDoc );
						docName[otherDoc] = otherTitle;
				    }
				    // Set the probability to 0 for now, to indicate that there is
				    // a link from fromdoc to otherDoc.
				    if ( link.get(fromdoc) == null ) {
						link.put(fromdoc, new HashMap<Integer,Boolean>());
				    }
				    if ( link.get(fromdoc).get(otherDoc) == null ) {
						link.get(fromdoc).put( otherDoc, true );
						out[fromdoc]++;
				    }
				}
		    }
		    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
				System.err.print( "stopped reading since documents table is full. " );
		    }
		    else {
				System.err.print( "done. " );
		    }
		}
		catch ( FileNotFoundException e ) {
		    System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
		    System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + fileIndex + " number of documents" );
		return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate( int numberOfDocs, int maxIterations ) {
		long startTime = System.currentTimeMillis();

		if (numberOfDocs == 0 || link.isEmpty()){
			System.err.println("No document");
			return;
		}

		double[] a = new double[numberOfDocs];
		a[0] = 1;

		int n = 0;
		double[] a_old = new double[numberOfDocs];
		double norm = 1;

		while (norm > EPSILON && n < maxIterations){
			norm = 0;
			System.arraycopy(a, 0, a_old, 0, numberOfDocs);
			Arrays.fill(a, 0);

			for (int from = 0; from < numberOfDocs; from++){
				// ******* Computing P *******
				// Dead ends are considered to have equal probability of linking to every page in the network.
				if (out[from] == 0){
					for (int to = 0; to < numberOfDocs; ++to) {
						a[to] += (1-BORED) * (1.0d / numberOfDocs) * a_old[from];
					}
					continue;
				}

				for (int to : link.get(from).keySet()){
					double P = (1 - BORED) * ( 1.0d / out[from]);
					a[to] += a_old[from] * P;
				}
			}

			// ******* Computing J *******
			double sum = 0;
			for (int i = 0; i < numberOfDocs; i++) {
				sum += a_old[i];
			}
			for (int i = 0; i < numberOfDocs; i++) {
				a[i] += BORED * (sum / numberOfDocs);
			}

			// ******* Computing Norm *******
			for (int i = 0; i < numberOfDocs; i++){
				norm += Math.abs(a[i] - a_old[i]);
			}

			n++;
			System.out.printf("Iteration: %-30s Norm: %f%n", n, norm);
		}

		List<DocRank> rankedDocs = new ArrayList<>();
		for (int i = 0; i < numberOfDocs; i++) {
			rankedDocs.add(new DocRank(i, a[i]));
		}
		Collections.sort(rankedDocs);

		System.out.println("\nSorted PageRank Results:");
		for (int i = 0; i < 30; i++) {
			System.out.printf("Document: %-30s PageRank: %.5f%n", docName[rankedDocs.get(i).index], rankedDocs.get(i).rank);
		}

		long elapsedTime = System.currentTimeMillis() - startTime;
		System.out.printf("Took %.2f seconds%n", (float) elapsedTime/1000);

		exactPageRank = a;
		writePagerank(numberOfDocs, rankedDocs, "./davisTitles.txt", "./pagerank.txt");
    }

	void writePagerank(int numberOfDocs, List<DocRank> rankedDocs, String titleFile, String targetFile){
		try (BufferedReader in = new BufferedReader(new FileReader(titleFile));
			 BufferedWriter fw = new BufferedWriter(new FileWriter(targetFile))) {

			// Store the mapping between document IDs and their real names
			Map<String, String> realName = new HashMap<>();
			String line;

			// Read and store the title mapping relationships
			while ((line = in.readLine()) != null) {
				String[] arr = line.split(";");
				if (arr.length == 2) {
					realName.put(arr[0].trim(), arr[1].trim());
				}
			}
            
			// Write the PageRank results to the output file
			for (int i = 0; i < numberOfDocs; i++) {
                int index = rankedDocs.get(i).index;
				String docTitle = realName.getOrDefault(docName[index], "Unknown Title");
				fw.write(String.format("%s %.9f%n", docTitle, rankedDocs.get(i).rank));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void monteCarlo(int numberOfDocs, int maxIterations) {
		int m = 20;

		System.out.println("\nMonte-Carlo 1:");
		int simulations = 0;
		int totalVisits = 0;
		visits = new int[numberOfDocs];
		long startTime1 = System.currentTimeMillis();
		for (int i = 0; i < m; i++) {
			totalVisits += mc1(numberOfDocs, maxIterations);
			simulations++;
			long elapsedTime = System.currentTimeMillis() - startTime1;
			printMC(simulations, totalVisits, elapsedTime);
		}

		System.out.println("\nMonte-Carlo 2:");
		simulations = 0;
		totalVisits = 0;
		visits = new int[numberOfDocs];
		long startTime2 = System.currentTimeMillis();
		for (int i = 0; i < m; i++) {
			totalVisits += mc2(numberOfDocs, maxIterations);
			simulations++;
			long elapsedTime = System.currentTimeMillis() - startTime2;
			printMC(simulations, totalVisits, elapsedTime);
		}

		System.out.println("\nMonte-Carlo 4:");
		simulations = 0;
		totalVisits = 0;
		visits = new int[numberOfDocs];
		long startTime4 = System.currentTimeMillis();
		for (int i = 0; i < m; i++) {
			totalVisits += mc4(numberOfDocs);
			simulations++;
			long elapsedTime = System.currentTimeMillis() - startTime4;
			printMC(simulations, totalVisits, elapsedTime);
		}

		System.out.println("Top 30:");
		double[] a = new double[numberOfDocs];
		for (int i = 0; i < a.length; ++i) {
			a[i] = (double) visits[i] / totalVisits;
		}

		List<DocRank> rankedDocsMC = new ArrayList<>();
		for (int i = 0; i < numberOfDocs; i++) {
			rankedDocsMC.add(new DocRank(i, a[i]));
		}
		Collections.sort(rankedDocsMC);

		System.out.println("\nSorted PageRank Results:");
		for (int i = 0; i < 30; i++) {
			System.out.printf("Document: %-30s PageRank: %.5f%n", docName[rankedDocsMC.get(i).index], rankedDocsMC.get(i).rank);
		}

		System.out.println("\nMonte-Carlo 5:");
		simulations = 0;
		totalVisits = 0;
		visits = new int[numberOfDocs];
		long startTime5 = System.currentTimeMillis();
		for (int i = 0; i < m; i++) {
			totalVisits += mc5(numberOfDocs);
			simulations++;
			long elapsedTime = System.currentTimeMillis() - startTime5;
			printMC(simulations, totalVisits, elapsedTime);
		}
	}

	/**
	 * Perform n random walks among all nodes
	 * Walks stop when the surfer gets bored or the maximum jumps are reached
	 * Register the end point
	 */
	public int mc1(int numberOfDocs, int maxIterations) {
		int totalVisits = 0;

		for (int i = 0; i < numberOfDocs; i++) {
			// Start the random walk from a random document
			int cur = rand.nextInt(numberOfDocs);
			int counter = 0;	// Counter to track the number of steps in the current random walk

			while (counter < maxIterations) {
				// With probability BORED, stop the walk
				if (rand.nextDouble() < BORED) {
					break;
				}
				// If no out links, jump to a random document
				if (out[cur] == 0) {
					cur = rand.nextInt(numberOfDocs);
				} else {
					// If there are outgoing links, randomly choose one to jump
					int index = rand.nextInt(out[cur]);
					cur = (int) link.get(cur).keySet().toArray()[index];
				}
				counter++;
			}
			visits[cur]++;
			totalVisits++;
		}

		return totalVisits;
	}

	/**
	 * Perform n random walks starting from each node
	 * Walks stop when the surfer gets bored or the maximum jumps are reached
	 * Register the end point
	 */
	public int mc2(int numberOfDocs, int maxIterations) {
		int totalVisits	= 0;
		for (int i = 0; i < numberOfDocs; i++) {
			// Start the random walk from document i
			int cur = i;
			int counter = 0;

			while (counter < maxIterations) {
				// With probability BORED, stop the walk
				if (rand.nextDouble() < BORED) {
					break;
				}
				// If no out links, jump to a random document
				if (out[cur] == 0) {
					cur = rand.nextInt(numberOfDocs);
				} else {
					// If there are outgoing links, randomly choose one to jump
					int index = rand.nextInt(out[cur]);
					cur = (int) link.get(cur).keySet().toArray()[index];
				}
				counter++;
			}
			visits[cur]++;
			totalVisits++;
		}
		return totalVisits;
	}

	/**
	 * Perform n random walks starting from each node
	 * Walks stop when the surfer gets bored or reaches the dangling node（node has no out link）
	 * Register all nodes visited along the path
	 */
	public int mc4(int numberOfDocs) {
		int totalVisits = 0;
		for (int i = 0; i < numberOfDocs; i++) {
			// Start the random walk from document i
			int cur = i;
			while (true) {
				// Register all nodes visited along the path
				totalVisits++;
				visits[cur]++;
				if (rand.nextDouble() < BORED) {
					break;
				}
				if (out[cur] == 0) break;
				int index = rand.nextInt(out[cur]);
				cur = (int) link.get(cur).keySet().toArray()[index];
			}
		}
		return totalVisits;
	}

	/**
	 * Perform n random walks among all nodes
	 * Walks stop when the surfer gets bored or reaches the dangling node（node has no out link）
	 * Register all nodes visited along the path
	 */
	public int mc5(int numberOfDocs) {
		int totalVisits = 0;
		for (int i = 0; i < numberOfDocs; i++) {
			// Start the random walk from document i
			int cur = rand.nextInt(numberOfDocs);
			while (true) {
				// Register all nodes visited along the path
				totalVisits++;
				visits[cur]++;
				if (rand.nextDouble() < BORED) {
					break;
				}
				if (out[cur] == 0) break;
				int index = rand.nextInt(out[cur]);
				cur = (int) link.get(cur).keySet().toArray()[index];
			}
		}
		return totalVisits;
	}

	public void printMC(int simulations, int totalVisits, long time){
		double dif = 0;
		for (int i = 0; i < visits.length; ++i) {
			double estimatedPageRank = (double) visits[i] / totalVisits;
			double delta = estimatedPageRank - exactPageRank[i];
			dif += delta*delta;
		}
		System.out.printf("Simulations: %-30s Difference: %-30.9f Time: %.2f seconds%n", simulations, dif, (float) time / 1000);

	}

	public void Svwiki(int m){
		link.clear();
		out = new int[MAX_NUMBER_OF_DOCS];
		int numberOfDocs = readDocs("linksSvwiki.txt");
		visits = new int[numberOfDocs];
		System.out.println("\nSvwiki:");
		int totalVisits = 0;
		double diff = 1;
		List<DocRank> newRank = new ArrayList<>(numberOfDocs);
		double[] oldRank = new double[30];

		while (diff > 1E-10){
			diff = 0;
			if (!newRank.isEmpty()){
				for (int i = 0; i < 30; i ++){
					oldRank[i] = newRank.get(i).rank;
				}
			}
			newRank.clear();

			// Use mc4 to iterate
			for (int i = 0; i < m; i++) {
				totalVisits += mc4(numberOfDocs);
			}

			// Compute difference
			for (int i = 0; i < numberOfDocs; i++) {
				newRank.add(new DocRank(i, (double) visits[i] / totalVisits));
			}
			Collections.sort(newRank);
			for (int i = 0; i < 30; i++){
				diff += Math.pow(newRank.get(i).rank - oldRank[i], 2) ;
			}

			System.out.printf("Difference: %-30s Target difference: %.10f%n", diff, 1E-9);
		}

		System.out.println("\nSorted PageRank Results:");
		for (int i = 0; i < 30; i++) {
			System.out.printf("Document: %-30s PageRank: %.15f%n", docName[newRank.get(i).index], newRank.get(i).rank);
		}

		writePagerank(numberOfDocs, newRank, "./svwikiTitles.txt", "./svwikiRank.txt");



	}

//	public void svWiki() {
//		docNumber = new HashMap<String,Integer>();
//		docName = new String[MAX_NUMBER_OF_DOCS];
//		link = new HashMap<Integer,HashMap<Integer,Boolean>>();
//		int numberOfDocs = readDocs("linksSvwiki.txt");
//		out = new int[numberOfDocs];
//		int totalVisits = 0;
//		double[] x = new double[numberOfDocs];
//		double[] xp = new double[numberOfDocs];
//		Integer[] indeces = new Integer[numberOfDocs];
//		for (Integer i = 0; i < numberOfDocs; ++i)
//			indeces[i] = i;
//		Integer[] indecesp = indeces.clone();
//
//		visits = new int[numberOfDocs];
//		while (true) {
//			totalVisits += mc4(numberOfDocs);
//
//			xp = x.clone();
//			for (int i = 0; i < x.length; ++i) {
//				x[i] = (double) visits[i] / totalVisits;
//			}
//			indecesp = indeces.clone();
//			Arrays.sort(indeces, new Comparator<Integer>() {
//						@Override
//						public int compare(Integer a, Integer b) {
//							return x[a] < x[b] ? 1 : x[a] == x[b] ? 0 : -1;
//						}
//					}
//			);
//			double norm2 = 0;
//			for (int i = 0; i < 30; ++i) {
//				double delta = x[indeces[i]] - xp[indeces[i]];
//				norm2 += delta*delta;
//			}
//			boolean stable = true;
//			for (int i = 0; i < 30; ++i) {
//				stable = stable && indeces[i] == indecesp[i];
//			}
//			System.out.println("norm2: " + norm2 + ", top30 stable: " + stable);
//
//			if (norm2 < 1e-10 && stable) {
//				System.out.println("Top30:");
//				for (int i = 0; i < 30; ++i) {
//					System.out.println(docName[indeces[i]] + ": " + x[indeces[i]]);
//				}
//				System.out.println();
//				try {
//					HashMap<String, String> realName = new HashMap<>();
//					BufferedReader in = new BufferedReader( new FileReader("./svwikiTitles.txt"));
//					String line;
//					while ((line = in.readLine()) != null) {
//						String[] arr = line.split(";");
//						realName.put(arr[0], arr[1]);
//					}
//					in.close();
//					for (int i = 0; i < 30; ++i) {
//						System.out.println(realName.get(docName[indeces[i]]) + " " + x[indeces[i]]);
//					}
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				break;
//			}
//		}
//	}
    /* --------------------------------------------- */


    public static void main( String[] args ) {
		if ( args.length != 1 ) {
			System.err.println( "Please give the name of the link file" );
		}
		else {
			new PageRank( args[0] );
		}
    }
}