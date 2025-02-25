package ir;

import java.io.*;
import java.util.*;

public class Ranking {
    public static final double A = 0.5;
    public static double B = 1000;
    public static PostingsList tf_idf(Query query, PostingsList postingsList, Index index,
                                      String tf_scheme, String df_scheme, NormalizationType normType){
        return cosineScore(query, postingsList, index, tf_scheme, df_scheme, normType);
    }

    public static PostingsList pageRank(PostingsList postingsList, Index index){
        PostingsList rankedList = new PostingsList();
        HashMap<String, Double> pages = new HashMap<>();

        try (BufferedReader file = new BufferedReader(new FileReader("index/pagerank.txt"))) {
            String line;

            while ((line = file.readLine()) != null) {
                String[] arr = line.split(" ", 2);
                if (arr.length == 2) {
                    pages.put(arr[0], Double.parseDouble(arr[1]));
                }
            }

//            for (PostingsEntry e : postingsList.getList()){
//                String docName = index.docNames.get(e.docID).substring("..\\davisWiki\\".length());
//                while ((line = file.readLine()) != null) {
//                    String[] arr = line.split(" ", 2);
//                    if (arr.length == 2) {
//                        pages.put(arr[0], Double.parseDouble(arr[1]));
//                    }
//                }
//            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (PostingsEntry e : postingsList.getList()){
            String docName = index.docNames.get(e.docID).substring("..\\davisWiki\\".length());
            rankedList.add(new PostingsEntry(e.docID, pages.get(docName)));
        }

        Collections.sort(rankedList.getList());
        return rankedList;
    }

    public static PostingsList combination(Query query, PostingsList postingsList, Index index,
                                           String tf_scheme, String df_scheme, NormalizationType normType){
        PostingsList tf_idf_list = tf_idf(query, postingsList, index, tf_scheme, df_scheme, normType);
        PostingsList page_list = pageRank(postingsList, index);
        PostingsList combinedList = new PostingsList();

        for (PostingsEntry t : tf_idf_list.getList()){
            for (PostingsEntry p : page_list.getList()){
                if (t.docID == p.docID){
                    combinedList.add(new PostingsEntry(p.docID, A * t.score + B * p.score));
                }
            }
        }

        Collections.sort(combinedList.getList());

        return combinedList;
    }

    public static PostingsList cosineScore(Query query, PostingsList postingsList, Index index,
                                           String tf_scheme, String df_scheme, NormalizationType normType){
        ArrayList<Double> score = new ArrayList<>(Collections.nCopies(postingsList.size(), 0.0));
        ArrayList<HashMap<Integer, Integer>> tfs = new ArrayList<>();
        for (int i = 0; i < query.size(); i++) {
            tfs.add(new HashMap<>());
        }

        // Compute tf
        for (int i = 0; i < query.size(); i++){
            PostingsList pl = index.getPostings(query.queryterm.get(i).term);
            for (PostingsEntry e : pl.getList()){
                tfs.get(i).put(e.docID, e.getTf());
            }
        }

        for (int i = 0; i < query.queryterm.size(); i++){
            String term = query.queryterm.get(i).term;

            // Compute qtf
            double qtf = query.queryterm.get(i).weight;
            // Compute df
            int df = index.getPostings(query.queryterm.get(i).term).size();

            for (int doc = 0; doc < postingsList.size(); doc++){
                // Check if the current query term exists in the current document
                double tf = tfs.get(i).get(postingsList.get(doc).docID) != null
                        ? tfs.get(i).get(postingsList.get(doc).docID)
                        : 0.0;
                // Compute weight
                double Wftd = calWeight(tf, df, postingsList, index, tf_scheme, df_scheme);
                double Wtq = calWeight(qtf, df, postingsList, index, tf_scheme, df_scheme);
                double weight = Wftd * Wtq;
                score.set(doc, score.get(doc) + weight);
            }
        }

        if (normType.equals(NormalizationType.NUMBER_OF_WORDS)){
            for (int doc = 0; doc < postingsList.size(); doc++){
                postingsList.get(doc).score = score.get(doc) / index.docLengths.get(postingsList.get(doc).docID);
            }
        } else {
            for (int doc = 0; doc < postingsList.size(); doc++){
                postingsList.get(doc).score = score.get(doc) / index.docLengths_Euclidean.get(postingsList.get(doc).docID);
            }
        }


        Collections.sort(postingsList.getList());

        return postingsList;
    }

    public static Double calWeight(double tf, int df, PostingsList postingsList, Index index, String tf_scheme, String df_scheme){
        return tf_weightingScheme(tf, postingsList, tf_scheme) * df_weightingScheme(df, index, df_scheme);
    }

    public static double tf_weightingScheme(double tf, PostingsList postingsList, String scheme){
        return switch (scheme) {
            case "n" -> tf;
            case "l" -> 1 + Math.log(tf);
            default -> -1.0;
        };
    }

    public static double df_weightingScheme(int df, Index index, String scheme){
        return switch (scheme) {
            case "t" -> Math.log((double) index.docNames.size() / df);
            default -> -1.0;
        };
    }


}
