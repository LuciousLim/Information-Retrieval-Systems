/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        // if the token does not exist, put <token, docID>
        if(!index.containsKey(token)){
            PostingsList pl = new PostingsList();
            pl.add(new PostingsEntry(docID));
            index.put(token, pl);
        }
        // if token exists but it does not contain the docID, create a new entry and insert
        else if (!getPostings(token).getList().contains(docID)){
            getPostings(token).add(new PostingsEntry(docID));
        }
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        return index.get(token);
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
