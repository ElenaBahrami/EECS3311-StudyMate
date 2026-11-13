package studymate.agent;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

public class OpenAIAdapter
        implements LLMClient {

    private static final String MODEL_ID =
            "gpt-6-astra";

    private final OpenAIClient client;

    public OpenAIAdapter() {

        String apiKey =
                System.getenv(
                        "OPENAI_API_KEY"
                );

        if (apiKey == null
                || apiKey.isBlank()) {

            throw new IllegalStateException(
                    "OPENAI_API_KEY environment variable is missing."
            );
        }

        client =
                OpenAIOkHttpClient
                        .builder()
                        .apiKey(apiKey)
                        .build();
    }

    @Override
    public ModelReply generate(
            String context,
            String instructions)
            throws Exception {

        String input =
                """
                INSTRUCTIONS:
                %s

                COURSE EVIDENCE:
                %s
                """
                        .formatted(
                                instructions,
                                context
                        );

        ResponseCreateParams params =
                ResponseCreateParams
                        .builder()
                        .model(MODEL_ID)
                        .input(input)
                        .build();

        Response response =
                client.responses()
                        .create(params);

        StringBuilder output =
                new StringBuilder();

        response.output()
                .stream()
                .flatMap(
                        item ->
                                item.message()
                                        .stream()
                )
                .flatMap(
                        message ->
                                message.content()
                                        .stream()
                )
                .flatMap(
                        content ->
                                content.outputText()
                                        .stream()
                )
                .forEach(
                        text ->
                                output.append(
                                        text.text()
                                )
                );

        if (output.isEmpty()) {

            throw new IllegalStateException(
                    "OpenAI returned an empty response."
            );
        }

        return ModelReply.finalReply(
                output.toString()
        );
    }
}
