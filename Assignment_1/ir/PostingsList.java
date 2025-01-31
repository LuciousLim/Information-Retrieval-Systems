/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

public class PostingsList implements Cloneable{
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();


    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
        return list.get( i );
    }

    public void clearList() {
        this.list.clear();
    }

    public void copy(PostingsList postingsList) {
        for (int i = 0; i < postingsList.size(); i++) {
            this.list.add(postingsList.list.get(i));
        }
    }


    /** Insert element in descending order*/
    public void add(PostingsEntry postingsEntry) {
        if(size() > 0){
            for(int i = 0; i < size(); i++){
                if (postingsEntry.docID == list.get(i).docID){
                    return;
                }
                else if (i == size() - 1) {
                    list.add(postingsEntry);
                }
                else if (postingsEntry.docID > list.get(i).docID){
                    list.add(i, postingsEntry);
                    return;
                }
            }
        }   else {
            list.add(postingsEntry);
        }

    }
    // 
    //  YOUR CODE HERE
    //
}

