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
		System.out.printf("Took %.2f seconds", (float) elapsedTime/1000);

		writePagerank(numberOfDocs, rankedDocs);
    }

	void writePagerank(int numberOfDocs, List<DocRank> rankedDocs){
		try (BufferedReader in = new BufferedReader(new FileReader("./davisTitles.txt"));
			 BufferedWriter fw = new BufferedWriter(new FileWriter("./pagerank.txt"))) {

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
				String docTitle = realName.getOrDefault(docName[i], "Unknown Title");
				fw.write(String.format("%s %.9f%n", docTitle, rankedDocs.get(i).rank));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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