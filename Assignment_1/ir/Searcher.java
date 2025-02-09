/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) {

        // task 1.2, single word search
        if(query.queryterm.size() == 1){
            return this.index.getPostings(query.queryterm.get(0).term);
        }

        // task 1.3, intersect search
        else if (query.queryterm.size() > 1 && queryType.equals(QueryType.INTERSECTION_QUERY)){
//            return intersection(extractPostingLists(query));
            PostingsList result = null;

            // traverse the queryterms
            for (int i = 0; i < query.queryterm.size(); i++){
                PostingsList postingsList = index.getPostings(query.queryterm.get(i).term);

                // return an empty list if the posting list is empty,
                if (postingsList == null){
                    return new PostingsList();
                }

                // when the first term come, the result is empty,
                // give the first term's posting list to it
                if (result == null){
                    result = postingsList;
                } else {
                    result = intersect(result, postingsList);
                }

            }

            return result;

        }

        // task 1.4, phrase search
        else if (query.queryterm.size() > 1 && queryType.equals(QueryType.PHRASE_QUERY)){
            PostingsList result = null;

            // traverse the queryterms
            for (int i = 0; i < query.queryterm.size(); i++){
                PostingsList postingsList = index.getPostings(query.queryterm.get(i).term);

                // return an empty list if the posting list is empty,
                if (postingsList == null){
                    return new PostingsList();
                }

                // when the first term come, the result is empty,
                // give the first term's posting list to it
                if (result == null){
                    result = postingsList;
                } else {
                    result = phrase(result, postingsList);
                }

            }

            return result;
        }

        return null;
    }

//    public List<PostingsList> extractPostingLists(Query query){
//        List<PostingsList> postingsLists = new ArrayList<>();
//        if (query.queryterm.size() > 1) {
//            for (int i = 0; i < query.queryterm.size(); i++) {
//                postingsLists.add(this.index.getPostings((query.queryterm.get(i).term)));
//            }
//        }
//        return postingsLists;
//    }

//    public PostingsList intersection(List<PostingsList> postingsLists){
//        if (postingsLists == null || postingsLists.size() < 2) {
//            return new PostingsList();
//        }
//
//        PostingsList result = new PostingsList();
//        PostingsList comparingList = new PostingsList();
//        int left = 0, right = 1;
//
//        while (left < postingsLists.size() && right < postingsLists.size()) {
//            if (postingsLists.size() >= 2){
//                if (left == 0){
//                    comparingList.copy(postingsLists.get(left));
//                }
//                else if (result.size() != 0){
//                    comparingList.clearList();
//                    comparingList.copy(result);
//                    result.clearList();
//                }
//                else {
//                    return null;
//                }
//
//                int i = 0, j = 0;
//                while (i < comparingList.size() && j < postingsLists.get(right).size()){
//                    int doc_i = comparingList.get(i).docID, doc_j = postingsLists.get(right).get(j).docID;
//                    if (doc_i == doc_j){
//                        result.add(new PostingsEntry(doc_i));
//                        i++;
//                        j++;
//                    }
//                    else if (doc_i > doc_j){
//                        i++;
//                    }
//                    else {
//                        j++;
//                    }
//                }
//
//                left++;
//                right++;
//            }
//        }
//
//        return result;
//    }

    public PostingsList intersect(PostingsList pl1, PostingsList pl2){
        PostingsList result = new PostingsList();

        int i = 0, j = 0;
        while (i < pl1.size() && j < pl2.size()){
            int doc_i = pl1.get(i).docID, doc_j = pl2.get(j).docID;
            if (doc_i == doc_j){
                result.add(new PostingsEntry(doc_i));
                i++;
                j++;
            }
            else if (doc_i < doc_j){
                // the docIDs are in ascending order,
                i++;
            }
            else {
                j++;
            }
        }

        return result;
    }

    public PostingsList phrase(PostingsList pl1, PostingsList pl2){
        PostingsList result = new PostingsList();

        int i = 0, j =0;
        while (i < pl1.size() && j < pl2.size()){

            if (pl1.get(i).docID == pl2.get(j).docID){
//                int m = 0, n = 0;
//                Collections.sort(pl1.get(i).offsets);
//                Collections.sort(pl2.get(j).offsets);
//                while (m < pl1.get(i).offsets.size() && n < pl2.get(j).offsets.size()){
//                    int offset_1 = pl1.get(i).offsets.get(m), offset_2 = pl2.get(j).offsets.get(n);
//
//                    if (offset_2 == offset_1 + 1){
//                        // if docIDs are the same and offset are nearby, insert it into result
//                        result.add(new PostingsEntry(pl1.get(i).docID, offset_2));
//                        break;
//                    }
//                    else if (offset_1 + 1 < offset_2){
//                        // the offsets are in ascending order,
//                        m++;
//                    }
//                    else {
//                        n++;
//                    }
//                }
                for (int m = 0; m < pl1.get(i).offsets.size(); m++){
                    for (int n = 0; n < pl2.get(j).offsets.size(); n++){
                        if (pl1.get(i).offsets.get(m) + 1 == pl2.get(j).offsets.get(n)){
                            result.add(new PostingsEntry(pl2.get(j).docID, pl2.get(j).offsets.get(n)));
                        }
                    }
                }
                i++;
                j++;
            }
            else  if (pl1.get(i).docID < pl2.get(j).docID){
                i++;
            }
            else {
                j++;
            }
        }

//        for (int k = 0; k < result.size(); k++){
//            System.out.print(result.get(k).docID + ",");
//        }
//        System.out.println("|****************|");

//        for (int p = 0; p < result.size(); p++){
//            System.out.print(result.get(p).docID + ": ");
//            for(int q = 0; q < result.get(p).offsets.size(); q++){
//                System.out.print(result.get(p).offsets.get(q) + ",");
//            }
//            System.out.println();
//        }
//        System.out.println("|****************|");

        return result;
    }


}