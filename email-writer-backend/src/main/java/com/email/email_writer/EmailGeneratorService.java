package com.email.email_writer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;


@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    private final String apikey;


    public EmailGeneratorService(
            @Value("${gemini.api.url}") String baseUrl,
            @Value("${gemini.api.key}") String geminiApiKey) {

        this.apikey = geminiApiKey;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

//    public EmailGeneratorService(WebClient.Builder webClientBuilder,
//            @Value("${gemini.api.url}")  String baseUrl,
//            @Value("${gemini.api.key}")  String geminiApiKey ) {
//
//
//        this.apikey = geminiApiKey;
//       // this.webClient = webClientBuilder.baseUrl(baseUrl).build();
//        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
//    }

    public String generateEmailReply(EmailRequest emailRequest) {
    // logic
        // build prompt
         String prompt = buildPrompt(emailRequest);

        // Prepare raw JSON Body
        String requestBody = String.format("""
                {
                    "contents": [
                      {
                        "parts": [
                          {
                            "text": "%s"
                          }
                        ]
                      }
                    ]
                  }
                """,prompt);
        // Send Request to google server

        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1beta/models/gemini-3-flash-preview:generateContent").build())
                .header("x-goog-api-key",apikey)
                .header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        // Extract Response
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        // ObjectMapper mapper = new ObjectMapper();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
         return    root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        }

        catch(JsonProcessingException e ){
            throw new RuntimeException(e);
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        // setting first line
        prompt.append("Generate a only one  professional email reply without subject and placeholders for the following email :  ");
        // checking tone
        if(emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");
        }
        // adding content
        prompt.append("Original Email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();


    }


}
