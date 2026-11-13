 package studymate.model;                              
                                                       
 import java.util.UUID;                                
                                                       
 public class Chunk {                                  
                                                       
     private UUID id;                                  
     private int page;                                 
     private String text;                              
                                                       
     // Used when creating a NEW chunk                 
     public Chunk(int page, String text) {             
         this.id = UUID.randomUUID();                  
         this.page = page;                             
         this.text = text;                             
     }                                                 
                                                       
     // Used when loading an EXISTING chunk from SQLite
     public Chunk(UUID id, int page, String text) {    
         this.id = id;                                 
         this.page = page;                             
         this.text = text;                             
     }                                                 
                                                       
     public UUID getId() {                             
         return id;                                    
     }                                                 
                                                       
     public int getPage() {                            
         return page;                                  
     }                                                 
                                                       
     public String getText() {                         
         return text;                                  
     }                                                 
                                                       
     public String sourceRef() {                       
         return "Page " + page;                        
     }                                                 
 }                                                     