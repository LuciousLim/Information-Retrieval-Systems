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
        if(!index.containsKey(token)){
            // if the token does not exist, create a new entry and insert
            PostingsList pl = new PostingsList();
            pl.add(new PostingsEntry(docID, offset));
            index.put(token, pl);
        }
        else if (!getPostings(token).isContainById(docID)){
            // if the token exists, but it does not contain the docID, create a new entry and insert
            getPostings(token).add(new PostingsEntry(docID, offset));
        }
        else if (getPostings(token).isContainById(docID)){
            // if the token exists, and it contains the docID, add offset to the corresponding posting entry
            getPostings(token).getById(docID).addOffset(offset);
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
