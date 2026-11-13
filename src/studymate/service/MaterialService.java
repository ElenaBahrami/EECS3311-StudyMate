  package studymate.service;                                    
                                                                
  import java.io.IOException;                                   
  import java.nio.file.Files;                                   
  import java.nio.file.Path;                                    
  import java.security.MessageDigest;                           
  import java.security.NoSuchAlgorithmException;                
  import java.util.List;                                        
  import java.util.UUID;                                        
                                                                
  import studymate.extraction.TextExtractor;                    
  import studymate.model.Chunk;                                 
  import studymate.model.Material;                              
  import studymate.repository.StudyRepository;                  
                                                                
  public class MaterialService {                                
                                                                
      private final StudyRepository repository;                 
      private final TextExtractor extractor;                    
                                                                
      public MaterialService(                                   
              StudyRepository repository,                       
              TextExtractor extractor) {                        
                                                                
          this.repository = repository;                         
          this.extractor = extractor;                           
      }                                                         
                                                                
      public Material importMaterial(                           
              UUID courseId,                                    
              String filePath)                                  
              throws IOException {                              
                                                                
          if (courseId == null) {                               
                                                                
              throw new IOException(                            
                      "Course ID is required."                  
              );                                                
          }                                                     
                                                                
          if (repository.findCourse(courseId)                   
                  == null) {                                    
                                                                
              throw new IOException(                            
                      "Unknown course ID."                      
              );                                                
          }                                                     
                                                                
          Path path =                                           
                  Path.of(filePath);                            
                                                                
          if (!Files.exists(path)) {                            
                                                                
              throw new IOException(                            
                      "File does not exist: "                   
                              + filePath                        
              );                                                
          }                                                     
                                                                
          String hash =                                         
                  calculateHash(path);                          
                                                                
          Material existing =                                   
                  repository.findMaterialByHash(                
                          hash                                  
                  );                                            
                                                                
          if (existing != null) {                               
                                                                
              throw new IOException(                            
                      "This material has already been imported."
              );                                                
          }                                                     
                                                                
          List<Chunk> chunks =                                  
                  extractor.extract(filePath);                  
                                                                
          String title =                                        
                  path.getFileName()                            
                          .toString();                          
                                                                
          Material material =                                   
                  new Material(                                 
                          courseId,                             
                          title,                                
                          hash                                  
                  );                                            
                                                                
          for (Chunk chunk : chunks) {                          
              material.addChunk(chunk);                         
          }                                                     
                                                                
          repository.saveMaterial(material);                    
                                                                
          return material;                                      
      }                                                         
                                                                
      private String calculateHash(                             
              Path path)                                        
              throws IOException {                              
                                                                
          try {                                                 
                                                                
              byte[] bytes =                                    
                      Files.readAllBytes(path);                 
                                                                
              MessageDigest digest =                            
                      MessageDigest.getInstance(                
                              "SHA-256"                         
                      );                                        
                                                                
              byte[] hashBytes =                                
                      digest.digest(bytes);                     
                                                                
              StringBuilder result =                            
                      new StringBuilder();                      
                                                                
              for (byte b : hashBytes) {                        
                                                                
                  result.append(                                
                          String.format(                        
                                  "%02x",                       
                                  b                             
                          )                                     
                  );                                            
              }                                                 
                                                                
              return result.toString();                         
                                                                
          } catch (NoSuchAlgorithmException e) {                
                                                                
              throw new IOException(                            
                      "Unable to calculate file hash.",         
                      e                                         
              );                                                
          }                                                     
      }                                                         
  }                                                             