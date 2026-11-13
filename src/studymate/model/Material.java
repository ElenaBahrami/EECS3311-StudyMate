 package studymate.model;                                       
                                                                
 import java.util.ArrayList;                                    
 import java.util.List;                                         
 import java.util.UUID;                                         
                                                                
 public class Material {                                        
                                                                
     private UUID id;                                           
     private UUID courseId;                                     
     private String title;                                      
     private String hash;                                       
     private List<Chunk> chunks;                                
                                                                
     // Used when creating a NEW material                       
     public Material(UUID courseId, String title, String hash) {
                                                                
         this.id = UUID.randomUUID();                           
         this.courseId = courseId;                              
         this.title = title;                                    
         this.hash = hash;                                      
         this.chunks = new ArrayList<>();                       
     }                                                          
                                                                
     // Used when loading an EXISTING material from SQLite      
     public Material(                                           
             UUID id,                                           
             UUID courseId,                                     
             String title,                                      
             String hash) {                                     
                                                                
         this.id = id;                                          
         this.courseId = courseId;                              
         this.title = title;                                    
         this.hash = hash;                                      
         this.chunks = new ArrayList<>();                       
     }                                                          
                                                                
     public UUID getId() {                                      
         return id;                                             
     }                                                          
                                                                
     public UUID getCourseId() {                                
         return courseId;                                       
     }                                                          
                                                                
     public String getTitle() {                                 
         return title;                                          
     }                                                          
                                                                
     public String getHash() {                                  
         return hash;                                           
     }                                                          
                                                                
     public List<Chunk> chunks() {                              
         return chunks;                                         
     }                                                          
                                                                
     public void addChunk(Chunk chunk) {                        
         chunks.add(chunk);                                     
     }                                                          
 }                                                              