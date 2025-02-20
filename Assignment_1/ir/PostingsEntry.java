/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;

    // adding for task 1.4
    public ArrayList<Integer> offsets = new ArrayList<Integer>();


    public  PostingsEntry(){}

    public  PostingsEntry(int docID){
        this.docID = docID;
    }

    public PostingsEntry(int docID, int offset){
        this.docID = docID;
        this.offsets.add(offset);
    }

    public PostingsEntry(int docID, int score, int offset){
        this.docID = docID;
        this.score = score;
        this.offsets.add(offset);
    }

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {return Double.compare( other.score, score );}

    public void addOffset(int offset){
        this.offsets.add(offset);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(docID).append(":");
        for (Integer offset : offsets) {
            s.append(offset).append(",");
        }
        s.append(score);
        return s.toString();
    }

    public int getTf(){
        return offsets.size();
    }


    //
    // YOUR CODE HERE
    //
}

