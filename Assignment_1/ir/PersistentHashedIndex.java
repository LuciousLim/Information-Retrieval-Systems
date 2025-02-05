/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.nio.charset.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        protected final long ptr;
        protected final long hash;
        protected final int size;
        public static final int BYTESIZE = 2 * Integer.BYTES + 2 * Long.BYTES;

        public Entry(long ptr, long hash, int size){
            this.ptr = ptr;     // pointer to the data file
            this.hash = hash;   // hash value of entry
            this.size = size;   // size of the string representation
        }

        public long getPtr() {return ptr;}

        public long getHash() {return hash;}

        public int getSize() {return size;}
    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        try {
            // allocate a byte buffer with size Entry.byteSize
            ByteBuffer buffer = ByteBuffer.allocate(Entry.BYTESIZE);

            // put the fields of the Entry into the buffer
            buffer.putLong(entry.getPtr());
            buffer.putLong(entry.getHash());
            buffer.putInt(entry.getSize());


            // move the file pointer to the specified position
            dictionaryFile.seek(ptr);

            // write the buffer contents to the file
            dictionaryFile.write(buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {
        try {
            // create a buffer with the size of an Entry
            ByteBuffer buffer = ByteBuffer.wrap(new byte[Entry.BYTESIZE]);

            // move the file pointer to the given position
            dictionaryFile.seek(ptr);

            // fill the buffer with data from the file
            dictionaryFile.readFully(buffer.array());

            // read the entry fields from the buffer
            long ptrData = buffer.getLong();
            long hash = buffer.getLong();
            int size = buffer.getInt();


            // return null if the entry is empty
            return (ptrData == 0 && size == 0 && hash == 0) ? null : new Entry(ptrData, hash, size);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put( new Integer(data[0]), data[1] );
                docLengths.put( new Integer(data[0]), new Integer(data[2]) );
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            collisions = write2Dict(collisions);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }

    public long hashFunction(String key) {
        return Math.abs(key.hashCode()) % TABLESIZE;
    }

    protected int write2Dict(int numOfCollisions) throws IOException {
        // clear the file content
        dictionaryFile.setLength(0);
        // cet the file length to TABLE_SIZE * Entry.BYTES to reserve space
        dictionaryFile.setLength(TABLESIZE * Entry.BYTESIZE);

        // iterate over each <token, PostingsList> pair in the index
        for (Map.Entry<String, PostingsList> entry : index.entrySet()) {
            String key = entry.getKey();
            PostingsList postingsList = entry.getValue();
            long hash = hashFunction(key); // compute the hash value for the key
            long ptrData = free; // starting position for writing
            long ptrDict = hash * Entry.BYTESIZE; // position of the entry in the dictionary file
            Entry e = readEntry(ptrDict); // read the entry at the current position

            // handle hash collisions
            while (e != null) {
                numOfCollisions++;
                hash = (hash + 1) % TABLESIZE; // Use linear probing to resolve collisions
                ptrDict = hash * Entry.BYTESIZE;
                e = readEntry(ptrDict); // Read the new position
            }

            // construct the data string to write
            String postingsListData = key + ">" + postingsList;
            int size = postingsListData.getBytes().length;

            // write the new entry to the dictionary file
            writeEntry(new Entry(ptrData, hash, size), ptrDict);

            // write the actual data and update the free pointer
            free += writeData(postingsListData, ptrData);
        }

        return numOfCollisions; // Return the number of collisions
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        long initialHash = hashFunction(token);
        long pointer = initialHash * Entry.BYTESIZE;

        // find the correct entry
        while (true) {
            Entry entry = readEntry(pointer);

            // check if entry is valid
            if (entry == null) {
                return null;
            }

            // verify hash and check token
            if (entry.getHash() == initialHash) {
                String entryData = readData(entry.getPtr(), entry.getSize());
                if (entryData.startsWith(token)) {
                    return PostingsList.decode(entryData.substring(token.length() + 1).trim());
                }
            }

            // handle collision with linear probing
            initialHash = (initialHash + 1) % TABLESIZE;
            pointer = initialHash * Entry.BYTESIZE;
        }
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        if (!index.containsKey(token)){
            // if the token does not exist, create a new entry and insert
            PostingsList pl = new PostingsList();
            pl.add(new PostingsEntry(docID, 1, offset));
            index.put(token, pl);
        }
        else {
            PostingsList pl = index.get(token);
            if(!pl.isContainById(docID)){
                // if the token exists, but it does not contain the docID, create a new entry and insert
                pl.add(new PostingsEntry(docID, 1, offset));
            }
            else {
                // if the token exists, and it contains the docID, add offset to the corresponding posting entry
                pl.getById(docID).addOffset(offset);
            }
        }
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
