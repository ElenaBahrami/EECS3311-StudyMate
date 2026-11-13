 package studymate;                                 
                                                    
 import java.io.IOException;                        
 import java.util.List;                             
 import java.util.Scanner;                          
                                                    
 import studymate.agent.LearningAgent;              
 import studymate.agent.OpenAIAdapter;              
 import studymate.agent.RetrievalTool;              
 import studymate.agent.ToolRegistry;               
 import studymate.extraction.TextExtractor;         
 import studymate.model.Course;                     
 import studymate.model.LearningArtifact;           
 import studymate.model.Material;                   
 import studymate.repository.StudyRepository;       
 import studymate.service.MaterialService;          
                                                    
 public class Main {                                
                                                    
     public static void main(String[] args) {       
                                                    
         System.out.println(                        
                 "=============================="   
         );                                         
                                                    
         System.out.println(                        
                 "          StudyMate"              
         );                                         
                                                    
         System.out.println(                        
                 "=============================="   
         );                                         
                                                    
         StudyRepository repository =               
                 new StudyRepository();             
                                                    
         Course course =                            
                 repository.getOrCreateCourse(      
                         "EECS3311 Software Design" 
                 );                                 
                                                    
         TextExtractor extractor =                  
                 new TextExtractor();               
                                                    
         MaterialService materialService =          
                 new MaterialService(               
                         repository,                
                         extractor                  
                 );                                 
                                                    
         ToolRegistry toolRegistry =                
                 new ToolRegistry();                
                                                    
         toolRegistry.register(                     
                 "retrieve",                        
                 new RetrievalTool(repository)      
         );                                         
                                                    
         OpenAIAdapter openAI =                     
                 new OpenAIAdapter();               
                                                    
         LearningAgent learningAgent =              
                 new LearningAgent(                 
                         openAI,                    
                         toolRegistry,              
                         repository                 
                 );                                 
                                                    
         Scanner scanner =                          
                 new Scanner(System.in);            
                                                    
         boolean running = true;                    
                                                    
         while (running) {                          
                                                    
             System.out.println();                  
             System.out.println(                    
                     "Course: "                     
                             + course.getName()     
             );                                     
                                                    
             System.out.println();                  
             System.out.println(                    
                     "1 - Import material"          
             );                                     
                                                    
             System.out.println(                    
                     "2 - Summarise materials"      
             );                                     
                                                    
             System.out.println(                    
                     "3 - Show saved materials"     
             );                                     
                                                    
             System.out.println(                    
                     "0 - Exit"                     
             );                                     
                                                    
             System.out.println();                  
                                                    
             System.out.print(                      
                     "Choose an option: "           
             );                                     
                                                    
             String option =                        
                     scanner.nextLine().trim();     
                                                    
             switch (option) {                      
                                                    
                 case "1" ->                        
                         importMaterial(            
                                 scanner,           
                                 materialService,   
                                 course,            
                                 repository         
                         );                         
                                                    
                 case "2" ->                        
                         summarise(                 
                                 learningAgent,     
                                 course             
                         );                         
                                                    
                 case "3" ->                        
                         showMaterials(             
                                 repository,        
                                 course             
                         );                         
                                                    
                 case "0" -> {                      
                                                    
                     running = false;               
                                                    
                     System.out.println(            
                             "StudyMate finished."  
                     );                             
                 }                                  
                                                    
                 default ->                         
                         System.out.println(        
                                 "Invalid option."  
                         );                         
             }                                      
         }                                          
                                                    
         scanner.close();                           
     }                                              
                                                    
     private static void importMaterial(            
             Scanner scanner,                       
             MaterialService materialService,       
             Course course,                         
             StudyRepository repository) {          
                                                    
         System.out.println();                      
                                                    
         System.out.print(                          
                 "Enter PDF/TXT filename: "         
         );                                         
                                                    
         String filePath =                          
                 scanner.nextLine().trim();         
                                                    
         try {                                      
                                                    
             Material material =                    
                     materialService                
                             .importMaterial(       
                                     course.id(),   
                                     filePath       
                             );                     
                                                    
             System.out.println();                  
                                                    
             System.out.println(                    
                     "Import successful!"           
             );                                     
                                                    
             System.out.println(                    
                     "Material: "                   
                             + material.getTitle()  
             );                                     
                                                    
             System.out.println(                    
                     "Chunks: "                     
                             + material.chunks()    
                             .size()                
             );                                     
                                                    
         } catch (IOException e) {                  
                                                    
             System.out.println();                  
                                                    
             System.out.println(                    
                     "Import rejected: "            
                             + e.getMessage()       
             );                                     
         }                                          
                                                    
         showMaterials(                             
                 repository,                        
                 course                             
         );                                         
     }                                              
                                                    
     private static void summarise(                 
             LearningAgent learningAgent,           
             Course course) {                       
                                                    
         System.out.println();                      
                                                    
         System.out.println(                        
                 "Generating AI summary..."         
         );                                         
                                                    
         try {                                      
                                                    
             LearningArtifact summary =             
                     learningAgent.summarise(       
                             course.id()            
                     );                             
                                                    
             System.out.println();                  
                                                    
             System.out.println(                    
                     "===== AI SUMMARY ====="       
             );                                     
                                                    
             System.out.println(                    
                     summary.content()              
             );                                     
                                                    
             System.out.println();                  
                                                    
             System.out.println(                    
                     "Sources used: "               
                             + summary              
                             .getSourceIds()        
                             .size()                
             );                                     
                                                    
         } catch (Exception e) {                    
                                                    
             System.out.println();                  
                                                    
             System.out.println(                    
                     "Summary failed: "             
                             + e.getMessage()       
             );                                     
         }                                          
     }                                              
                                                    
     private static void showMaterials(             
             StudyRepository repository,            
             Course course) {                       
                                                    
         List<Material> materials =                 
                 repository                         
                         .getMaterialsByCourse(     
                                 course.id()        
                         );                         
                                                    
         System.out.println();                      
                                                    
         System.out.println(                        
                 "Saved materials for this course: "
                         + materials.size()         
         );                                         
                                                    
         if (materials.isEmpty()) {                 
                                                    
             System.out.println(                    
                     "No materials imported yet."   
             );                                     
                                                    
             return;                                
         }                                          
                                                    
         for (Material material : materials) {      
                                                    
             System.out.println(                    
                     "- "                           
                             + material.getTitle()  
                             + " | "                
                             + material.chunks()    
                             .size()                
                             + " chunks"            
             );                                     
         }                                          
     }                                              
 }                                                  