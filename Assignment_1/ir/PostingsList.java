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

    public ArrayList<PostingsEntry> getList(){
        return list;
    }


    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
        return list.get( i );
    }

    public PostingsEntry getById(int docID){
        for (PostingsEntry entry : list){
            if (entry.docID == docID){
                return entry;
            }
        }
        return new PostingsEntry();
    }

    public boolean isContainById(int docID){
        for (PostingsEntry entry : list){
            if (entry.docID == docID){
                return true;
            }
        }
        return false;
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
                // if there has same docID, quit insert
                if (postingsEntry.docID == list.get(i).docID){
                    return;
                }
                // move the cursor until it can not find an element that is smaller than it
                else if (postingsEntry.docID > list.get(i).docID){
                    list.add(i, postingsEntry);
                    return;
                }
                else if (i == size() - 1) {
                    list.add(postingsEntry);
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

