package org.metadatacenter.sap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NcboAnnotator {
    private final String REST_URL;
    private final String API_KEY;
    static final ObjectMapper mapper = new ObjectMapper();

    private Map<String, Annotation> annotationMap = new HashMap<>();

    private int callCount = 0;
    private int cachedCount = 0;


    public NcboAnnotator(String REST_URL, String API_KEY)
    {
        this.REST_URL = REST_URL;
        this.API_KEY = API_KEY;
    }

    public Optional<Annotation> getAnnotation(String text, String[] ontology_ids)  {

        if (annotationMap.containsKey(text)) {
            cachedCount++;
            Annotation annotation = annotationMap.get(text);
            if (annotation != null)
                return Optional.of(annotation);
            else
                return Optional.empty();
        } else {
            callCount++;

            String urlParameters;
            String text_encoded;
            String ontolgyIds_encoded = "";


            try {
                text_encoded = URLEncoder.encode(text, "ISO-8859-1");

                for (String ontology_id : ontology_ids) {
                    ontolgyIds_encoded += URLEncoder.encode(ontology_id, "ISO-8859-1") + ",";
                }

                if (ontolgyIds_encoded != null && ontolgyIds_encoded.length() > 0 && ontolgyIds_encoded.charAt(ontolgyIds_encoded.length() - 1) == ',') {
                    ontolgyIds_encoded = ontolgyIds_encoded.substring(0, ontolgyIds_encoded.length() - 1);
                }

                urlParameters = "text=" + text_encoded +
                        "&ontologies=" + ontolgyIds_encoded +
                        "&longest_only=true";
                JsonNode annotations = jsonToNode(get(REST_URL + "/annotator?" + urlParameters));

                // choose the first annotation
                if (annotations.has(0)) {
                    JsonNode annotationNode = annotations.get(0);

                    // Get the details for the ontology term that was found in the annotation
                    JsonNode classDetails = jsonToNode(get(annotationNode.get("annotatedClass").get("links").get("self").asText()));
                    String termId = classDetails.get("@id").asText();
                    String prefLabel = classDetails.get("prefLabel").asText();
                    String ontologyId = classDetails.get("links").get("ontology").asText();

                    Annotation annotation = new Annotation(termId, prefLabel, ontologyId);
                    annotationMap.put(text, annotation);
                    return Optional.of(annotation);
                } else {
                    annotationMap.put(text, null);
                    return Optional.empty();
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }

    }


    private JsonNode jsonToNode(String json) {
        JsonNode root = null;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }

    private String get(String urlToGet) {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(urlToGet);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
            conn.setRequestProperty("Accept", "application/json");
            rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String post(String urlToGet, String urlParameters) {
        URL url;
        HttpURLConnection conn;

        String line;
        String result = "";
        try {
            url = new URL(urlToGet);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("charset", "utf-8");
            conn.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            conn.disconnect();

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public int getCallCount() {
        return callCount;
    }

    public int getCachedCount() {
        return cachedCount;
    }

    public static void main(String[] args) throws Exception {
        String REST_URL = "http://data.bioontology.org";
        String API_KEY = "24df94fa-54e0-11e0-9d7b-005056aa3316";

        NcboAnnotator annotator = new NcboAnnotator(REST_URL, API_KEY);

        String text = "breast cancer";
        String[] ontologyIds = {"http://data.bioontology.org/ontologies/DOID"};

        Optional<Annotation> annotation = annotator.getAnnotation(text, ontologyIds);
        if (annotation.isPresent())
            System.out.println(annotation.toString());
        else
            System.out.println("No Annotation");

        System.out.println("call count = " + annotator.getCallCount());
        System.out.println("cached count = " + annotator.getCachedCount());



    }
}
