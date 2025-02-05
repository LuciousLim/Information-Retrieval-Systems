/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.Arrays;

public class PostingsList {
    
    /** The postings list */
    private final ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

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

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (PostingsEntry entry : list) {
            s.append(entry.toString()).append(";");
        }
        s.deleteCharAt(s.length() - 1);
        s.append("\n");
        return s.toString();
    }

    public static PostingsList decode(String s) {
        String[] entries = s.split(";");
        PostingsList postingsList = new PostingsList();
        for (String entry : entries) {
            String[] parts = entry.split(":");


            try {
                int docID = Integer.parseInt(parts[0]);
                String[] offsetsAndScore = parts[1].split(",");
                PostingsEntry newEntry = new PostingsEntry(docID);

                if (offsetsAndScore.length < 2) {
                    return null;
                } else {
                    int[] offsets = new int[offsetsAndScore.length - 1];
                    for (int i = 0; i < offsetsAndScore.length - 1; i++) {
                        offsets[i] = Integer.parseInt(offsetsAndScore[i]);
                    }
                    for (int offset : offsets) {
                        newEntry.addOffset(offset);
                        newEntry.score++;
                    }
                    double score = Double.parseDouble(offsetsAndScore[offsetsAndScore.length - 1]);
                    newEntry.score = score;
                }

                // add the complete entry to the postings list
                postingsList.add(newEntry);

            } catch (NumberFormatException e) {
                System.err.println("Error parsing postings list: " + s);
                e.printStackTrace();
                System.exit(1);
            }

//            try {
//                int docID = Integer.parseInt(parts[0]);
//                String[] offsets = parts[1].split(",");
//                PostingsEntry newEntry = new PostingsEntry(docID);
//
//                // iterate over each offset and add to the entry
//                for (int i = 0; i < offsets.length; i++) {
//                    newEntry.addOffset(Integer.parseInt(offsets[i].trim()));
//                    newEntry.score++;
//                }
//
//                // add the complete entry to the postings list
//                postingsList.add(newEntry);
//
//            } catch (NumberFormatException e) {
//                // handle parsing errors
//                System.err.println("Error parsing postings list: " + s);
//                e.printStackTrace();
//                System.exit(1);
//            }
        }

        return postingsList;
    }

//    public static PostingsList decode(String s) {
//        PostingsList postingsList = new PostingsList();
//        String[] entries = s.split(";");
//        for (String entry : entries) {
//            String[] parts = entry.split(":");
//            try {
//                int docID = Integer.parseInt(parts[0]);
//                String[] offsets = parts[1].split(",");
//                postingsList.add(new PostingsEntry(docID, 1, Integer.parseInt(offsets[0])));
//                for (int i = 1; i < offsets.length - 1; i++) {
//                    postingsList.get(postingsList.size() - 1).offsets.add(Integer.parseInt(offsets[i]));
//                    postingsList.get(postingsList.size() - 1).score++;
//                }
//            } catch (NumberFormatException e) {
//                System.err.println("Error parsing postings list: " + s);
//                e.printStackTrace();
//                System.exit(1);
//            }
//        }
//        return postingsList;
//    }
}

