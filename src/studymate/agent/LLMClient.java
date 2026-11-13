package studymate.agent;

public interface LLMClient {

    ModelReply generate(
            String context,
            String instructions)
            throws Exception;
}
