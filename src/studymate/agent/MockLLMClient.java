package studymate.agent;

public class MockLLMClient
        implements LLMClient {

    @Override
    public ModelReply generate(
            String context,
            String instructions) {

        return ModelReply.finalReply(
                "Temporary mock response."
        );
    }
}