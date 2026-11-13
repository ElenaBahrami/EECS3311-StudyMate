  package studymate.agent;                                      
                                                                
  import java.util.ArrayList;                                   
  import java.util.List;                                        
  import java.util.Map;                                         
  import java.util.UUID;                                        
                                                                
  import studymate.model.LearningArtifact;                      
  import studymate.repository.StudyRepository;                  
                                                                
  public class LearningAgent {                                  
                                                                
      private static final int MAX_STEPS = 6;                   
                                                                
      private final LLMClient client;                           
      private final ToolRegistry tools;                         
      private final StudyRepository repository;                 
                                                                
      public LearningAgent(                                     
              LLMClient client,                                 
              ToolRegistry tools,                               
              StudyRepository repository) {                     
                                                                
          this.client = client;                                 
          this.tools = tools;                                   
          this.repository = repository;                         
      }                                                         
                                                                
      // =====================================================  
      // F02 - SUMMARISE MATERIALS                              
      // =====================================================  
                                                                
      public LearningArtifact summarise(                        
              UUID courseId)                                    
              throws Exception {                                
                                                                
          String instructions =                                 
                  """                                           
                  Create a concise study summary using only     
                  the supplied course evidence.                 
                                                                
                  Organize the summary into clear sections.     
                                                                
                  Do not introduce facts that are not           
                  supported by the supplied evidence.           
                                                                
                  Include source identifiers from the           
                  supplied [SOURCE ...] labels when making      
                  factual points.                               
                                                                
                  If the evidence is insufficient, say so.      
                  """;                                          
                                                                
          return generateArtifact(                              
                  courseId,                                     
                  "SUMMARY",                                    
                  instructions                                  
          );                                                    
      }                                                         
                                                                
      // =====================================================  
      // F03 - EXTRACT KEY CONCEPTS                             
      // =====================================================  
                                                                
      public LearningArtifact extractConcepts(                  
              UUID courseId)                                    
              throws Exception {                                
                                                                
          String instructions =                                 
                  """                                           
                  Extract the most important concepts from      
                  the supplied course evidence.                 
                                                                
                  For each concept provide:                     
                                                                
                  1. Concept name                               
                  2. Short definition                           
                  3. Why the concept is important               
                  4. Relevant source identifier                 
                                                                
                  Focus only on concepts that are useful for    
                  studying the course material.                 
                                                                
                  Do not introduce concepts that are not        
                  supported by the supplied evidence.           
                                                                
                  Use the supplied [SOURCE ...] identifiers     
                  when citing evidence.                         
                                                                
                  Avoid duplicates.                             
                                                                
                  Aim for approximately 8 to 12 key concepts.   
                                                                
                  If the evidence is insufficient, say so.      
                  """;                                          
                                                                
          return generateArtifact(                              
                  courseId,                                     
                  "CONCEPTS",                                   
                  instructions                                  
          );                                                    
      }                                                         
                                                                
      // =====================================================  
      // SHARED AGENT LOGIC                                     
      // =====================================================  
                                                                
      private LearningArtifact generateArtifact(                
              UUID courseId,                                    
              String artifactKind,                              
              String instructions)                              
              throws Exception {                                
                                                                
          if (courseId == null) {                               
                                                                
              throw new IllegalArgumentException(               
                      "Course ID is required."                  
              );                                                
          }                                                     
                                                                
          if (repository.findCourse(courseId)                   
                  == null) {                                    
                                                                
              throw new IllegalArgumentException(               
                      "Unknown course ID."                      
              );                                                
          }                                                     
                                                                
          StringBuilder context =                               
                  new StringBuilder();                          
                                                                
          List<UUID> usedSourceIds =                            
                  new ArrayList<>();                            
                                                                
          boolean retrievalUsed =                               
                  false;                                        
                                                                
          for (int step = 0;                                    
               step < MAX_STEPS;                                
               step++) {                                        
                                                                
              if (!retrievalUsed) {                             
                                                                
                  ToolResult retrieval =                        
                          tools.execute(                        
                                  "retrieve",                   
                                  Map.of(                       
                                          "courseId",           
                                          courseId.toString(),  
                                          "query",              
                                          ""                    
                                  )                             
                          );                                    
                                                                
                  if (retrieval                                 
                          .getSourceIds()                       
                          .isEmpty()) {                         
                                                                
                      throw new IllegalStateException(          
                              "No course evidence is available."
                      );                                        
                  }                                             
                                                                
                  context.append(                               
                          retrieval.getData()                   
                  );                                            
                                                                
                  usedSourceIds.addAll(                         
                          retrieval.getSourceIds()              
                  );                                            
                                                                
                  retrievalUsed = true;                         
              }                                                 
                                                                
              ModelReply reply =                                
                      client.generate(                          
                              context.toString(),               
                              instructions                      
                      );                                        
                                                                
              if (reply.getType()                               
                      == ModelReply.Type.FINAL) {               
                                                                
                  String payload =                              
                          reply.getFinalPayload();              
                                                                
                  if (payload == null                           
                          || payload.isBlank()) {               
                                                                
                      throw new IllegalStateException(          
                              "Model returned an empty result." 
                      );                                        
                  }                                             
                                                                
                  return new LearningArtifact(                  
                          courseId,                             
                          artifactKind,                         
                          payload,                              
                          usedSourceIds                         
                  );                                            
              }                                                 
                                                                
              if (reply.getType()                               
                      == ModelReply.Type.TOOL_CALL) {           
                                                                
                  String toolName =                             
                          reply.getToolName();                  
                                                                
                  if (!tools.contains(toolName)) {              
                                                                
                      context.append(                           
                              "\nInvalid tool request: "        
                                      + toolName                
                                      + "\n"                    
                      );                                        
                                                                
                      continue;                                 
                  }                                             
                                                                
                  ToolResult result =                           
                          tools.execute(                        
                                  toolName,                     
                                  reply.getArguments()          
                          );                                    
                                                                
                  context.append(                               
                          "\n"                                  
                                  + result.getData()            
                                  + "\n"                        
                  );                                            
                                                                
                  usedSourceIds.addAll(                         
                          result.getSourceIds()                 
                  );                                            
              }                                                 
          }                                                     
                                                                
          throw new IllegalStateException(                      
                  "Agent step limit reached "                   
                          + "without a valid result."           
          );                                                    
      }                                                         
  }                                                             