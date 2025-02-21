package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Ranking {
    public static PostingsList tf_idf(Query query, PostingsList postingsList, Index index, String tf_scheme, String df_scheme){
        return cosineScore(query, postingsList, index, tf_scheme, df_scheme);
    }

    public static PostingsList cosineScore(Query query, PostingsList postingsList, Index index, String tf_scheme, String df_scheme){
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

        for (int doc = 0; doc < postingsList.size(); doc++){
            postingsList.get(doc).score = score.get(doc) / index.docLengths.get(postingsList.get(doc).docID);
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
